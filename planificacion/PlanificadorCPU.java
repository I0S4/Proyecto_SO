package planificacion;

import core.Proceso;
import core.EstadoProceso;
import strategy.AlgoritmoPlanificacion;
import memoria.GestorMemoriaVirtual;
import interrupciones.GestorInterrupciones;
import interrupciones.Interrupcion;
import interrupciones.EscuchadorInterrupcion;
import interrupciones.Interrupcion.TipoInterrupcion;
import dto.EstadoSistemaDTO.ColasProcesosDTO;
import dto.EstadoSistemaDTO.EstadoCPUDTO;

import java.util.*;

/**
 * Motor del planificador de CPU del simulador.
 * Controla el ciclo de ejecución tick-a-tick, la admisión, el despacho,
 * la expiración del quantum, y reacciona a interrupciones de Page Fault y E/S.
 */
public class PlanificadorCPU implements EscuchadorInterrupcion {
    private final Queue<Proceso> colaNuevos;
    private final List<Proceso> colaListos;
    private final List<Proceso> colaBloqueados;
    private final Map<String, Proceso> mapaProcesos;
    private final Map<String, Integer> ticksRestantesPageFault;

    private Proceso procesoEnCPU;
    private int ticksQuantumActual;
    private int quantum;
    private AlgoritmoPlanificacion algoritmo;
    private GestorMemoriaVirtual gmv;
    private final GestorInterrupciones gestorInterrupciones;

    public PlanificadorCPU(GestorMemoriaVirtual gmv, GestorInterrupciones gestorInterrupciones) {
        this.colaNuevos = new LinkedList<>();
        this.colaListos = new ArrayList<>();
        this.colaBloqueados = new ArrayList<>();
        this.mapaProcesos = new HashMap<>();
        this.ticksRestantesPageFault = new HashMap<>();
        this.procesoEnCPU = null;
        this.ticksQuantumActual = 0;
        this.quantum = 4; // default
        this.gmv = gmv;
        this.gestorInterrupciones = gestorInterrupciones;

        if (gestorInterrupciones != null) {
            gestorInterrupciones.suscribir(TipoInterrupcion.PAGE_FAULT, this);
            gestorInterrupciones.suscribir(TipoInterrupcion.ES_COMPLETA, this);
        }
    }

    public void admitirProceso(Proceso p) {
        if (p == null) return;
        mapaProcesos.put(p.getId(), p);
        colaNuevos.add(p);
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }

    public int getQuantum() {
        return quantum;
    }

    public void setAlgoritmo(AlgoritmoPlanificacion algoritmo) {
        this.algoritmo = algoritmo;
    }

    public AlgoritmoPlanificacion getAlgoritmo() {
        return algoritmo;
    }

    public Proceso getProcesoEnCPU() {
        return procesoEnCPU;
    }

    public List<Proceso> getColaListos() {
        return colaListos;
    }

    public List<Proceso> getColaBloqueados() {
        return colaBloqueados;
    }

    public Queue<Proceso> getColaNuevos() {
        return colaNuevos;
    }

    public Map<String, Proceso> getMapaProcesos() {
        return mapaProcesos;
    }

    public void reset(GestorMemoriaVirtual nuevoGmv) {
        this.gmv = nuevoGmv;
        this.colaNuevos.clear();
        this.colaListos.clear();
        this.colaBloqueados.clear();
        this.mapaProcesos.clear();
        this.ticksRestantesPageFault.clear();
        this.procesoEnCPU = null;
        this.ticksQuantumActual = 0;
    }

    public void ejecutarTick() {
        // 0. Decrementar y resolver Page Faults de ticks anteriores
        List<String> listosParaDesbloquear = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ticksRestantesPageFault.entrySet()) {
            int restante = entry.getValue() - 1;
            ticksRestantesPageFault.put(entry.getKey(), restante);
            if (restante <= 0) {
                listosParaDesbloquear.add(entry.getKey());
            }
        }

        for (String idProc : listosParaDesbloquear) {
            ticksRestantesPageFault.remove(idProc);
            Proceso p = mapaProcesos.get(idProc);
            if (p != null && p.getEstado() == EstadoProceso.BLOQUEADO) {
                p.setEstado(EstadoProceso.LISTO);
                colaBloqueados.remove(p);
                if (!colaListos.contains(p)) {
                    colaListos.add(p);
                }
            }
        }

        // 1. ADMISIÓN: mover todos los de colaNuevos a colaListos
        while (!colaNuevos.isEmpty()) {
            Proceso p = colaNuevos.poll();
            p.setEstado(EstadoProceso.LISTO);
            colaListos.add(p);
        }

        // 2. PREEMPCIÓN (solo algoritmos expropiativos como SRTF) y EXPIRACIÓN QUANTUM (RR)
        if (procesoEnCPU != null && algoritmo != null) {
            if (algoritmo.usaQuantum() && ticksQuantumActual >= quantum) {
                if (procesoEnCPU.getTiempoRafagaRestante() > 0) {
                    procesoEnCPU.setEstado(EstadoProceso.LISTO);
                    colaListos.add(procesoEnCPU);
                    procesoEnCPU = null;
                }
            } else if (algoritmo.esExpropiativo() && !colaListos.isEmpty()) {
                Proceso candidato = algoritmo.seleccionarSiguiente(colaListos);
                if (candidato != null && candidato.getTiempoRafagaRestante() < procesoEnCPU.getTiempoRafagaRestante()) {
                    procesoEnCPU.setEstado(EstadoProceso.LISTO);
                    colaListos.add(procesoEnCPU);
                    procesoEnCPU = null;
                }
            }
        }

        // 3. DESPACHO: si CPU ociosa, seleccionar de colaListos
        if (procesoEnCPU == null && !colaListos.isEmpty()) {
            if (algoritmo != null) {
                Proceso sgte = algoritmo.seleccionarSiguiente(colaListos);
                if (sgte != null) {
                    colaListos.remove(sgte);
                    sgte.setEstado(EstadoProceso.EJECUTANDO);
                    procesoEnCPU = sgte;
                    ticksQuantumActual = 0;
                }
            }
        }

        // 4. EJECUCIÓN
        if (procesoEnCPU != null) {
            String idProceso = procesoEnCPU.getId();
            long pcValue = procesoEnCPU.getProgramCounter().getValor();

            // Acceso a memoria virtual
            gmv.accederDireccion(idProceso, pcValue);

            // Si ocurrió un PAGE_FAULT, procesoEnCPU ahora es null
            if (procesoEnCPU != null && procesoEnCPU.getEstado() == EstadoProceso.EJECUTANDO) {
                procesoEnCPU.incrementarPC();
                procesoEnCPU.decrementarRafaga();
                ticksQuantumActual++;
            }
        }

        // 6. TERMINACIÓN
        if (procesoEnCPU != null && procesoEnCPU.getTiempoRafagaRestante() == 0) {
            procesoEnCPU.setEstado(EstadoProceso.TERMINADO);
            procesoEnCPU = null;
        }
    }

    public void bloquearPorES(Proceso p) {
        if (p == null) return;
        if (p.getEstado() == EstadoProceso.EJECUTANDO) {
            p.setEstado(EstadoProceso.BLOQUEADO);
        }
        if (procesoEnCPU == p) {
            procesoEnCPU = null;
        }
        colaListos.remove(p);
        if (!colaBloqueados.contains(p)) {
            colaBloqueados.add(p);
        }
    }

    @Override
    public void procesarInterrupcion(Interrupcion interrupcion) {
        if (interrupcion == null) return;

        if (interrupcion.tipo() == TipoInterrupcion.PAGE_FAULT) {
            String idProceso = (String) interrupcion.payload();
            Proceso p = mapaProcesos.get(idProceso);
            if (p != null) {
                if (p.getEstado() == EstadoProceso.EJECUTANDO) {
                    p.setEstado(EstadoProceso.BLOQUEADO);
                }
                if (procesoEnCPU != null && procesoEnCPU.getId().equals(idProceso)) {
                    procesoEnCPU = null;
                }
                colaListos.remove(p);
                if (!colaBloqueados.contains(p)) {
                    colaBloqueados.add(p);
                }
                ticksRestantesPageFault.put(idProceso, 1);
            }
        } else if (interrupcion.tipo() == TipoInterrupcion.ES_COMPLETA) {
            Proceso p = (Proceso) interrupcion.payload();
            if (p != null) {
                p.setEstado(EstadoProceso.LISTO);
                colaBloqueados.remove(p);
                if (!colaListos.contains(p)) {
                    colaListos.add(p);
                }
            }
        }
    }

    public ColasProcesosDTO generarColasDTO() {
        List<String> nuevos = new ArrayList<>();
        for (Proceso p : colaNuevos) nuevos.add(p.getId());

        List<String> listos = new ArrayList<>();
        for (Proceso p : colaListos) listos.add(p.getId());

        List<String> bloqueados = new ArrayList<>();
        for (Proceso p : colaBloqueados) bloqueados.add(p.getId());

        return new ColasProcesosDTO(nuevos, listos, bloqueados);
    }

    public EstadoCPUDTO generarCPUDTO() {
        if (procesoEnCPU == null) {
            return new EstadoCPUDTO(null, 0, "0x00000000");
        }
        return new EstadoCPUDTO(
            procesoEnCPU.getId(),
            procesoEnCPU.getProgramCounter().getValor(),
            procesoEnCPU.getProgramCounter().toString()
        );
    }
}

import core.*;
import planificacion.PlanificadorCPU;
import memoria.*;
import interrupciones.GestorInterrupciones;
import interrupciones.Interrupcion;
import interrupciones.Interrupcion.TipoInterrupcion;
import dto.EstadoSistemaDTO;
import dto.EstadoSistemaDTO.*;
import strategy.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Orquestador principal del motor del Simulador del Sistema Operativo.
 * Coordina el Planificador de CPU, la Memoria Virtual, los Dispositivos de E/S,
 * y centraliza las interrupciones y el registro de logs del sistema.
 */
public class SimuladorSO {
    private int tickGlobal;
    private final GestorInterrupciones gestorInterrupciones;
    private final PlanificadorCPU planificadorCPU;
    private GestorMemoriaVirtual gmv;
    private final List<DispositivoES> dispositivosES;
    private final List<String> logs;

    private String algoritmoPlanificacionNombre;
    private int quantum;
    private int tamanoPaginaBytes;
    private int tamanoRamBytes;

    public SimuladorSO() {
        this.tickGlobal = 0;
        this.logs = new ArrayList<>();
        this.gestorInterrupciones = new GestorInterrupciones();

        // Valores por defecto
        this.algoritmoPlanificacionNombre = "RR";
        this.quantum = 4;
        this.tamanoPaginaBytes = 4096;
        this.tamanoRamBytes = 16384;

        this.gmv = new GestorMemoriaVirtual(this.tamanoRamBytes, this.tamanoPaginaBytes);
        this.gmv.setGestorInterrupciones(this.gestorInterrupciones);

        this.planificadorCPU = new PlanificadorCPU(this.gmv, this.gestorInterrupciones);
        this.planificadorCPU.setQuantum(this.quantum);
        this.planificadorCPU.setAlgoritmo(new RoundRobin());

        // Inicializar dispositivos de E/S
        this.dispositivosES = new ArrayList<>();
        this.dispositivosES.add(new DispositivoES("Teclado", this.gestorInterrupciones));
        this.dispositivosES.add(new DispositivoES("Disco", this.gestorInterrupciones));
        this.dispositivosES.add(new DispositivoES("Impresora", this.gestorInterrupciones));

        // Suscribirse a interrupciones para el registro de logs del sistema
        this.gestorInterrupciones.suscribir(TipoInterrupcion.PAGE_FAULT, intr -> {
            logs.add(String.format("[Tick %d] Page Fault resuelto: %s.", tickGlobal, intr.descripcion()));
        });

        this.gestorInterrupciones.suscribir(TipoInterrupcion.ES_COMPLETA, intr -> {
            Proceso p = (Proceso) intr.payload();
            logs.add(String.format("[Tick %d] E/S completada para proceso %s.", tickGlobal, p.getId()));
        });
    }

    public void configurar(String algoritmo, int quantum, int tamanoPagina, int tamanoRam) {
        this.algoritmoPlanificacionNombre = algoritmo;
        this.quantum = quantum;
        this.tamanoPaginaBytes = tamanoPagina;
        this.tamanoRamBytes = tamanoRam;

        AlgoritmoPlanificacion alg;
        switch (algoritmo.toUpperCase()) {
            case "FCFS":
            case "FIFO":
                alg = new FCFS();
                break;
            case "SJF":
                alg = new SJF();
                break;
            case "SRTF":
                alg = new SRTF();
                break;
            case "RR":
            case "ROUND_ROBIN":
            case "ROUNDROBIN":
                alg = new RoundRobin();
                break;
            default:
                alg = new FCFS();
        }

        // Crear nuevo gestor de memoria virtual
        this.gmv = new GestorMemoriaVirtual(tamanoRam, tamanoPagina);
        this.gmv.setGestorInterrupciones(this.gestorInterrupciones);

        // Resetear estado del planificador de CPU
        this.planificadorCPU.reset(this.gmv);
        this.planificadorCPU.setAlgoritmo(alg);
        this.planificadorCPU.setQuantum(quantum);

        // Limpiar colas de dispositivos de E/S
        for (DispositivoES dev : dispositivosES) {
            dev.setProcesoActivo(null);
            dev.setTiempoRestanteTick(0);
            dev.getColaEspera().clear();
        }

        this.tickGlobal = 0;
        this.logs.clear();
        this.logs.add(String.format("[Config] Simulación inicializada: Algoritmo %s, Quantum %d, Página: %d B, RAM: %d B.",
                algoritmo, quantum, tamanoPagina, tamanoRam));
    }

    public void admitirProceso(String id, int totalInstrucciones, int bytesStack, int bytesHeap) {
        Proceso p = new Proceso(id, totalInstrucciones, tamanoPaginaBytes);
        // Inicializar espacio de páginas en el gestor de memoria
        TablaPaginas tp = gmv.inicializarEspacioProceso(id, bytesStack, bytesHeap);
        p.setTablaPaginas(tp);
        // Admitir proceso al planificador de CPU
        planificadorCPU.admitirProceso(p);

        logs.add(String.format("[Admitir] Proceso %s admitido al sistema. Instrucciones: %d, Stack: %d B, Heap: %d B.",
                id, totalInstrucciones, bytesStack, bytesHeap));
    }

    public void solicitarES(String idProceso, String nombreDispositivo) {
        Proceso p = planificadorCPU.getMapaProcesos().get(idProceso);
        if (p != null) {
            DispositivoES dev = buscarDispositivo(nombreDispositivo);
            if (dev != null) {
                planificadorCPU.bloquearPorES(p);
                dev.solicitarES(p);
                logs.add(String.format("[Tick %d] Proceso %s solicitó E/S en %s.", tickGlobal, idProceso, nombreDispositivo));
            }
        }
    }

    public EstadoSistemaDTO ejecutarTick() {
        // 1. Ejecutar paso del planificador de CPU
        planificadorCPU.ejecutarTick();

        // 2. Actualizar dispositivos de E/S
        for (DispositivoES dev : dispositivosES) {
            dev.actualizarTick();
        }

        // 3. Incrementar tick global
        tickGlobal++;

        // 4. Notificar tick a observadores
        gestorInterrupciones.notificar(new Interrupcion(
                TipoInterrupcion.TICK,
                "Tick global " + tickGlobal,
                tickGlobal
        ));

        // 5. Registrar log de ejecución básica si hay proceso en CPU
        Proceso ejec = planificadorCPU.getProcesoEnCPU();
        if (ejec != null) {
            logs.add(String.format("[Tick %d] Instrucción ejecutada exitosamente para %s.", tickGlobal, ejec.getId()));
        } else {
            logs.add(String.format("[Tick %d] CPU en estado ocioso.", tickGlobal));
        }

        return generarEstadoCompleto();
    }

    public EstadoSistemaDTO generarEstadoCompleto() {
        ConfiguracionDTO configDTO = new ConfiguracionDTO(
                algoritmoPlanificacionNombre,
                quantum,
                tamanoPaginaBytes
        );

        EstadoCPUDTO cpuDTO = planificadorCPU.generarCPUDTO();
        ColasProcesosDTO colasDTO = planificadorCPU.generarColasDTO();

        Map<String, ProcesoDTO> diccionarioProcesos = new HashMap<>();
        for (Map.Entry<String, Proceso> entry : planificadorCPU.getMapaProcesos().entrySet()) {
            Proceso p = entry.getValue();
            diccionarioProcesos.put(p.getId(), new ProcesoDTO(
                p.getId(),
                p.getEstado().name(),
                p.getTiempoRafagaRestante()
            ));
        }

        GestionMemoriaDTO memDTO = gmv.generarGestionMemoriaDTO();

        List<DispositivoESDTO> devsDTO = new ArrayList<>();
        for (DispositivoES dev : dispositivosES) {
            String procActId = (dev.getProcesoActivo() != null) ? dev.getProcesoActivo().getId() : null;
            List<String> colaEsperaIds = new ArrayList<>();
            for (Proceso p : dev.getColaEspera()) {
                colaEsperaIds.add(p.getId());
            }
            devsDTO.add(new DispositivoESDTO(
                    dev.getNombre(),
                    procActId,
                    dev.getTiempoRestanteTick(),
                    colaEsperaIds
            ));
        }

        List<String> logsCopy = new ArrayList<>(logs);
        Collections.reverse(logsCopy); // Últimos logs primero

        SistemaDTO sistemaDTO = new SistemaDTO(
                false, // toleranciaFallosExcedida por defecto false
                logsCopy
        );

        return new EstadoSistemaDTO(
                tickGlobal,
                configDTO,
                cpuDTO,
                colasDTO,
                diccionarioProcesos,
                memDTO,
                devsDTO,
                sistemaDTO
        );
    }

    private DispositivoES buscarDispositivo(String nombre) {
        for (DispositivoES dev : dispositivosES) {
            if (dev.getNombre().equalsIgnoreCase(nombre)) {
                return dev;
            }
        }
        return null;
    }

    // Getters auxiliares para propósitos de verificación / testing
    public int getTickGlobal() { return tickGlobal; }
    public GestorInterrupciones getGestorInterrupciones() { return gestorInterrupciones; }
    public PlanificadorCPU getPlanificadorCPU() { return planificadorCPU; }
    public GestorMemoriaVirtual getGmv() { return gmv; }
    public List<DispositivoES> getDispositivosES() { return dispositivosES; }
    public List<String> getLogs() { return logs; }
}

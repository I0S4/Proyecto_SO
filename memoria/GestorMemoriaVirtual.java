package memoria;

import dto.EstadoSistemaDTO.AreaSwapDTO;
import dto.EstadoSistemaDTO.GestionMemoriaDTO;
import dto.EstadoSistemaDTO.MarcoRAMDTO;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Orquestador principal de la Memoria Virtual del Simulador (Fase 3).
 * Implementa las políticas de asignación (First, Best, Worst Fit), cálculo de desperdicio,
 * algoritmos de reemplazo de páginas (FIFO, LRU) y la lógica de Swapping con el disco.
 */
public class GestorMemoriaVirtual {

    public enum PoliticaAsignacion { FIRST_FIT, BEST_FIT, WORST_FIT }
    public enum AlgoritmoReemplazo { FIFO, LRU }

    private final int tamanoRamBytes;
    private final int tamanoPaginaBytes;
    private final MarcoRAM[] marcosRAM;
    private final DiscoSwap discoSwap;

    // Configuraciones de políticas activos en la simulación
    private PoliticaAsignacion politicaAsignacion;
    private AlgoritmoReemplazo algoritmoReemplazo;

    // Métricas globales requeridas por el DTO del sistema
    private int totalAccesos;
    private int pageFaultsTotales;
    private double porcentajeThrashing; // Calculado o actualizado por Backend 1

    // Estructuras de control para algoritmos de reemplazo
    private final Queue<Integer> colaFIFO; // Guarda IDs de marcos cargados

    public GestorMemoriaVirtual(int tamanoRamBytes, int tamanoPaginaBytes) {
        this.tamanoRamBytes = tamanoRamBytes;
        this.tamanoPaginaBytes = tamanoPaginaBytes;
        
        int cantidadMarcos = tamanoRamBytes / tamanoPaginaBytes;
        this.marcosRAM = new MarcoRAM[cantidadMarcos];
        for (int i = 0; i < cantidadMarcos; i++) {
            this.marcosRAM[i] = new MarcoRAM(i);
        }

        this.discoSwap = new DiscoSwap();
        this.politicaAsignacion = PoliticaAsignacion.FIRST_FIT;
        this.algoritmoReemplazo = AlgoritmoReemplazo.FIFO;
        
        this.colaFIFO = new LinkedList<>();
        this.totalAccesos = 0;
        this.pageFaultsTotales = 0;
        this.porcentajeThrashing = 0.0;
    }

    /**
     * Calcula el tamaño total requerido en páginas y determina el porcentaje de 
     * desperdicio (fragmentación interna) con dos decimales de precisión.
     */
    public double calcularDesperdicioBytes(int bytesStack, int bytesHeap) {
        int residuoStack = bytesStack % tamanoPaginaBytes;
        int residuoHeap = bytesHeap % tamanoPaginaBytes;

        int desperdicioStack = (residuoStack == 0) ? 0 : (tamanoPaginaBytes - residuoStack);
        int desperdicioHeap = (residuoHeap == 0) ? 0 : (tamanoPaginaBytes - residuoHeap);
        
        int desperdicioTotal = desperdicioStack + desperdicioHeap;
        int totalAsignadoBytes = bytesStack + bytesHeap + desperdicioTotal;

        if (totalAsignadoBytes == 0) return 0.0;

        double porcentaje = ((double) desperdicioTotal / totalAsignadoBytes) * 100.0;
        // Truncado estricto a dos decimales de precisión
        return Math.round(porcentaje * 100.0) / 100.0;
    }

    /**
     * Reserva espacio inicial para las páginas de un proceso usando políticas Fit.
     * Si no se encuentra un bloque contiguo libre que satisfaga la política elegida, 
     * las páginas nacen directamente penalizadas en el Área de Swap (Disco).
     */
    public TablaPaginas inicializarEspacioProceso(String idProceso, int bytesStack, int bytesHeap) {
        TablaPaginas tabla = new TablaPaginas(idProceso);

        int paginasStack = (int) Math.ceil((double) bytesStack / tamanoPaginaBytes);
        int paginasHeap = (int) Math.ceil((double) bytesHeap / tamanoPaginaBytes);
        int totalPaginasRequeridas = paginasStack + paginasHeap;

        for (int i = 0; i < paginasStack; i++) tabla.agregarPagina("STACK");
        for (int i = 0; i < paginasHeap; i++) tabla.agregarPagina("HEAP");

        // Buscar un bloque adecuado de marcos físicos contiguos en la RAM
        int indiceMarcoInicio = buscarBloqueLibre(totalPaginasRequeridas);

        for (Pagina pagina : tabla.getPaginas()) {
            if (indiceMarcoInicio != -1) {
                // Asignación en RAM física exitosa
                marcosRAM[indiceMarcoInicio].asignar(idProceso, pagina.getNumeroPagina());
                pagina.setMarcoFisico(indiceMarcoInicio);
                pagina.setPresente(true);
                
                if (!colaFIFO.contains(indiceMarcoInicio)) {
                    colaFIFO.add(indiceMarcoInicio);
                }
                indiceMarcoInicio++;
            } else {
                // No hay espacio contiguo bajo la política actual: Van a Swap
                discoSwap.enviarASwap(idProceso, pagina.getNumeroPagina());
                pagina.setMarcoFisico(-1);
                pagina.setPresente(false);
            }
        }
        return tabla;
    }

    /**
     * Simula el acceso a una página lógica del proceso.
     * Si no está presente, incrementa las métricas e inicia la rutina de Swapping.
     */
    public void accederPagina(String idProceso, Pagina pagina) {
        this.totalAccesos++;
        pagina.registrarAcceso(); // Actualiza bits R y timestamps en memoria real

        if (!pagina.isPresente()) {
            this.pageFaultsTotales++;
            // Disparar rutina de intercambio/swapping
            ejecutarSwapping(idProceso, pagina);
        }
    }

    /**
     * Trae una página del Swap hacia la RAM, seleccionando una página víctima
     * si es que no se encuentran marcos totalmente vacíos.
     */
    private void ejecutarSwapping(String idProceso, Pagina paginaRequerida) {
        int marcoDestino = buscarPrimerMarcoLibreAsilado();

        if (marcoDestino == -1) {
            // RAM Llena: Aplicar algoritmo de reemplazo seleccionado para desalojar
            marcoDestino = (this.algoritmoReemplazo == AlgoritmoReemplazo.FIFO) 
                ? seleccionarVictimaFIFO() 
                : seleccionarVictimaLRU();
            
            // Notificar el desalojo enviando los datos de la víctima hacia el Swap de disco
            MarcoRAM marcoVictima = marcosRAM[marcoDestino];
            discoSwap.enviarASwap(marcoVictima.getIdProcesoAsignado(), marcoVictima.getNumeroPaginaAsignada());
            
            // NOTA: En la integración de la CPU, aquí se actualiza el bit "presente=false" 
            // de la Tabla de Páginas del proceso desalojado.
            marcoVictima.liberar();
        }

        // Retirar la nueva página del disco e ingresarla al marco físico liberado
        discoSwap.retirarDeSwap(idProceso, paginaRequerida.getNumeroPagina());
        marcosRAM[marcoDestino].asignar(idProceso, paginaRequerida.getNumeroPagina());
        
        paginaRequerida.setMarcoFisico(marcoDestino);
        paginaRequerida.setPresente(true);
        
        if (this.algoritmoReemplazo == AlgoritmoReemplazo.FIFO) {
            colaFIFO.add(marcoDestino);
        }
    }

    // --- ALGORITMOS DE UBICACIÓN (POLÍTICAS FIT) ---

    private int buscarBloqueLibre(int tamanoRequerido) {
        List<Integer> bloquesValidos = new ArrayList<>();
        int longitudActual = 0;
        int inicioActual = -1;

        for (int i = 0; i < marcosRAM.length; i++) {
            if (marcosRAM[i].estaLibre()) {
                if (longitudActual == 0) inicioActual = i;
                longitudActual++;
                if (longitudActual >= tamanoRequerido) {
                    bloquesValidos.add(inicioActual);
                }
            } else {
                longitudActual = 0;
            }
        }

        if (bloquesValidos.isEmpty()) return -1;

        if (politicaAsignacion == PoliticaAsignacion.FIRST_FIT) {
            return bloquesValidos.get(0);
        }

        // Evaluar bloques para determinar el mejor ajuste (Best) o peor ajuste (Worst)
        int mejorIndice = bloquesValidos.get(0);
        int extremoTamaño = obtenerLongitudBloqueDesde(mejorIndice);

        for (int indice : bloquesValidos) {
            int len = obtenerLongitudBloqueDesde(indice);
            if (politicaAsignacion == PoliticaAsignacion.BEST_FIT && len < extremoTamaño) {
                extremoTamaño = len;
                mejorIndice = indice;
            } else if (politicaAsignacion == PoliticaAsignacion.WORST_FIT && len > extremoTamaño) {
                extremoTamaño = len;
                mejorIndice = indice;
            }
        }
        return mejorIndice;
    }

    private int obtenerLongitudBloqueDesde(int inicio) {
        int len = 0;
        for (int i = inicio; i < marcosRAM.length && marcosRAM[i].estaLibre(); i++) {
            len++;
        }
        return len;
    }

    private int buscarPrimerMarcoLibreAsilado() {
        for (MarcoRAM marco : marcosRAM) {
            if (marco.estaLibre()) return marco.getIdMarco();
        }
        return -1;
    }

    // --- ALGORITMOS DE REEMPLAZO ---

    private int seleccionarVictimaFIFO() {
        Integer marcoVictima = colaFIFO.poll();
        return (marcoVictima != null) ? marcoVictima : 0;
    }

    private int seleccionarVictimaLRU() {
        // En un caso real iteraríamos las páginas lógicas activas en las tablas,
        // simulamos la expulsión del marco con páginas de mayor antigüedad en la RAM física.
        int marcoVictima = 0;
        long tiempoMasAntiguo = Long.MAX_VALUE;

        // Por simplicidad de diseño, el procesador asignará el timestamp al invocar accederPagina
        for (int i = 0; i < marcosRAM.length; i++) {
            // Mock de seguridad: Expulsar el primer marco activo que encontremos si no hay datos de reloj
            if (!marcosRAM[i].estaLibre()) {
                return i;
            }
        }
        return marcoVictima;
    }

    /**
     * MAPEADOR DIRECTO: Transforma el estado interno del gestor en el DTO exacto 
     * requerido por el archivo unificado de transmisión del Frontend.
     */
    public GestionMemoriaDTO generarGestionMemoriaDTO() {
        List<MarcoRAMDTO> marcosDTO = new ArrayList<>();
        for (MarcoRAM marco : marcosRAM) {
            marcosDTO.add(new MarcoRAMDTO(
                marco.getIdMarco(),
                marco.getIdProcesoAsignado(),
                marco.getNumeroPaginaAsignada()
            ));
        }

        AreaSwapDTO swapDTO = new AreaSwapDTO(
            discoSwap.getPaginas().size(),
            discoSwap.getPaginas()
        );

        return new GestionMemoriaDTO(
            algoritmoReemplazo.toString(),
            totalAccesos,
            pageFaultsTotales,
            porcentajeThrashing,
            marcosDTO,
            swapDTO
        );
    }

    // Getters y Setters de configuración
    public void setPoliticaAsignacion(PoliticaAsignacion p) { this.politicaAsignacion = p; }
    public void setAlgoritmoReemplazo(AlgoritmoReemplazo a) { this.algoritmoReemplazo = a; }
    public void setPorcentajeThrashing(double pt) { this.porcentajeThrashing = pt; }
    public int getPageFaultsTotales() { return pageFaultsTotales; }
}
    }

    public boolean cargarPaginaEnRAM(String idProceso, int numeroPagina) {
        // TODO: Fase 3 - Cargar en marco libre o ejecutar reemplazo (FIFO/LRU/Reloj)
        return true;
    }
}

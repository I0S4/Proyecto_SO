package memoria;

import dto.EstadoSistemaDTO.AreaSwapDTO;
import dto.EstadoSistemaDTO.GestionMemoriaDTO;
import dto.EstadoSistemaDTO.MarcoRAMDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import strategy.AlgoritmoReemplazo;

/**
 * Orquestador principal de la Memoria Virtual del Simulador.
 *
 * Fase 2 - Backend 2:
 *   - Clase de partición de memoria (cálculo de tamaño stack/heap).
 *   - Mapa de Bits implícito: cada MarcoRAM representa un bit (libre/ocupado).
 *   - División estricta en Marcos (RAM): instanciados en el constructor.
 *   - Instanciación del Área de Swap (Disco): DiscoSwap encapsula las páginas en disco.
 *   - Lógica de la Tabla de Páginas: inicializarEspacioProceso construye y registra la tabla.
 *
 * Fase 3 (ya adelantado, no modificar):
 *   - Políticas de asignación First/Best/Worst Fit.
 *   - Algoritmos de reemplazo FIFO y LRU.
 *   - Generación del DTO unificado.
 */
public class GestorMemoriaVirtual {

    public enum PoliticaAsignacion { FIRST_FIT, BEST_FIT, WORST_FIT }

    private final int tamanoRamBytes;
    private final int tamanoPaginaBytes;
    private final MarcoRAM[] marcosRAM;
    private final DiscoSwap discoSwap;

    /**
     * Registro centralizado de tablas de páginas por proceso.
     * Permite que ejecutarSwapping actualice correctamente el Bit P de la víctima.
     */
    private final Map<String, TablaPaginas> tablasPaginas;

    // Configuración de políticas activas en la simulación
    private PoliticaAsignacion politicaAsignacion;
    private AlgoritmoReemplazo algoritmoReemplazo;

    // Métricas globales requeridas por el DTO del sistema
    private int totalAccesos;
    private int pageFaultsTotales;
    private double porcentajeThrashing;

    // Cola FIFO interna para el algoritmo de reemplazo por defecto
    private final Queue<Integer> colaFIFO;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public GestorMemoriaVirtual(int tamanoRamBytes, int tamanoPaginaBytes) {
        this.tamanoRamBytes    = tamanoRamBytes;
        this.tamanoPaginaBytes = tamanoPaginaBytes;
        this.tablasPaginas     = new HashMap<>();
        this.colaFIFO          = new LinkedList<>();
        this.totalAccesos      = 0;
        this.pageFaultsTotales = 0;
        this.porcentajeThrashing = 0.0;

        int cantidadMarcos = tamanoRamBytes / tamanoPaginaBytes;
        this.marcosRAM = new MarcoRAM[cantidadMarcos];
        for (int i = 0; i < cantidadMarcos; i++) {
            this.marcosRAM[i] = new MarcoRAM(i);
        }

        this.discoSwap          = new DiscoSwap();
        this.politicaAsignacion = PoliticaAsignacion.FIRST_FIT;

        // Algoritmo de reemplazo por defecto: FIFO (implementado como lambda)
        this.algoritmoReemplazo = marcos -> seleccionarVictimaFIFO();
    }

    // -------------------------------------------------------------------------
    // Fase 2 – Backend 2: Partición de Memoria y Tabla de Páginas
    // -------------------------------------------------------------------------

    /**
     * Calcula el desperdicio por fragmentación interna (stack + heap)
     * y lo devuelve como porcentaje con 2 decimales de precisión.
     */
    public double calcularDesperdicioBytes(int bytesStack, int bytesHeap) {
        int residuoStack = bytesStack % tamanoPaginaBytes;
        int residuoHeap  = bytesHeap  % tamanoPaginaBytes;

        int desperdicioStack = (residuoStack == 0) ? 0 : (tamanoPaginaBytes - residuoStack);
        int desperdicioHeap  = (residuoHeap  == 0) ? 0 : (tamanoPaginaBytes - residuoHeap);

        int desperdicioTotal     = desperdicioStack + desperdicioHeap;
        int totalAsignadoBytes   = bytesStack + bytesHeap + desperdicioTotal;

        if (totalAsignadoBytes == 0) return 0.0;

        double porcentaje = ((double) desperdicioTotal / totalAsignadoBytes) * 100.0;
        return Math.round(porcentaje * 100.0) / 100.0;
    }

    /**
     * Reserva espacio inicial para las páginas de un proceso usando la política Fit activa.
     * Si no existe bloque contiguo suficiente, las páginas van directamente al Swap.
     *
     * Registra la tabla en el mapa interno para que ejecutarSwapping pueda actualizar
     * el Bit P de las páginas víctimas correctamente.
     *
     * @return La TablaPaginas construida y registrada para el proceso.
     */
    public TablaPaginas inicializarEspacioProceso(String idProceso, int bytesStack, int bytesHeap) {
        TablaPaginas tabla = new TablaPaginas(idProceso);

        int paginasStack          = (int) Math.ceil((double) bytesStack / tamanoPaginaBytes);
        int paginasHeap           = (int) Math.ceil((double) bytesHeap  / tamanoPaginaBytes);
        int totalPaginasRequeridas = paginasStack + paginasHeap;

        for (int i = 0; i < paginasStack; i++) tabla.agregarPagina("STACK");
        for (int i = 0; i < paginasHeap;  i++) tabla.agregarPagina("HEAP");

        int indiceMarcoInicio = buscarBloqueLibre(totalPaginasRequeridas);

        for (Pagina pagina : tabla.getPaginas()) {
            if (indiceMarcoInicio != -1) {
                marcosRAM[indiceMarcoInicio].asignar(idProceso, pagina.getNumeroPagina());
                pagina.setMarcoFisico(indiceMarcoInicio);
                pagina.setPresente(true);

                if (!colaFIFO.contains(indiceMarcoInicio)) {
                    colaFIFO.add(indiceMarcoInicio);
                }
                indiceMarcoInicio++;
            } else {
                discoSwap.enviarASwap(idProceso, pagina.getNumeroPagina());
                pagina.setMarcoFisico(-1);
                pagina.setPresente(false);
            }
        }

        // Registrar la tabla para mantener consistencia del Bit P durante swapping
        tablasPaginas.put(idProceso, tabla);
        return tabla;
    }

    /**
     * Método de acceso directo requerido por el verificador y el Frontend.
     * Carga la página lógica indicada en RAM (si no está ya presente).
     *
     * @param idProceso    Identificador del proceso.
     * @param numeroPagina Número de página lógica a cargar.
     * @return true si la operación se completó sin errores.
     */
    public boolean cargarPaginaEnRAM(String idProceso, int numeroPagina) {
        // Obtener o crear la tabla de páginas del proceso
        TablaPaginas tabla = tablasPaginas.computeIfAbsent(
            idProceso, id -> new TablaPaginas(id)
        );

        // Buscar la página en la tabla; si no existe, crearla como página HEAP sin marco
        Pagina pagina = tabla.getPaginas().stream()
            .filter(p -> p.getNumeroPagina() == numeroPagina)
            .findFirst()
            .orElse(null);

        if (pagina == null) {
            tabla.agregarPagina("HEAP");
            pagina = tabla.getPaginas().get(tabla.getPaginas().size() - 1);
        }

        // Acceder a la página: si no está presente, dispara page fault y swapping
        accederPagina(idProceso, pagina);
        return true;
    }

    /**
     * Simula el acceso a una página lógica del proceso.
     * Si no está presente en RAM (Bit P = 0), genera un Page Fault e inicia swapping.
     */
    public void accederPagina(String idProceso, Pagina pagina) {
        this.totalAccesos++;
        pagina.registrarAcceso();

        if (!pagina.isPresente()) {
            this.pageFaultsTotales++;
            ejecutarSwapping(idProceso, pagina);
        }
    }

    /**
     * Trae una página del Swap hacia la RAM.
     * Si la RAM está llena, aplica el algoritmo de reemplazo seleccionado y
     * actualiza el Bit P (presente = false, marco = -1) de la página víctima
     * en la tabla de páginas de su proceso.
     */
    private void ejecutarSwapping(String idProceso, Pagina paginaRequerida) {
        int marcoDestino = buscarPrimerMarcoLibreAsilado();

        if (marcoDestino == -1) {
            // RAM llena: seleccionar víctima con el algoritmo activo
            marcoDestino = this.algoritmoReemplazo.seleccionarVictima(marcosRAM);

            MarcoRAM marcoVictima = marcosRAM[marcoDestino];
            String   idVictima    = marcoVictima.getIdProcesoAsignado();
            int      paginaVictima = marcoVictima.getNumeroPaginaAsignada();

            // Enviar la página víctima al disco
            discoSwap.enviarASwap(idVictima, paginaVictima);

            // Actualizar el Bit P y marco físico en la Tabla de Páginas del proceso víctima
            TablaPaginas tablaVictima = tablasPaginas.get(idVictima);
            if (tablaVictima != null) {
                for (Pagina p : tablaVictima.getPaginas()) {
                    if (p.getNumeroPagina() == paginaVictima) {
                        p.setPresente(false);
                        p.setMarcoFisico(-1);
                        break;
                    }
                }
            }

            marcoVictima.liberar();
        }

        // Retirar la página requerida del Swap y cargarla en el marco liberado
        discoSwap.retirarDeSwap(idProceso, paginaRequerida.getNumeroPagina());
        marcosRAM[marcoDestino].asignar(idProceso, paginaRequerida.getNumeroPagina());
        paginaRequerida.setMarcoFisico(marcoDestino);
        paginaRequerida.setPresente(true);
        colaFIFO.add(marcoDestino);
    }

    // -------------------------------------------------------------------------
    // Algoritmos de Ubicación (Políticas Fit) – Fase 3 adelantado
    // -------------------------------------------------------------------------

    private int buscarBloqueLibre(int tamanoRequerido) {
        List<Integer> bloquesValidos = new ArrayList<>();
        int longitudActual = 0;
        int inicioActual   = -1;

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

        int mejorIndice  = bloquesValidos.get(0);
        int extremoTamano = obtenerLongitudBloqueDesde(mejorIndice);

        for (int indice : bloquesValidos) {
            int len = obtenerLongitudBloqueDesde(indice);
            if (politicaAsignacion == PoliticaAsignacion.BEST_FIT && len < extremoTamano) {
                extremoTamano = len;
                mejorIndice   = indice;
            } else if (politicaAsignacion == PoliticaAsignacion.WORST_FIT && len > extremoTamano) {
                extremoTamano = len;
                mejorIndice   = indice;
            }
        }
        return mejorIndice;
    }

    private int obtenerLongitudBloqueDesde(int inicio) {
        int len = 0;
        for (int i = inicio; i < marcosRAM.length && marcosRAM[i].estaLibre(); i++) len++;
        return len;
    }

    private int buscarPrimerMarcoLibreAsilado() {
        for (MarcoRAM marco : marcosRAM) {
            if (marco.estaLibre()) return marco.getIdMarco();
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // Algoritmos de Reemplazo – Fase 3 adelantado
    // -------------------------------------------------------------------------

    private int seleccionarVictimaFIFO() {
        Integer marcoVictima = colaFIFO.poll();
        return (marcoVictima != null) ? marcoVictima : 0;
    }

    private int seleccionarVictimaLRU() {
        // Mock: expulsa el primer marco ocupado encontrado.
        // La integración completa del timestamp se realiza en Fase 3/4.
        for (int i = 0; i < marcosRAM.length; i++) {
            if (!marcosRAM[i].estaLibre()) return i;
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Generación del DTO – Fase 3 adelantado
    // -------------------------------------------------------------------------

    /**
     * Transforma el estado interno del gestor en el DTO exacto requerido por el Frontend.
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

    // -------------------------------------------------------------------------
    // Getters y Setters de configuración
    // -------------------------------------------------------------------------

    public int getTamanoPaginaBytes()      { return tamanoPaginaBytes; }
    public int getPageFaultsTotales()      { return pageFaultsTotales; }
    public AlgoritmoReemplazo getAlgoritmoReemplazo() { return algoritmoReemplazo; }

    public void setPoliticaAsignacion(PoliticaAsignacion p) { this.politicaAsignacion = p; }
    public void setAlgoritmoReemplazo(AlgoritmoReemplazo a) { this.algoritmoReemplazo = a; }
    public void setPorcentajeThrashing(double pt)           { this.porcentajeThrashing = pt; }
}
    }

    public boolean cargarPaginaEnRAM(String idProceso, int numeroPagina) {
        // TODO: Fase 3 - Cargar en marco libre o ejecutar reemplazo (FIFO/LRU/Reloj)
        return true;
    }
}

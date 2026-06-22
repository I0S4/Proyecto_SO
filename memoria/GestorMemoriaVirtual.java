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
import interrupciones.GestorInterrupciones;
import interrupciones.Interrupcion;
import interrupciones.Interrupcion.TipoInterrupcion;

/**
 * Orquestador principal de la Memoria Virtual del Simulador.
 * * Implementa de forma estricta todos los requerimientos de la Fase 3:
 * - Políticas de asignación inicial continuas (First, Best y Worst Fit)
 * corregidas.
 * - Algoritmos de reemplazo integrados con la interfaz Strategy (FIFO y LRU
 * Real).
 * - Sincronización del Bit P y mapeo directo a las estructuras DTO del sistema.
 */
public class GestorMemoriaVirtual {

    public enum PoliticaAsignacion {
        FIRST_FIT, BEST_FIT, WORST_FIT
    }

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
    private GestorInterrupciones gestorInterrupciones;

    // Métricas globales requeridas por el DTO del sistema
    private int totalAccesos;
    private int pageFaultsTotales;
    private double porcentajeThrashing;

    // Cola FIFO interna para el algoritmo de reemplazo por defecto
    private final Queue<Integer> colaFIFO;

    public void setGestorInterrupciones(GestorInterrupciones gestorInterrupciones) {
        this.gestorInterrupciones = gestorInterrupciones;
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public GestorMemoriaVirtual(int tamanoRamBytes, int tamanoPaginaBytes) {
        this.tamanoRamBytes = tamanoRamBytes;
        this.tamanoPaginaBytes = tamanoPaginaBytes;
        this.tablasPaginas = new HashMap<>();
        this.colaFIFO = new LinkedList<>();
        this.totalAccesos = 0;
        this.pageFaultsTotales = 0;
        this.porcentajeThrashing = 0.0;

        int cantidadMarcos = tamanoRamBytes / tamanoPaginaBytes;
        this.marcosRAM = new MarcoRAM[cantidadMarcos];
        for (int i = 0; i < cantidadMarcos; i++) {
            this.marcosRAM[i] = new MarcoRAM(i);
        }

        this.discoSwap = new DiscoSwap();
        this.politicaAsignacion = PoliticaAsignacion.FIRST_FIT;

        // Por defecto se configura FIFO mediante la interfaz funcional
        configurarAlgoritmoPorNombre("FIFO");
    }

    // -------------------------------------------------------------------------
    // Fase 2 y 3 – Ubicación, Partición y Tabla de Páginas
    // -------------------------------------------------------------------------

    /**
     * Calcula el desperdicio por fragmentación interna (stack + heap)
     * y lo devuelve como porcentaje con 2 decimales de precisión.
     */
    public double calcularDesperdicioBytes(int bytesStack, int bytesHeap) {
        int residuoStack = bytesStack % tamanoPaginaBytes;
        int residuoHeap = bytesHeap % tamanoPaginaBytes;

        int desperdicioStack = (residuoStack == 0) ? 0 : (tamanoPaginaBytes - residuoStack);
        int desperdicioHeap = (residuoHeap == 0) ? 0 : (tamanoPaginaBytes - residuoHeap);

        int desperdicioTotal = desperdicioStack + desperdicioHeap;
        int totalAsignadoBytes = bytesStack + bytesHeap + desperdicioTotal;

        if (totalAsignadoBytes == 0)
            return 0.0;

        double porcentaje = ((double) desperdicioTotal / totalAsignadoBytes) * 100.0;
        return Math.round(porcentaje * 100.0) / 100.0;
    }

    /**
     * Reserva espacio inicial para las páginas de un proceso.
     * Asigna cada página al primer marco libre disponible de forma individual
     * (la paginación no requiere contigüidad física). Si no hay marcos disponibles,
     * las páginas restantes van directamente al Swap.
     *
     * Registra la tabla en el mapa interno para que ejecutarSwapping pueda
     * actualizar
     * el Bit P de las páginas víctimas correctamente.
     *
     * @return La TablaPaginas construida y registrada para el proceso.
     */
    public TablaPaginas inicializarEspacioProceso(String idProceso, int bytesStack, int bytesHeap) {
        TablaPaginas tabla = new TablaPaginas(idProceso);

        int paginasStack = (int) Math.ceil((double) bytesStack / tamanoPaginaBytes);
        int paginasHeap = (int) Math.ceil((double) bytesHeap / tamanoPaginaBytes);
        int totalPaginasRequeridas = paginasStack + paginasHeap;

        for (int i = 0; i < paginasStack; i++)
            tabla.agregarPagina("STACK");
        for (int i = 0; i < paginasHeap; i++)
            tabla.agregarPagina("HEAP");

        for (Pagina pagina : tabla.getPaginas()) {
            // Buscar un marco libre de forma individual (paginación no requiere
            // contigüidad)
            int marcoLibre = buscarPrimerMarcoLibreAsilado();
            if (marcoLibre != -1) {
                marcosRAM[marcoLibre].asignar(idProceso, pagina.getNumeroPagina());
                pagina.setMarcoFisico(marcoLibre);
                pagina.setPresente(true);

                if (!colaFIFO.contains(marcoLibre)) {
                    colaFIFO.add(marcoLibre);
                }
            } else {
                discoSwap.enviarASwap(idProceso, pagina.getNumeroPagina());
                pagina.setMarcoFisico(-1);
                pagina.setPresente(false);
            }
        }

        tablasPaginas.put(idProceso, tabla);
        return tabla;
    }

    /**
     * Método de acceso directo requerido por el simulador y el Frontend.
     * Carga la página lógica indicada en RAM (si no está ya presente).
     */
    public boolean cargarPaginaEnRAM(String idProceso, int numeroPagina) {
        TablaPaginas tabla = tablasPaginas.computeIfAbsent(
                idProceso, id -> new TablaPaginas(id));

        Pagina pagina = tabla.getPaginas().stream()
                .filter(p -> p.getNumeroPagina() == numeroPagina)
                .findFirst()
                .orElse(null);

        if (pagina == null) {
            tabla.agregarPagina("HEAP");
            pagina = tabla.getPaginas().get(tabla.getPaginas().size() - 1);
        }

        accederPagina(idProceso, pagina);
        return true;
    }

    /**
     * Accede a una dirección de memoria lógica del proceso.
     * Traduce dirección a número de página lógica, busca o crea la página y la
     * accede.
     */
    public void accederDireccion(String idProceso, long direccionLogica) {
        if (tamanoPaginaBytes <= 0) {
            throw new IllegalStateException("El tamaño de página debe ser mayor a 0.");
        }
        int numeroPagina = (int) (direccionLogica / tamanoPaginaBytes);
        TablaPaginas tabla = tablasPaginas.get(idProceso);
        if (tabla == null) {
            tabla = inicializarEspacioProceso(idProceso, tamanoPaginaBytes, 0);
        }

        Pagina pagina = null;
        for (Pagina p : tabla.getPaginas()) {
            if (p.getNumeroPagina() == numeroPagina) {
                pagina = p;
                break;
            }
        }

        if (pagina == null) {
            while (tabla.getPaginas().size() <= numeroPagina) {
                tabla.agregarPagina("HEAP");
            }
            pagina = tabla.getPaginas().get(numeroPagina);
        }

        accederPagina(idProceso, pagina);
    }

    /**
     * Simula el acceso a una página lógica del proceso.
     * Si no está presente en RAM (Bit P = 0), genera un Page Fault e inicia
     * swapping.
     */
    public void accederPagina(String idProceso, Pagina pagina) {
        this.totalAccesos++;
        pagina.registrarAcceso(); // Actualiza el bit R y el timestamp en nanosegundos

        if (!pagina.isPresente()) {
            this.pageFaultsTotales++;
            if (gestorInterrupciones != null) {
                gestorInterrupciones.notificar(new Interrupcion(
                        TipoInterrupcion.PAGE_FAULT,
                        "Page Fault en proceso " + idProceso + ", página " + pagina.getNumeroPagina(),
                        idProceso));
            }
            ejecutarSwapping(idProceso, pagina);
        }
    }

    /**
     * Trae una página del Swap hacia la RAM.
     * Si la RAM está llena, aplica el algoritmo de reemplazo seleccionado mediante
     * la interfaz Strategy
     * y actualiza el Bit P (presente = false, marco = -1) de la página víctima.
     */
    private void ejecutarSwapping(String idProceso, Pagina paginaRequerida) {
        int marcoDestino = buscarPrimerMarcoLibreAsilado();

        if (marcoDestino == -1) {
            // RAM llena: seleccionar víctima invocando la estrategia funcional activa
            marcoDestino = this.algoritmoReemplazo.seleccionarVictima(marcosRAM);

            MarcoRAM marcoVictima = marcosRAM[marcoDestino];
            String idVictima = marcoVictima.getIdProcesoAsignado();
            int paginaVictima = marcoVictima.getNumeroPaginaAsignada();

            discoSwap.enviarASwap(idVictima, paginaVictima);

            // Apagar bits de control en la tabla de la página expulsada
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

        discoSwap.retirarDeSwap(idProceso, paginaRequerida.getNumeroPagina());
        marcosRAM[marcoDestino].asignar(idProceso, paginaRequerida.getNumeroPagina());
        paginaRequerida.setMarcoFisico(marcoDestino);
        paginaRequerida.setPresente(true);
        colaFIFO.add(marcoDestino);
    }

    // -------------------------------------------------------------------------
    // Algoritmos de Ubicación Corregidos (Políticas Fit)
    // -------------------------------------------------------------------------

    /**
     * Busca un bloque libre contiguo según la política activa (First, Best o Worst
     * Fit).
     * Garantiza comparaciones y extremos inicializados correctamente.
     */
    private int buscarBloqueLibre(int tamanoRequerido) {
        List<Integer> bloquesValidos = new ArrayList<>();
        int longitudActual = 0;
        int inicioActual = -1;

        // Identificar los índices de inicio de todos los bloques que cumplen el tamaño
        // requerido
        for (int i = 0; i < marcosRAM.length; i++) {
            if (marcosRAM[i].estaLibre()) {
                if (longitudActual == 0)
                    inicioActual = i;
                longitudActual++;
                if (longitudActual >= tamanoRequerido) {
                    // Guardamos el inicio si es el primer momento en que este bloque alcanza el
                    // tamaño
                    if (!bloquesValidos.contains(inicioActual)) {
                        bloquesValidos.add(inicioActual);
                    }
                }
            } else {
                longitudActual = 0;
            }
        }

        if (bloquesValidos.isEmpty())
            return -1;

        // First Fit: Retorna el primer bloque encontrado
        if (politicaAsignacion == PoliticaAsignacion.FIRST_FIT) {
            return bloquesValidos.get(0);
        }

        int mejorIndice = bloquesValidos.get(0);
        int extremoTamano = obtenerLongitudBloqueDesde(mejorIndice);

        // Evaluar Best Fit y Worst Fit con inicializaciones reales
        for (int indice : bloquesValidos) {
            int len = obtenerLongitudBloqueDesde(indice);
            if (politicaAsignacion == PoliticaAsignacion.BEST_FIT) {
                if (len < extremoTamano) {
                    extremoTamano = len;
                    mejorIndice = indice;
                }
            } else if (politicaAsignacion == PoliticaAsignacion.WORST_FIT) {
                if (len > extremoTamano) {
                    extremoTamano = len;
                    mejorIndice = indice;
                }
            }
        }
        return mejorIndice;
    }

    private int obtenerLongitudBloqueDesde(int inicio) {
        int len = 0;
        for (int i = inicio; i < marcosRAM.length && marcosRAM[i].estaLibre(); i++)
            len++;
        return len;
    }

    private int buscarPrimerMarcoLibreAsilado() {
        for (MarcoRAM marco : marcosRAM) {
            if (marco.estaLibre())
                return marco.getIdMarco();
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // Métodos de Selección de Víctimas (Estrategias de Reemplazo)
    // -------------------------------------------------------------------------

    private int seleccionarVictimaFIFO() {
        Integer marcoVictima = colaFIFO.poll();
        return (marcoVictima != null) ? marcoVictima : 0;
    }

    /**
     * Implementación determinista de LRU Real.
     * Analiza los timestamps en nanosegundos de las páginas presentes en los marcos
     * ocupados.
     */
    private int seleccionarVictimaLRU() {
        // LRU real: expulsa el marco cuya página fue accedida hace más tiempo
        // usando el campo timestampAcceso de Pagina (actualizado en registrarAcceso).
        int marcoVictima = -1;
        long timestampMinimo = Long.MAX_VALUE;

        for (int i = 0; i < marcosRAM.length; i++) {
            MarcoRAM marco = marcosRAM[i];
            if (!marco.estaLibre()) {
                TablaPaginas tabla = tablasPaginas.get(marco.getIdProcesoAsignado());
                if (tabla != null) {
                    for (Pagina p : tabla.getPaginas()) {
                        if (p.getNumeroPagina() == marco.getNumeroPaginaAsignada()) {
                            if (p.getTimestampAcceso() < timestampMinimo) {
                                timestampMinimo = p.getTimestampAcceso();
                                marcoVictima = i;
                            }
                            break;
                        }
                    }
                } else {
                    // No se encontró tabla: usar este marco como fallback
                    if (marcoVictima == -1)
                        marcoVictima = i;
                }
            }
        }
        return (marcoVictima != -1) ? marcoVictima : 0;
    }

    // -------------------------------------------------------------------------
    // Generación del DTO e Integración Externa
    // -------------------------------------------------------------------------

    public GestionMemoriaDTO generarGestionMemoriaDTO() {
        List<MarcoRAMDTO> marcosDTO = new ArrayList<>();
        for (MarcoRAM marco : marcosRAM) {
            marcosDTO.add(new MarcoRAMDTO(
                    marco.getIdMarco(),
                    marco.getIdProcesoAsignado(),
                    marco.getNumeroPaginaAsignada()));
        }

        AreaSwapDTO swapDTO = new AreaSwapDTO(
                discoSwap.getPaginas().size(),
                discoSwap.getPaginas());

        // Muestra de manera unificada la combinación de políticas activa
        String configuracionEstrategia = politicaAsignacion.toString() + " / " + nombreAlgoritmoActivo;

        return new GestionMemoriaDTO(
                configuracionEstrategia,
                totalAccesos,
                pageFaultsTotales,
                porcentajeThrashing,
                marcosDTO,
                swapDTO);
    }

    /**
     * Permite cambiar dinámicamente el comportamiento de reemplazo desde el
     * controlador central,
     * inyectando las expresiones lambda correspondientes que implementan la
     * interfaz Strategy.
     */
    public void configurarAlgoritmoPorNombre(String nombre) {
        if (nombre == null)
            return;

        if (nombre.equalsIgnoreCase("LRU")) {
            this.algoritmoReemplazo = marcos -> seleccionarVictimaLRU();
            this.nombreAlgoritmoActivo = "LRU";
        } else {
            this.algoritmoReemplazo = marcos -> seleccionarVictimaFIFO();
            this.nombreAlgoritmoActivo = "FIFO";
        }
    }

    // Getters y Setters base
    public int getTamanoPaginaBytes() {
        return tamanoPaginaBytes;
    }

    public int getPageFaultsTotales() {
        return pageFaultsTotales;
    }

    public AlgoritmoReemplazo getAlgoritmoReemplazo() {
        return algoritmoReemplazo;
    }

    public void setPoliticaAsignacion(PoliticaAsignacion p) {
        this.politicaAsignacion = p;
    }

    public void setAlgoritmoReemplazo(AlgoritmoReemplazo a) {
        this.algoritmoReemplazo = a;
        this.nombreAlgoritmoActivo = "CUSTOM_STRATEGY";
    }

    public void setPorcentajeThrashing(double pt) {
        this.porcentajeThrashing = pt;
    }
}
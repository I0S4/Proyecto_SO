package memoria;

import core.Proceso;
import strategy.AlgoritmoReemplazo;

/**
 * Módulo de Memoria Virtual que actúa como un Facade.
 * Oculta la complejidad de la traducción de direcciones, la comunicación con el disco de swap,
 * la RAM física (GestorMemoriaPrincipal) y la aplicación del algoritmo de reemplazo de páginas.
 */
public class GestorMemoriaVirtual {
    private final DiscoSwap discoSwap;
    private final int tamanoPaginaBytes;
    private AlgoritmoReemplazo algoritmoReemplazo; // Strategy Pattern

    public GestorMemoriaVirtual(int tamanoRAMBytes, int tamanoPaginaBytes) {
        this.tamanoPaginaBytes = tamanoPaginaBytes;
        this.discoSwap = new DiscoSwap();
        // Inicializa el Singleton del Gestor de Memoria Principal
        GestorMemoriaPrincipal.getInstancia().inicializar(tamanoRAMBytes, tamanoPaginaBytes);
    }

    public void setAlgoritmoReemplazo(AlgoritmoReemplazo algoritmoReemplazo) {
        this.algoritmoReemplazo = algoritmoReemplazo;
    }

    public AlgoritmoReemplazo getAlgoritmoReemplazo() {
        return algoritmoReemplazo;
    }

    public DiscoSwap getDiscoSwap() {
        return discoSwap;
    }

    public int getTamanoPaginaBytes() {
        return tamanoPaginaBytes;
    }

    /**
     * Carga una página lógica de un proceso en la memoria física RAM.
     * En caso de no haber marcos disponibles, aplicará el algoritmo de reemplazo asignado.
     */
    public boolean cargarPaginaEnRAM(String idProceso, int numeroPagina) {
        // TODO: Fase 3 - Cargar en marco libre o ejecutar reemplazo (FIFO/LRU/Reloj)
        return true;
    }
}

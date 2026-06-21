package strategy;

import memoria.MarcoRAM;

/**
 * Interfaz Strategy para algoritmos de reemplazo de páginas en memoria virtual (ej: FIFO, LRU, Reloj).
 */
public interface AlgoritmoReemplazo {
    /**
     * Selecciona el marco de página que debe ser desalojado (víctima) de la RAM física.
     * 
     * @param marcos RAM física actual.
     * @return El índice del marco seleccionado para ser desalojado.
     */
    int seleccionarVictima(MarcoRAM[] marcos);
}

package strategy;

import core.Proceso;
import java.util.List;

/**
 * Interfaz Strategy para algoritmos de planificación de la CPU (ej: FIFO, Round Robin).
 */
public interface AlgoritmoPlanificacion {
    /**
     * Selecciona el siguiente proceso a ejecutar de la lista de procesos listos.
     * 
     * @param listos Lista de procesos en estado LISTO.
     * @return El proceso seleccionado para ejecución, o null si la lista está vacía.
     */
    Proceso seleccionarSiguiente(List<Proceso> listos);
}

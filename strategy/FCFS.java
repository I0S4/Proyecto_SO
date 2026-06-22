package strategy;

import core.Proceso;
import java.util.List;

/**
 * Algoritmo First-Come, First-Served (FCFS).
 * Selecciona el proceso que llegó primero a la cola de listos (el primer elemento).
 * Es no expropiativo.
 */
public class FCFS implements AlgoritmoPlanificacion {
    @Override
    public Proceso seleccionarSiguiente(List<Proceso> listos) {
        if (listos == null || listos.isEmpty()) {
            return null;
        }
        return listos.get(0);
    }
}

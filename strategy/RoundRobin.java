package strategy;

import core.Proceso;
import java.util.List;

/**
 * Algoritmo Round Robin (RR).
 * Selecciona el proceso al frente de la cola de listos (el primer elemento).
 * Retorna true para usaQuantum().
 */
public class RoundRobin implements AlgoritmoPlanificacion {
    @Override
    public Proceso seleccionarSiguiente(List<Proceso> listos) {
        if (listos == null || listos.isEmpty()) {
            return null;
        }
        return listos.get(0);
    }

    @Override
    public boolean usaQuantum() {
        return true;
    }
}

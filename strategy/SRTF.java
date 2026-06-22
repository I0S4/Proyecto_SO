package strategy;

import core.Proceso;
import java.util.List;

/**
 * Algoritmo Shortest Remaining Time First (SRTF) / SJF Expropiativo.
 * Selecciona el proceso con menor tiempo de ráfaga restante (tiempoRafagaRestante).
 * Retorna true para esExpropiativo().
 */
public class SRTF implements AlgoritmoPlanificacion {
    @Override
    public Proceso seleccionarSiguiente(List<Proceso> listos) {
        if (listos == null || listos.isEmpty()) {
            return null;
        }
        Proceso seleccionado = listos.get(0);
        for (int i = 1; i < listos.size(); i++) {
            Proceso p = listos.get(i);
            if (p.getTiempoRafagaRestante() < seleccionado.getTiempoRafagaRestante()) {
                seleccionado = p;
            }
        }
        return seleccionado;
    }

    @Override
    public boolean esExpropiativo() {
        return true;
    }
}

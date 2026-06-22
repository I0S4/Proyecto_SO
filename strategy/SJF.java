package strategy;

import core.Proceso;
import java.util.List;

/**
 * Algoritmo Shortest Job First (SJF) No Expropiativo.
 * Selecciona el proceso con menor tiempo de ráfaga total (totalInstrucciones).
 */
public class SJF implements AlgoritmoPlanificacion {
    @Override
    public Proceso seleccionarSiguiente(List<Proceso> listos) {
        if (listos == null || listos.isEmpty()) {
            return null;
        }
        Proceso seleccionado = listos.get(0);
        for (int i = 1; i < listos.size(); i++) {
            Proceso p = listos.get(i);
            if (p.getTotalInstrucciones() < seleccionado.getTotalInstrucciones()) {
                seleccionado = p;
            }
        }
        return seleccionado;
    }
}

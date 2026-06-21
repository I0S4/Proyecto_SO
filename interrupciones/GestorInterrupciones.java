package interrupciones;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Sujeto (Subject) o despachador de eventos del patrón Observer.
 * Permite registrar observadores y notificarles sobre interrupciones del sistema.
 */
public class GestorInterrupciones {
    private final Map<Interrupcion.TipoInterrupcion, List<EscuchadorInterrupcion>> suscriptores;

    public GestorInterrupciones() {
        this.suscriptores = new EnumMap<>(Interrupcion.TipoInterrupcion.class);
        for (Interrupcion.TipoInterrupcion tipo : Interrupcion.TipoInterrupcion.values()) {
            suscriptores.put(tipo, new ArrayList<>());
        }
    }

    public synchronized void suscribir(Interrupcion.TipoInterrupcion tipo, EscuchadorInterrupcion escuchador) {
        if (escuchador != null) {
            suscriptores.get(tipo).add(escuchador);
        }
    }

    public synchronized void desuscribir(Interrupcion.TipoInterrupcion tipo, EscuchadorInterrupcion escuchador) {
        if (escuchador != null) {
            suscriptores.get(tipo).remove(escuchador);
        }
    }

    public synchronized void notificar(Interrupcion interrupcion) {
        if (interrupcion == null) return;
        List<EscuchadorInterrupcion> escuchadores = suscriptores.get(interrupcion.tipo());
        if (escuchadores != null) {
            List<EscuchadorInterrupcion> copia = new ArrayList<>(escuchadores);
            for (EscuchadorInterrupcion escuchador : copia) {
                escuchador.procesarInterrupcion(interrupcion);
            }
        }
    }
}

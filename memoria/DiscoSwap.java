package memoria;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Representa el área de Swap en disco para almacenar páginas desalojadas de RAM.
 */
public class DiscoSwap {
    // Registra los identificadores de páginas en swap (Ej: "P1-Pag-0")
    private final Set<String> paginasEnSwap;

    public DiscoSwap() {
        this.paginasEnSwap = new HashSet<>();
    }

    public void enviarASwap(String idProceso, int numeroPagina) {
        paginasEnSwap.add(idProceso + "-Pag-" + numeroPagina);
    }

    public void retirarDeSwap(String idProceso, int numeroPagina) {
        paginasEnSwap.remove(idProceso + "-Pag-" + numeroPagina);
    }

    public boolean estaEnSwap(String idProceso, int numeroPagina) {
        return paginasEnSwap.contains(idProceso + "-Pag-" + numeroPagina);
    }

    public Set<String> getPaginasEnSwap() {
        return Collections.unmodifiableSet(paginasEnSwap);
    }
}
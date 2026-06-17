package memoria;

import java.util.HashSet;
import java.util.Set;

public class DiscoSwap {
    // Registra los identificadores de páginas en swap (Ej: "P1-Pag0")
    private Set<String> paginasEnSwap;

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
}
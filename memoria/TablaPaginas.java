package memoria;

import java.util.HashMap;
import java.util.Map;

public class TablaPaginas {
    private String idProceso;
    // Mapea: Número de Página Lógica -> Datos de la Página (Marco, bit Presente, etc.)
    private Map<Integer, EntradaTablaPaginas> entradas;

    public TablaPaginas(String idProceso) {
        this.idProceso = idProceso;
        this.entradas = new HashMap<>();
    }

    // Estructura interna para modelar los bits de control de hardware
    public static class EntradaTablaPaginas {
        public int numeroMarco = -1;  // -1 significa que no está en RAM
        public boolean presente = false;  // Bit P (0 = Swap, 1 = RAM)
        public boolean referencia = false; // Bit R (Para algoritmos de reemplazo)
        public long timestampAcceso = 0;   // Para LRU
    }

    public Map<Integer, EntradaTablaPaginas> getEntradas() { return entradas; }
}
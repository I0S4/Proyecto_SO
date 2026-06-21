package memoria;

import java.util.HashMap;
import java.util.Map;

/**
 * Tabla de páginas asociada a un proceso para la traducción de direcciones lógicas a físicas.
 */
public class TablaPaginas {
    private final String idProceso;
    private final Map<Integer, EntradaTablaPaginas> entradas;

    public TablaPaginas(String idProceso) {
        this.idProceso = idProceso;
        this.entradas = new HashMap<>();
    }

    public String getIdProceso() {
        return idProceso;
    }

    public Map<Integer, EntradaTablaPaginas> getEntradas() {
        return entradas;
    }

    /**
     * Estructura interna para modelar los bits de control de hardware de cada página.
     */
    public static class EntradaTablaPaginas {
        private int numeroMarco = -1;       // -1 significa que no está en RAM
        private boolean presente = false;    // Bit P (false = Swap, true = RAM)
        private boolean referencia = false;  // Bit R (Para algoritmos de reemplazo)
        private long timestampAcceso = 0;    // Para algoritmo LRU

        public int getNumeroMarco() {
            return numeroMarco;
        }

        public void setNumeroMarco(int numeroMarco) {
            this.numeroMarco = numeroMarco;
        }

        public boolean isPresente() {
            return presente;
        }

        public void setPresente(boolean presente) {
            this.presente = presente;
        }

        public boolean isReferencia() {
            return referencia;
        }

        public void setReferencia(boolean referencia) {
            this.referencia = referencia;
        }

        public long getTimestampAcceso() {
            return timestampAcceso;
        }

        public void setTimestampAcceso(long timestampAcceso) {
            this.timestampAcceso = timestampAcceso;
        }
    }
}
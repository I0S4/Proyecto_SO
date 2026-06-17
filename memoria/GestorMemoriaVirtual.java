package memoria;

import core.Proceso;

public class GestorMemoriaVirtual {
    private MarcoRAM[] marcosRAM;
    private DiscoSwap discoSwap;
    private int tamanoPaginaBytes;

    public GestorMemoriaVirtual(int tamanoRAMBytes, int tamanoPaginaBytes) {
        this.tamanoPaginaBytes = tamanoPaginaBytes;
        int cantidadMarcos = tamanoRAMBytes / tamanoPaginaBytes;
        
        this.marcosRAM = new MarcoRAM[cantidadMarcos];
        for (int i = 0; i < cantidadMarcos; i++) {
            marcosRAM[i] = new MarcoRAM(i);
        }
        this.discoSwap = new DiscoSwap();
    }

    public boolean cargarPaginaEnRAM(String idProceso, int numeroPagina) {
        // TODO: Fase 3 - Cargar en marco libre o ejecutar reemplazo (FIFO/LRU/Reloj)
        return true;
    }
} {
    
}

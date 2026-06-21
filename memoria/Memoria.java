package memoria;

/**
 * Representa la Memoria Principal (RAM) física del simulador.
 * Contiene marcos de página y encapsula la capacidad física de la memoria.
 */
public class Memoria {
    private final int tamanoBytes;
    private final int tamanoPaginaBytes;
    private final MarcoRAM[] marcos;

    public Memoria(int tamanoBytes, int tamanoPaginaBytes) {
        if (tamanoBytes <= 0 || tamanoPaginaBytes <= 0) {
            throw new IllegalArgumentException("El tamaño de memoria y página deben ser mayores que cero.");
        }
        if (tamanoBytes % tamanoPaginaBytes != 0) {
            throw new IllegalArgumentException("El tamaño de la memoria debe ser múltiplo del tamaño de página.");
        }
        this.tamanoBytes = tamanoBytes;
        this.tamanoPaginaBytes = tamanoPaginaBytes;
        int cantidadMarcos = tamanoBytes / tamanoPaginaBytes;
        this.marcos = new MarcoRAM[cantidadMarcos];
        for (int i = 0; i < cantidadMarcos; i++) {
            this.marcos[i] = new MarcoRAM(i);
        }
    }

    public int getTamanoBytes() {
        return tamanoBytes;
    }

    public int getTamanoPaginaBytes() {
        return tamanoPaginaBytes;
    }

    public int getCantidadMarcos() {
        return marcos.length;
    }

    public MarcoRAM[] getMarcos() {
        return marcos;
    }

    public MarcoRAM getMarco(int idMarco) {
        if (idMarco < 0 || idMarco >= marcos.length) {
            throw new IndexOutOfBoundsException("ID de marco inválido: " + idMarco);
        }
        return marcos[idMarco];
    }
}

package memoria;

/**
 * Representa un marco físico (Frame) de la memoria RAM del sistema.
 * Actúa como la estructura del Mapa de Bits: un marco con un ID de proceso 
 * asignado equivale a un bit '1' (ocupado), y si es nulo equivale a un bit '0' (libre).
 */
public class MarcoRAM {
    private final int idMarco;
    private String idProcesoAsignado; // null equivale a 0 en el Mapa de Bits
    private int numeroPaginaAsignada;  // -1 si está libre

    public MarcoRAM(int idMarco) {
        this.idMarco = idMarco;
        this.idProcesoAsignado = null;
        this.numeroPaginaAsignada = -1;
    }

    public boolean estaLibre() { return idProcesoAsignado == null; }

    public void asignar(String idProceso, int numeroPagina) {
        this.idProcesoAsignado = idProceso;
        this.numeroPaginaAsignada = numeroPagina;
    }

    public void liberar() {
        this.idProcesoAsignado = null;
        this.numeroPaginaAsignada = -1;
    }

    public int getIdMarco() { return idMarco; }
    public String getIdProcesoAsignado() { return idProcesoAsignado; }
    public int getNumeroPaginaAsignada() { return numeroPaginaAsignada; }
}
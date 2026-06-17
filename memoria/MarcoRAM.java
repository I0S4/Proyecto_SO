package memoria;

public class MarcoRAM {
    private int idMarco;
    private String idProcesoAsignado; // null si está libre
    private int numeroPaginaAsignada;

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

    // Getters
    public int getIdMarco() { return idMarco; }
    public String getIdProcesoAsignado() { return idProcesoAsignado; }
    public int getNumeroPaginaAsignada() { return numeroPaginaAsignada; }
}
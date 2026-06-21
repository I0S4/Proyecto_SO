package memoria;

public class Pagina {
    private final int numeroPagina;
    private final String tipo; // "STACK" o "HEAP"
    private int marcoFisico;   // -1 si está en Swap
    private boolean presente;   // Bit P
    private boolean referencia; // Bit R
    private long timestampAcceso;

    public Pagina(int numeroPagina, String tipo) {
        this.numeroPagina = numeroPagina;
        this.tipo = tipo.toUpperCase();
        this.marcoFisico = -1;
        this.presente = false;
        this.referencia = false;
        this.timestampAcceso = System.nanoTime();
    }

    public void registrarAcceso() {
        this.referencia = true;
        this.timestampAcceso = System.nanoTime();
    }

    // Getters y Setters
    public int getNumeroPagina() { return numeroPagina; }
    public String getTipo() { return tipo; }
    public int getMarcoFisico() { return marcoFisico; }
    public void setMarcoFisico(int marcoFisico) { this.marcoFisico = marcoFisico; }
    public boolean isPresente() { return presente; }
    public void setPresente(boolean presente) { this.presente = presente; }
    public boolean isReferencia() { return referencia; }
    public void setReferencia(boolean referencia) { this.referencia = referencia; }
    public long getTimestampAcceso() { return timestampAcceso; }
}
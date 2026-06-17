package core;

import memoria.TablaPaginas; 

public class Proceso {
    private String id;
    private String estado; // "NUEVO", "LISTO", "EJECUTANDO", "BLOQUEADO", "TERMINADO"
    private int programCounter;
    private int totalInstrucciones;
    private TablaPaginas tablaPaginas; // [NUEVO] Cada proceso tiene su mapeo virtual

    public Proceso(String id, int totalInstrucciones, int tamanoPaginaBytes) {
        this.id = id;
        this.estado = "NUEVO";
        this.programCounter = 0;
        this.totalInstrucciones = totalInstrucciones;
        // Se inicializará la tabla calculando las páginas necesarias según su tamaño
        this.tablaPaginas = new TablaPaginas(id);
    }

    // Getters y Setters base
    public String getId() { return id; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public int getProgramCounter() { return programCounter; }
    public void incrementarPC() { this.programCounter++; }
    public TablaPaginas getTablaPaginas() { return tablaPaginas; }
}
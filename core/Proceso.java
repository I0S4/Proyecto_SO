package core;

import memoria.TablaPaginas; 

public class Proceso {
    private final String id;
    private EstadoProceso estado; // "NUEVO", "LISTO", "EJECUTANDO", "BLOQUEADO", "TERMINADO"
    private Direccion32 programCounter;
    private final int totalInstrucciones;
    private final TablaPaginas tablaPaginas; // Cada proceso tiene su mapeo virtual

    public Proceso(String id, int totalInstrucciones, int tamanoPaginaBytes) {
        this.id = id;
        this.estado = EstadoProceso.NUEVO;
        this.programCounter = Direccion32.CERO;
        this.totalInstrucciones = totalInstrucciones;
        // Se inicializará la tabla calculando las páginas necesarias según su tamaño
        this.tablaPaginas = new TablaPaginas(id);
    }

    // Getters y Setters base
    public String getId() { return id; }
    
    public EstadoProceso getEstado() { return estado; }
    
    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    
    public Direccion32 getProgramCounter() { return programCounter; }
    
    public void setProgramCounter(Direccion32 programCounter) {
        if (programCounter == null) {
            throw new IllegalArgumentException("El Program Counter no puede ser nulo.");
        }
        this.programCounter = programCounter;
    }
    
    public void incrementarPC() { 
        this.programCounter = this.programCounter.incrementar(); 
    }
    
    public int getTotalInstrucciones() { return totalInstrucciones; }
    
    public TablaPaginas getTablaPaginas() { return tablaPaginas; }
}
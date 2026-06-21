package core;

import memoria.TablaPaginas;

/**
 * Representa un proceso del sistema operativo en el simulador.
 * Aplica una máquina de estados estricta para el ciclo de vida de 5 estados:
 * NUEVO → LISTO → EJECUTANDO → {LISTO | BLOQUEADO | TERMINADO}
 * BLOQUEADO → LISTO
 */
public class Proceso {
    private final String id;
    private EstadoProceso estado;
    private Direccion32 programCounter;
    private final int totalInstrucciones;
    private TablaPaginas tablaPaginas; // No-final: el GestorMemoriaVirtual la sincroniza

    public Proceso(String id, int totalInstrucciones, int tamanoPaginaBytes) {
        this.id = id;
        this.estado = EstadoProceso.NUEVO;
        this.programCounter = Direccion32.CERO;
        this.totalInstrucciones = totalInstrucciones;
        this.tablaPaginas = new TablaPaginas(id);
    }

    // --- Getters base ---

    public String getId() { return id; }

    public EstadoProceso getEstado() { return estado; }

    public Direccion32 getProgramCounter() { return programCounter; }

    public int getTotalInstrucciones() { return totalInstrucciones; }

    public TablaPaginas getTablaPaginas() { return tablaPaginas; }

    // --- Setters ---

    /**
     * Cambia el estado del proceso aplicando la máquina de transiciones de 5 estados.
     * Lanza IllegalStateException si la transición no está permitida.
     *
     * Transiciones válidas:
     *   NUEVO      → LISTO
     *   LISTO      → EJECUTANDO
     *   EJECUTANDO → LISTO  (quantum expirado / preempción)
     *   EJECUTANDO → BLOQUEADO (solicitud de E/S)
     *   EJECUTANDO → TERMINADO (proceso finalizado)
     *   BLOQUEADO  → LISTO  (E/S completada)
     */
    public void setEstado(EstadoProceso nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo.");
        }
        if (!esTransicionValida(this.estado, nuevoEstado)) {
            throw new IllegalStateException(
                String.format("Transición de estado inválida para el proceso '%s': %s → %s.",
                    id, this.estado, nuevoEstado)
            );
        }
        this.estado = nuevoEstado;
    }

    /**
     * Permite al GestorMemoriaVirtual sincronizar la tabla de páginas real del proceso
     * después de haberla calculado e inicializado con stack/heap.
     */
    public void setTablaPaginas(TablaPaginas tablaPaginas) {
        if (tablaPaginas == null) {
            throw new IllegalArgumentException("La Tabla de Páginas no puede ser nula.");
        }
        this.tablaPaginas = tablaPaginas;
    }

    public void setProgramCounter(Direccion32 programCounter) {
        if (programCounter == null) {
            throw new IllegalArgumentException("El Program Counter no puede ser nulo.");
        }
        this.programCounter = programCounter;
    }

    public void incrementarPC() {
        this.programCounter = this.programCounter.incrementar();
    }

    // --- Lógica interna de la máquina de estados ---

    private boolean esTransicionValida(EstadoProceso actual, EstadoProceso nuevo) {
        if (actual == nuevo) return true; // Transición reflexiva permitida
        switch (actual) {
            case NUEVO:      return nuevo == EstadoProceso.LISTO;
            case LISTO:      return nuevo == EstadoProceso.EJECUTANDO;
            case EJECUTANDO: return nuevo == EstadoProceso.LISTO
                                 || nuevo == EstadoProceso.BLOQUEADO
                                 || nuevo == EstadoProceso.TERMINADO;
            case BLOQUEADO:  return nuevo == EstadoProceso.LISTO;
            case TERMINADO:  return false;
            default:         return false;
        }
    }
}
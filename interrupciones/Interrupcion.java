package interrupciones;

/**
 * Representa una señal o interrupción en el sistema.
 */
public record Interrupcion(TipoInterrupcion tipo, String descripcion, Object payload) {
    
    public enum TipoInterrupcion {
        TICK,
        PAGE_FAULT,
        ES_COMPLETA,
        ERROR_SISTEMA
    }
}

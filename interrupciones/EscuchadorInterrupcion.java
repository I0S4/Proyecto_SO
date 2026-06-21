package interrupciones;

/**
 * Interfaz Observer para recibir señales/interrupciones del sistema.
 */
public interface EscuchadorInterrupcion {
    void procesarInterrupcion(Interrupcion interrupcion);
}

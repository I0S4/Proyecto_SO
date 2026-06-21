package core;

/**
 * Enum que representa los estados posibles en los que puede encontrarse un proceso
 * dentro del simulador de sistema operativo.
 */
public enum EstadoProceso {
    NUEVO,
    LISTO,
    EJECUTANDO,
    BLOQUEADO,
    TERMINADO
}

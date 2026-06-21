package dto;

import java.util.List;

/**
 * DTO que representa el payload JSON unificado que se envía por WebSockets.
 * Modela el estado completo del simulador del sistema operativo en un instante dado (Tick).
 */
public record EstadoSistemaDTO(
    int tickActual,
    ConfiguracionDTO configuracion,
    EstadoCPUDTO estadoCPU,
    ColasProcesosDTO colasProcesos,
    GestionMemoriaDTO gestionMemoria,
    List<DispositivoESDTO> dispositivosES,
    SistemaDTO sistema
) {
    public record ConfiguracionDTO(
        String algoritmoPlanificacion,
        int quantum,
        int tamanoPaginaBytes
    ) {}

    public record EstadoCPUDTO(
        String ejecutandoProcesoId, // null si no hay proceso
        long programCounter,
        String limite32Bits // Representación hexadecimal (ej: "0x0000000A")
    ) {}

    public record ColasProcesosDTO(
        List<String> nuevos,
        List<String> listos,
        List<String> bloqueados
    ) {}

    public record GestionMemoriaDTO(
        String algoritmoReemplazo,
        int totalAccesos,
        int pageFaultsTotales,
        double porcentajeThrashing,
        List<MarcoRAMDTO> marcosRAM,
        AreaSwapDTO areaSwap
    ) {}

    public record MarcoRAMDTO(
        int idMarco,
        String idProcesoAsignado, // null si está libre
        int numeroPaginaAsignada
    ) {}

    public record AreaSwapDTO(
        int totalPaginasEnDisco,
        List<PaginaSwapDTO> paginas
    ) {}

    public record PaginaSwapDTO(
        String idProceso,
        int numeroPagina
    ) {}

    public record DispositivoESDTO(
        String nombre,
        String procesoActualId, // null si está libre
        int tiempoRestanteTick,
        List<String> colaEspera
    ) {}

    public record SistemaDTO(
        boolean toleranciaFallosExcedida,
        List<String> logs
    ) {}
}

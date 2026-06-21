package memoria;

import dto.EstadoSistemaDTO.PaginaSwapDTO;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa el área de Swap en disco para almacenar páginas desalojadas de RAM.
 * Mantiene el registro de qué páginas de qué procesos han sido enviadas al área 
 * de intercambio por falta de espacio físico en la memoria principal.
 */
public class DiscoSwap {
    private final List<PaginaSwapDTO> paginasEnSwap;

    public DiscoSwap() {
        this.paginasEnSwap = new ArrayList<>();
    }

    public void enviarASwap(String idProceso, int numeroPagina) {
        // Vinculación directa con el DTO unificado del sistema
        paginasEnSwap.add(new PaginaSwapDTO(idProceso, numeroPagina));
    }

    public void retirarDeSwap(String idProceso, int numeroPagina) {
        paginasEnSwap.removeIf(p -> p.idProceso().equals(idProceso) && p.numeroPagina() == numeroPagina);
    }

    public List<PaginaSwapDTO> getPaginas() { return paginasEnSwap; }
}
import core.*;
import memoria.*;
import strategy.*;
import interrupciones.*;
import dto.*;

import java.util.ArrayList;
import java.util.List;

public class VerificadorFundaciones {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO VERIFICACIÓN DE LA FASE 1: FUNDACIONES ===");
        
        try {
            testDireccion32();
            testEstadoProcesoYProceso();
            testDispositivoESYObserver();
            testMemoriaYSingleton();
            testMemoriaVirtualFacade();
            testDTOContrato();
            
            System.out.println("\n[ÉXITO] ¡Todas las verificaciones pasaron exitosamente!");
        } catch (Exception e) {
            System.err.println("\n[ERROR] Hubo un error en las verificaciones:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void testDireccion32() {
        System.out.print("Probando Direccion32... ");
        
        // 1. Limites válidos
        Direccion32 dirMin = new Direccion32(0);
        Direccion32 dirMax = new Direccion32(0xFFFFFFFFL);
        Direccion32 dirNormal = new Direccion32(0x0000000AL);
        
        if (dirMin.getValor() != 0 || dirMax.getValor() != 4294967295L) {
            throw new RuntimeException("Valores de límites incorrectos.");
        }
        
        // 2. Formato toString hexadecimal
        if (!dirNormal.toString().equals("0x0000000A")) {
            throw new RuntimeException("Formato toString incorrecto: " + dirNormal);
        }
        
        // 3. Incremento y operaciones
        Direccion32 dirInc = dirNormal.incrementar();
        if (dirInc.getValor() != 11) {
            throw new RuntimeException("Error en incremento.");
        }
        
        // 4. Límite estricto 32-bit (debe lanzar excepción)
        try {
            new Direccion32(0x100000000L);
            throw new RuntimeException("Error: Se permitió crear dirección mayor a 32 bits.");
        } catch (IllegalArgumentException e) {
            // Excepción esperada
        }

        try {
            new Direccion32(-1);
            throw new RuntimeException("Error: Se permitió crear dirección negativa.");
        } catch (IllegalArgumentException e) {
            // Excepción esperada
        }
        
        System.out.println("OK");
    }

    private static void testEstadoProcesoYProceso() {
        System.out.print("Probando Proceso y Estados... ");
        
        Proceso p = new Proceso("P-01", 10, 4096);
        if (p.getEstado() != EstadoProceso.NUEVO) {
            throw new RuntimeException("El estado inicial de un proceso debe ser NUEVO.");
        }
        
        // Transición válida: NUEVO → LISTO → EJECUTANDO
        p.setEstado(EstadoProceso.LISTO);
        p.setEstado(EstadoProceso.EJECUTANDO);
        if (p.getEstado() != EstadoProceso.EJECUTANDO) {
            throw new RuntimeException("No se actualizó el estado del proceso.");
        }
        
        if (p.getProgramCounter().getValor() != 0) {
            throw new RuntimeException("El PC inicial debe ser 0.");
        }
        
        p.incrementarPC();
        if (p.getProgramCounter().getValor() != 1) {
            throw new RuntimeException("El PC no se incrementó correctamente.");
        }
        
        System.out.println("OK");
    }

    private static void testDispositivoESYObserver() {
        System.out.print("Probando DispositivoES y Observer... ");
        
        GestorInterrupciones gestor = new GestorInterrupciones();
        DispositivoES teclado = new DispositivoES("Teclado", gestor);
        
        List<Interrupcion> interrupcionesRecibidas = new ArrayList<>();
        gestor.suscribir(Interrupcion.TipoInterrupcion.ES_COMPLETA, new EscuchadorInterrupcion() {
            @Override
            public void procesarInterrupcion(Interrupcion interrupcion) {
                interrupcionesRecibidas.add(interrupcion);
            }
        });
        
        Proceso p = new Proceso("P-02", 5, 4096);
        teclado.solicitarES(p);
        
        // Simular ticks de E/S
        for (int i = 0; i < 5; i++) {
            teclado.actualizarTick();
        }
        
        if (interrupcionesRecibidas.isEmpty()) {
            throw new RuntimeException("No se recibió la interrupción de E/S completada.");
        }
        
        Interrupcion intr = interrupcionesRecibidas.get(0);
        if (intr.tipo() != Interrupcion.TipoInterrupcion.ES_COMPLETA || !intr.descripcion().contains("Teclado")) {
            throw new RuntimeException("Los datos de la interrupción son incorrectos: " + intr);
        }
        
        System.out.println("OK");
    }

    private static void testMemoriaYSingleton() {
        System.out.print("Probando Memoria y Singleton de GestorMemoriaPrincipal... ");
        
        GestorMemoriaPrincipal gmp = GestorMemoriaPrincipal.getInstancia();
        gmp.inicializar(8192, 2048); // 8KB RAM, páginas de 2KB -> 4 marcos
        
        Memoria mem = gmp.getMemoria();
        if (mem.getCantidadMarcos() != 4) {
            throw new RuntimeException("La cantidad de marcos calculada es incorrecta: " + mem.getCantidadMarcos());
        }
        
        // Verificar que sea el mismo objeto Singleton
        GestorMemoriaPrincipal gmp2 = GestorMemoriaPrincipal.getInstancia();
        if (gmp != gmp2 || gmp.getMemoria() != gmp2.getMemoria()) {
            throw new RuntimeException("El GestorMemoriaPrincipal no es un Singleton.");
        }
        
        System.out.println("OK");
    }

    private static void testMemoriaVirtualFacade() {
        System.out.print("Probando Facade de GestorMemoriaVirtual... ");
        
        GestorMemoriaVirtual gmv = new GestorMemoriaVirtual(16384, 4096);
        if (gmv.getTamanoPaginaBytes() != 4096) {
            throw new RuntimeException("Tamaño de página incorrecto en GestorMemoriaVirtual.");
        }
        
        // Cargar una página lógica
        boolean exito = gmv.cargarPaginaEnRAM("P-01", 0);
        if (!exito) {
            throw new RuntimeException("Fallo cargar página en RAM.");
        }
        
        // Verificar estrategia de reemplazo (strategy.AlgoritmoReemplazo es una interfaz)
        strategy.AlgoritmoReemplazo lru = marcos -> 0;
        gmv.setAlgoritmoReemplazo(lru);
        if (gmv.getAlgoritmoReemplazo() != lru) {
            throw new RuntimeException("Error asignando estrategia de reemplazo.");
        }
        
        System.out.println("OK");
    }

    private static void testDTOContrato() {
        System.out.print("Probando DTO del Contrato... ");
        
        EstadoSistemaDTO.ConfiguracionDTO config = new EstadoSistemaDTO.ConfiguracionDTO("RR", 4, 4096);
        EstadoSistemaDTO.EstadoCPUDTO cpu = new EstadoSistemaDTO.EstadoCPUDTO("P-01", 10, "0x0000000A");
        EstadoSistemaDTO.ColasProcesosDTO colas = new EstadoSistemaDTO.ColasProcesosDTO(
            List.of("P-04"), List.of("P-02", "P-03"), List.of()
        );
        
        EstadoSistemaDTO.MarcoRAMDTO marco0 = new EstadoSistemaDTO.MarcoRAMDTO(0, "P-01", 0);
        EstadoSistemaDTO.MarcoRAMDTO marco1 = new EstadoSistemaDTO.MarcoRAMDTO(1, "P-01", 1);
        EstadoSistemaDTO.MarcoRAMDTO marco2 = new EstadoSistemaDTO.MarcoRAMDTO(2, "P-02", 0);
        EstadoSistemaDTO.MarcoRAMDTO marco3 = new EstadoSistemaDTO.MarcoRAMDTO(3, null, -1);
        
        EstadoSistemaDTO.PaginaSwapDTO swap0 = new EstadoSistemaDTO.PaginaSwapDTO("P-02", 1);
        EstadoSistemaDTO.PaginaSwapDTO swap1 = new EstadoSistemaDTO.PaginaSwapDTO("P-03", 0);
        
        EstadoSistemaDTO.AreaSwapDTO swap = new EstadoSistemaDTO.AreaSwapDTO(2, List.of(swap0, swap1));
        
        EstadoSistemaDTO.GestionMemoriaDTO memoria = new EstadoSistemaDTO.GestionMemoriaDTO(
            "FIFO", 150, 12, 8.0, List.of(marco0, marco1, marco2, marco3), swap
        );
        
        EstadoSistemaDTO.DispositivoESDTO dev1 = new EstadoSistemaDTO.DispositivoESDTO("Teclado", null, 0, List.of());
        EstadoSistemaDTO.DispositivoESDTO dev2 = new EstadoSistemaDTO.DispositivoESDTO("Disco", null, 0, List.of());
        EstadoSistemaDTO.DispositivoESDTO dev3 = new EstadoSistemaDTO.DispositivoESDTO("Impresora", null, 0, List.of());
        
        EstadoSistemaDTO.SistemaDTO sistema = new EstadoSistemaDTO.SistemaDTO(
            false, List.of(
                "[Tick 18] Instrucción ejecutada exitosamente para P-01.",
                "[Tick 17] Page Fault resuelto: Página 0 de P-02 movida a RAM. Página 1 de P-02 enviada a Swap."
            )
        );
        
        EstadoSistemaDTO payload = new EstadoSistemaDTO(
            18, config, cpu, colas, memoria, List.of(dev1, dev2, dev3), sistema
        );
        
        if (payload.tickActual() != 18 || payload.configuracion().quantum() != 4 || payload.estadoCPU().programCounter() != 10) {
            throw new RuntimeException("Error en valores DTO del payload unificado.");
        }
        
        System.out.println("OK");
    }
}

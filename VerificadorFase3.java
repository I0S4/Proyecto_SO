import core.*;
import planificacion.PlanificadorCPU;
import memoria.GestorMemoriaVirtual;
import interrupciones.GestorInterrupciones;
import interrupciones.Interrupcion;
import dto.EstadoSistemaDTO;
import dto.EstadoSistemaDTO.*;
import strategy.*;

import java.util.List;

public class VerificadorFase3 {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO VERIFICACIÓN DE LA FASE 3: PLANIFICACIÓN Y MEMORIA VIRTUAL ===");

        try {
            testFCFS();
            testSJF();
            testSRTF();
            testRoundRobin();
            testPageFaultStallings();

            System.out.println("\n[ÉXITO] ¡Todas las verificaciones de la Fase 3 pasaron exitosamente!");
        } catch (Exception e) {
            System.err.println("\n[ERROR] Hubo un error en las verificaciones de la Fase 3:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void testFCFS() {
        System.out.print("Probando planificador FCFS... ");
        SimuladorSO sim = new SimuladorSO();
        sim.configurar("FCFS", 4, 4096, 16384);

        // Admitir P1 (3 instrucciones) y P2 (2 instrucciones)
        sim.admitirProceso("P1", 3, 4096, 0);
        sim.admitirProceso("P2", 2, 4096, 0);

        // FCFS: P1 debe ejecutarse hasta terminar (3 ticks), luego P2 (2 ticks)
        // Tick 1
        EstadoSistemaDTO estado = sim.ejecutarTick();
        if (!"P1".equals(estado.estadoCPU().ejecutandoProcesoId())) {
            throw new RuntimeException("FCFS error: P1 debería estar en CPU en tick 1. Encontrado: " + estado.estadoCPU().ejecutandoProcesoId());
        }

        // Tick 2
        estado = sim.ejecutarTick();
        if (!"P1".equals(estado.estadoCPU().ejecutandoProcesoId())) {
            throw new RuntimeException("FCFS error: P1 debería estar en CPU en tick 2.");
        }

        // Tick 3: P1 ejecuta su última instrucción y se marca como TERMINADO al final
        estado = sim.ejecutarTick();
        
        // Tick 4: P2 debe ser despachado
        estado = sim.ejecutarTick();
        if (!"P2".equals(estado.estadoCPU().ejecutandoProcesoId())) {
            throw new RuntimeException("FCFS error: P2 debería estar en CPU en tick 4.");
        }

        // Tick 5: P2 ejecuta su última instrucción y se marca como TERMINADO
        estado = sim.ejecutarTick();

        // Tick 6: CPU ociosa
        estado = sim.ejecutarTick();
        if (estado.estadoCPU().ejecutandoProcesoId() != null) {
            throw new RuntimeException("FCFS error: CPU debería estar ociosa en tick 6.");
        }

        System.out.println("OK");
    }

    private static void testSJF() {
        System.out.print("Probando planificador SJF (No Apropiativo)... ");
        SimuladorSO sim = new SimuladorSO();
        sim.configurar("SJF", 4, 4096, 16384);

        // Ambos son admitidos en el tick 0.
        // P1 tiene 5 instrucciones, P2 tiene 2 instrucciones.
        sim.admitirProceso("P1", 5, 4096, 0);
        sim.admitirProceso("P2", 2, 4096, 0);

        // SJF es no expropiativo, pero en el primer despacho (tick 1),
        // debe elegir el más corto: P2.
        EstadoSistemaDTO estado = sim.ejecutarTick();
        if (!"P2".equals(estado.estadoCPU().ejecutandoProcesoId())) {
            throw new RuntimeException("SJF error: P2 (ráfaga 2) debería ejecutarse antes que P1 (ráfaga 5). En CPU: " + estado.estadoCPU().ejecutandoProcesoId());
        }

        // Tick 2: P2 continúa y termina
        estado = sim.ejecutarTick();

        // Tick 3: P1 debe ser despachado
        estado = sim.ejecutarTick();
        if (!"P1".equals(estado.estadoCPU().ejecutandoProcesoId())) {
            throw new RuntimeException("SJF error: P1 debería estar en CPU en tick 3.");
        }

        System.out.println("OK");
    }

    private static void testSRTF() {
        System.out.print("Probando planificador SRTF (SJF Apropiativo)... ");
        SimuladorSO sim = new SimuladorSO();
        sim.configurar("SRTF", 4, 4096, 16384);

        // Admitimos P1 con 6 instrucciones
        sim.admitirProceso("P1", 6, 4096, 0);

        // Tick 1: P1 en CPU (ejecuta PC=0, le quedan 5)
        EstadoSistemaDTO estado = sim.ejecutarTick();
        if (!"P1".equals(estado.estadoCPU().ejecutandoProcesoId())) {
            throw new RuntimeException("SRTF error: P1 en CPU en tick 1.");
        }

        // Tick 2: P1 en CPU (ejecuta PC=1, le quedan 4)
        estado = sim.ejecutarTick();

        // Admitimos P2 con 2 instrucciones (menor que las 4 restantes de P1)
        sim.admitirProceso("P2", 2, 4096, 0);

        // Tick 3: Al inicio de este tick, P2 está en listos.
        // Como SRTF es expropiativo, debe quitar a P1 y despachar a P2
        estado = sim.ejecutarTick();
        if (!"P2".equals(estado.estadoCPU().ejecutandoProcesoId())) {
            throw new RuntimeException("SRTF error: P2 debería haber expropiado a P1. En CPU: " + estado.estadoCPU().ejecutandoProcesoId());
        }

        // P2 continúa en tick 4 y termina
        estado = sim.ejecutarTick();

        // Tick 5: P1 debe volver a la CPU
        estado = sim.ejecutarTick();
        if (!"P1".equals(estado.estadoCPU().ejecutandoProcesoId())) {
            throw new RuntimeException("SRTF error: P1 debería volver a la CPU.");
        }

        System.out.println("OK");
    }

    private static void testRoundRobin() {
        System.out.print("Probando planificador Round Robin... ");
        SimuladorSO sim = new SimuladorSO();
        sim.configurar("RR", 2, 4096, 16384); // Quantum = 2

        // Admitir P1 (4 instrucciones) y P2 (3 instrucciones)
        sim.admitirProceso("P1", 4, 4096, 0);
        sim.admitirProceso("P2", 3, 4096, 0);

        // Ticks 1 y 2: P1 ejecuta (2 ticks, quantum expira)
        sim.ejecutarTick();
        EstadoSistemaDTO estado = sim.ejecutarTick();
        if (!"P1".equals(estado.estadoCPU().ejecutandoProcesoId())) {
            throw new RuntimeException("RR error: P1 debería estar en CPU en tick 2.");
        }

        // Tick 3: P2 es despachado por expiración del quantum de P1
        estado = sim.ejecutarTick();
        if (!"P2".equals(estado.estadoCPU().ejecutandoProcesoId())) {
            throw new RuntimeException("RR error: P2 debería estar en CPU en tick 3. En CPU: " + estado.estadoCPU().ejecutandoProcesoId());
        }

        // Tick 4: P2 continúa (su quantum = 2 expira)
        estado = sim.ejecutarTick();

        // Tick 5: P1 es despachado nuevamente
        estado = sim.ejecutarTick();
        if (!"P1".equals(estado.estadoCPU().ejecutandoProcesoId())) {
            throw new RuntimeException("RR error: P1 debería estar en CPU en tick 5.");
        }

        System.out.println("OK");
    }

    private static void testPageFaultStallings() {
        System.out.print("Probando Page Fault con Modelo de Stallings... ");
        SimuladorSO sim = new SimuladorSO();
        
        // Configuramos página de 2 bytes y RAM de 2 bytes (1 solo marco disponible)
        sim.configurar("FCFS", 4, 2, 2);

        // Proceso P1 con 5 instrucciones. stack: 2 bytes (pág 0), heap: 2 bytes (pág 1)
        sim.admitirProceso("P1", 5, 2, 2);

        // Tick 1: PC=0. Dirección 0 / 2 = pág 0 (en RAM). P1 se ejecuta correctamente. PC -> 1
        EstadoSistemaDTO estado = sim.ejecutarTick();
        if (!"P1".equals(estado.estadoCPU().ejecutandoProcesoId()) || estado.estadoCPU().programCounter() != 1) {
            throw new RuntimeException("PageFault error: P1 debió ejecutar PC=0. Encontrado PC=" + estado.estadoCPU().programCounter());
        }

        // Tick 2: PC=1. Dirección 1 / 2 = pág 0 (en RAM). P1 se ejecuta correctamente. PC -> 2
        estado = sim.ejecutarTick();
        if (!"P1".equals(estado.estadoCPU().ejecutandoProcesoId()) || estado.estadoCPU().programCounter() != 2) {
            throw new RuntimeException("PageFault error: P1 debió ejecutar PC=1. Encontrado PC=" + estado.estadoCPU().programCounter());
        }

        // Tick 3: PC=2. Dirección 2 / 2 = pág 1 (NO en RAM, está en swap).
        // Debe lanzar un Page Fault.
        // Según el modelo de Stallings:
        // - El proceso se interrumpe y se bloquea (se detiene su ejecución en este tick).
        // - El PC NO se incrementa (queda en 2).
        // - La CPU queda libre (OCIOSA / null) para el resto del tick.
        estado = sim.ejecutarTick();
        
        if (estado.estadoCPU().ejecutandoProcesoId() != null) {
            throw new RuntimeException("PageFault error: CPU debería haber quedado ociosa tras el Page Fault. Encontrado: " + estado.estadoCPU().ejecutandoProcesoId());
        }
        
        if (estado.estadoCPU().programCounter() != 0) {
            // Cuando la CPU está libre/ociosa, generarCPUDTO() devuelve PC = 0.
            // Pero verifiquemos que P1 está en la lista de BLOQUEADOS
            if (!estado.colasProcesos().bloqueados().contains("P1")) {
                throw new RuntimeException("PageFault error: P1 debería estar en la cola de BLOQUEADOS.");
            }
        }

        // Al inicio del Tick 4, la simulación de swapping de 1 tick se completa.
        // P1 regresa a la cola de LISTOS. Se vuelve a despachar.
        // PC=2. Dirección 2 / 2 = pág 1 (ya en RAM tras el swapping). Se ejecuta con éxito. PC -> 3.
        estado = sim.ejecutarTick();
        if (!"P1".equals(estado.estadoCPU().ejecutandoProcesoId()) || estado.estadoCPU().programCounter() != 3) {
            throw new RuntimeException("PageFault error: P1 debió reanudarse y ejecutar PC=2. Encontrado: " + estado.estadoCPU().ejecutandoProcesoId() + ", PC=" + estado.estadoCPU().programCounter());
        }

        System.out.println("OK");
    }
}

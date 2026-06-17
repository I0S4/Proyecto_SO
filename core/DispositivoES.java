package core;

import java.util.LinkedList;
import java.util.Queue;

public class DispositivoES {
    private String nombre; // "Teclado", "Disco", "Impresora"
    private Queue<Proceso> colaEspera;
    private Proceso procesoActivo;
    private int tiempoRestanteTick;

    public DispositivoES(String nombre) {
        this.nombre = nombre;
        this.colaEspera = new LinkedList<>();
        this.procesoActivo = null;
        this.tiempoRestanteTick = 0;
    }

    public void actualizarTick() {
        // TODO: Fase 4 - Decrementar tiempo y disparar interrupción asíncrona al terminar
    }

    public void solicitarES(Proceso proceso) {
        // TODO: Fase 4 - Introducir duraciones aleatorias entre 5 y 20 unidades
    }
}
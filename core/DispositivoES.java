package core;

import java.util.LinkedList;
import java.util.Queue;
import interrupciones.GestorInterrupciones;
import interrupciones.Interrupcion;

public class DispositivoES {
    private final String nombre; // "Teclado", "Disco", "Impresora"
    private final Queue<Proceso> colaEspera;
    private Proceso procesoActivo;
    private int tiempoRestanteTick;
    private GestorInterrupciones gestorInterrupciones;

    public DispositivoES(String nombre) {
        this.nombre = nombre;
        this.colaEspera = new LinkedList<>();
        this.procesoActivo = null;
        this.tiempoRestanteTick = 0;
    }

    public DispositivoES(String nombre, GestorInterrupciones gestorInterrupciones) {
        this.nombre = nombre;
        this.colaEspera = new LinkedList<>();
        this.procesoActivo = null;
        this.tiempoRestanteTick = 0;
        this.gestorInterrupciones = gestorInterrupciones;
    }

    public void setGestorInterrupciones(GestorInterrupciones gestorInterrupciones) {
        this.gestorInterrupciones = gestorInterrupciones;
    }

    public String getNombre() {
        return nombre;
    }

    public Queue<Proceso> getColaEspera() {
        return colaEspera;
    }

    public Proceso getProcesoActivo() {
        return procesoActivo;
    }

    public void setProcesoActivo(Proceso procesoActivo) {
        this.procesoActivo = procesoActivo;
    }

    public int getTiempoRestanteTick() {
        return tiempoRestanteTick;
    }

    public void setTiempoRestanteTick(int tiempoRestanteTick) {
        this.tiempoRestanteTick = tiempoRestanteTick;
    }

    public void actualizarTick() {
        if (procesoActivo != null) {
            tiempoRestanteTick--;
            if (tiempoRestanteTick <= 0) {
                if (gestorInterrupciones != null) {
                    gestorInterrupciones.notificar(new Interrupcion(
                        Interrupcion.TipoInterrupcion.ES_COMPLETA,
                        "E/S completada en dispositivo: " + nombre,
                        procesoActivo
                    ));
                }
                procesoActivo = null;
            }
        }
    }

    public void solicitarES(Proceso proceso) {
        if (proceso == null) return;
        
        if (procesoActivo == null) {
            procesoActivo = proceso;
            this.tiempoRestanteTick = 5; // Duración base, extensible en Fase 4
        } else {
            colaEspera.add(proceso);
        }
    }
}
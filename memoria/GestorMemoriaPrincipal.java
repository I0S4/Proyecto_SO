package memoria;

/**
 * Gestor de Memoria Principal aplicando el patrón Singleton.
 * Proporciona un punto único de acceso a la memoria física del simulador.
 */
public class GestorMemoriaPrincipal {
    private static volatile GestorMemoriaPrincipal instancia;
    private Memoria memoria;

    private GestorMemoriaPrincipal() {
        // Constructor privado para evitar instanciación externa
    }

    public static GestorMemoriaPrincipal getInstancia() {
        if (instancia == null) {
            synchronized (GestorMemoriaPrincipal.class) {
                if (instancia == null) {
                    instancia = new GestorMemoriaPrincipal();
                }
            }
        }
        return instancia;
    }

    /**
     * Inicializa la memoria física con parámetros específicos de capacidad.
     */
    public synchronized void inicializar(int tamanoBytes, int tamanoPaginaBytes) {
        this.memoria = new Memoria(tamanoBytes, tamanoPaginaBytes);
    }

    public synchronized Memoria getMemoria() {
        if (memoria == null) {
            // Inicialización predeterminada (ej: 16KB de RAM con páginas de 4KB) si no se ha llamado a inicializar
            inicializar(16384, 4096);
        }
        return memoria;
    }
    
    public synchronized boolean estaInicializada() {
        return memoria != null;
    }
}

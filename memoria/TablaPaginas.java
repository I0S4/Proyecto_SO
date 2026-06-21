package memoria;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa la Tabla de Páginas de un proceso individual.
 * Se encarga de mantener el mapeo y la traducción entre el espacio de 
 * direccionamiento lógico del proceso y los marcos de la memoria física.
 */
public class TablaPaginas {
    private final String idProceso;
    private final List<Pagina> paginas;

    public TablaPaginas(String idProceso) {
        this.idProceso = idProceso;
        this.paginas = new ArrayList<>();
    }

    public void agregarPagina(String tipo) {
        paginas.add(new Pagina(paginas.size(), tipo));
    }

    public List<Pagina> getPaginas() { return paginas; }
    public String getIdProceso() { return idProceso; }
}
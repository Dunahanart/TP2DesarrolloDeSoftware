import java.util.ArrayList;
import java.util.List;

// Lugar físico donde se procesan envíos: un envío comienza su recorrido en una
// sucursal y puede pasar por otras antes de llegar a su destino.
//
// Responsabilidad de la sucursal: decidir qué envíos procesa (recibe y
// despacha), y llevar el control de los que tiene en curso. No es responsable
// de cómo cambia el estado de los paquetes: para eso le manda un mensaje al
// envío, que es quien coordina a sus paquetes (Colaboración:
// App -> Sucursal -> Envio -> Paquete).
public class Sucursal {

    private final String id;
    private final String nombre;
    private final List<Envio> enviosEnProceso;

    public Sucursal(String id, String nombre) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("La sucursal necesita un id.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("La sucursal necesita un nombre.");
        }
        this.id = id;
        this.nombre = nombre;
        this.enviosEnProceso = new ArrayList<>();
    }

    // El envío llega a esta sucursal.
    // Primero se delega al envío (que valida su etapa, actualiza sus paquetes y
    // registra el movimiento) y recién después se lo suma a la lista propia:
    // si el envío rechaza la operación, la sucursal no queda con un envío fantasma.
    public void recibir(Envio envio) {
        validarEnvio(envio);
        if (enviosEnProceso.contains(envio)) {
            throw new IllegalStateException(
                "El envío #" + envio.getId() + " ya se encuentra en la sucursal " + nombre + ".");
        }
        envio.recibirEn(this);
        enviosEnProceso.add(envio);
    }

    // El envío sale de esta sucursal hacia su próxima parada.
    // Se le pide permiso al envío antes de quitarlo de la lista, por el mismo
    // motivo anterior: la lista propia solo refleja operaciones que realmente
    // sucedieron.
    public void despachar(Envio envio) {
        validarEnvio(envio);
        if (!enviosEnProceso.contains(envio)) {
            throw new IllegalStateException(
                "El envío #" + envio.getId() + " no se está procesando en la sucursal " + nombre + ".");
        }
        envio.despacharDesde(this);
        enviosEnProceso.remove(envio);
    }

    private void validarEnvio(Envio envio) {
        if (envio == null) {
            throw new IllegalArgumentException("El envío no puede ser nulo.");
        }
    }

    // Copia inmutable: el historial del recorrido vive en cada Envio, no acá.
    public List<Envio> getEnviosEnProceso() {
        return List.copyOf(enviosEnProceso);
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }

    @Override
    public String toString() {
        return nombre + " (" + id + ")";
    }
}

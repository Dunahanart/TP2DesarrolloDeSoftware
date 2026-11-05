import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

// Lugar físico donde se procesan envíos: un envío comienza su recorrido en una
// sucursal y puede pasar por otras antes de llegar a su destino.
//
// TP4, Parte 3: la sucursal tiene una COLA DE DESPACHO (Queue<Envio>, FIFO):
// los envíos que llegan esperan su turno en orden de llegada — el primero que
// entra es el primero que sale — y el camión se lleva hasta "cantidad" envíos
// por viaje.
//
// Responsabilidad de la sucursal: decidir qué envíos procesa (recibe y
// despacha), y llevar el control de los que tiene en curso. No es responsable
// de cómo cambia el estado de los paquetes: para eso le manda un mensaje al
// envío, que es quien coordina a sus paquetes (Colaboración:
// App -> Sucursal -> Envio -> Paquete).
public class Sucursal {

    private final String id;
    private final String nombre;

    // La cola reemplaza a la lista del TP2: misma información (los envíos que
    // esta sucursal tiene en curso), pero ahora con orden de llegada. Un
    // ArrayDeque es una implementación de Queue pensada para esto.
    private final Queue<Envio> colaDeDespacho;

    public Sucursal(String id, String nombre) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("La sucursal necesita un id.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("La sucursal necesita un nombre.");
        }
        this.id = id;
        this.nombre = nombre;
        this.colaDeDespacho = new ArrayDeque<>();
    }

    // El envío llega a esta sucursal y queda al FINAL de la cola, esperando su
    // turno de despacho. Primero se delega al envío (que valida su etapa,
    // actualiza sus paquetes y registra el movimiento) y recién después entra a
    // la cola: si el envío rechaza la operación, la sucursal no queda con un
    // envío fantasma.
    public void recibirEnvio(Envio envio) {
        validarEnvio(envio);
        if (colaDeDespacho.contains(envio)) {
            throw new IllegalStateException(
                "El envío #" + envio.getId() + " ya está esperando despacho en " + nombre + ".");
        }
        envio.recibirEn(this);
        colaDeDespacho.offer(envio);
    }

    // Salen como máximo "cantidad" envíos, respetando el orden de llegada
    // (FIFO: sale el más viejo de la cola). Si hay menos envíos que la
    // capacidad, salen los que haya. Si no sale ninguno, se informa.
    // Devuelve cuántos salieron para que el llamador pueda informarlo.
    public int despachar(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                "La cantidad a despachar debe ser mayor a cero (recibido: " + cantidad + ").");
        }
        int despachados = 0;
        while (despachados < cantidad && !colaDeDespacho.isEmpty()) {
            // poll() saca el primero de la cola; ese envío es el que sale.
            Envio envio = colaDeDespacho.poll();
            envio.despacharDesde(this);
            despachados++;
        }
        if (despachados == 0) {
            throw new IllegalStateException(
                "La cola de la sucursal " + nombre + " está vacía: no hay envíos para despachar.");
        }
        return despachados;
    }

    private void validarEnvio(Envio envio) {
        if (envio == null) {
            throw new IllegalArgumentException("El envío no puede ser nulo.");
        }
    }

    // Copia inmutable, en orden de llegada: el primero de la lista es el
    // próximo en despacharse. El historial del recorrido vive en cada Envio.
    public List<Envio> getEnviosEnProceso() {
        return List.copyOf(colaDeDespacho);
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }

    @Override
    public String toString() {
        return nombre + " (" + id + ")";
    }
}

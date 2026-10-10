import java.util.ArrayList;
import java.util.List;

// Quién solicita los envíos. Un cliente puede realizar varios envíos a lo largo
// del tiempo, y el sistema debe poder responder "qué envíos tiene este cliente".
//
// Responsabilidad: el cliente CONOCE y ADMINISTRA su propia historia de envíos.
// La lista es privada y solo se completa por dentro (ver registrarEnvio), nunca
// porque un objeto externo haga cliente.getEnvios().add(...).
public class Cliente {

    private final String id;
    private final String nombre;
    private final List<Envio> envios;

    public Cliente(String id, String nombre) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El cliente necesita un id.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El cliente necesita un nombre.");
        }
        this.id = id;
        this.nombre = nombre;
        this.envios = new ArrayList<>();
    }

    // ¿Quién se entera de que el cliente realizó un envío? El propio Envio, al
    // crearse (ver su constructor). Este método es package-private porque nadie
    // más debería usarlo: ni App ni el GestorEnvios registran envíos a mano.
    //
    // La validación de "¿este envío realmente me pertenece?" garantiza que la
    // lista nunca se desincronice de la relación cliente <-> envío.
    void registrarEnvio(Envio envio) {
        if (envio == null) {
            throw new IllegalArgumentException("El envío no puede ser nulo.");
        }
        if (envio.getCliente() != this) {
            throw new IllegalStateException("El envío no pertenece a este cliente.");
        }
        if (!envios.contains(envio)) {
            envios.add(envio);
        }
    }

    // Copia inmutable: se puede consultar, pero no se puede modificar desde afuera.
    public List<Envio> getEnvios() {
        return List.copyOf(envios);
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }

    @Override
    public String toString() {
        return nombre + " (" + id + ")";
    }
}

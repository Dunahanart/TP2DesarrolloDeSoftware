import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

// Registro central de los envíos del sistema.
//
// Responsabilidad: registrar y CONSULTAR múltiples envíos, y calcular
// información general sobre todos ellos. Centralizar esto acá evita que App
// tenga que guardar listas y recorrerlas buscando cosas.
//
// Ojo: NO administra los envíos del cliente — eso le corresponde al Cliente
// (ver Cliente.registrarEnvio). Acá solo queda el "mapa" del sistema completo.
public class GestorEnvios {

    private final List<Envio> envios;

    public GestorEnvios() {
        this.envios = new ArrayList<>();
    }

    // Registrar un envío es un acto de negocio con reglas: tiene que existir,
    // no estar registrado y no puede estar vacío.
    public void registrar(Envio envio) {
        if (envio == null) {
            throw new IllegalArgumentException("El envío no puede ser nulo.");
        }
        if (envio.getPaquetes().isEmpty()) {
            throw new IllegalStateException(
                "No se puede registrar un envío sin paquetes (envío #" + envio.getId() + ").");
        }
        if (envios.contains(envio)) {
            throw new IllegalStateException("El envío #" + envio.getId() + " ya está registrado.");
        }
        envios.add(envio);
        System.out.println("Envío #" + envio.getId() + " registrado para "
            + envio.getCliente().getNombre() + ".");
    }

    // Operar sobre un envío inexistente debe FALLAR con un mensaje claro.
    public Envio buscarPorId(int id) {
        for (Envio envio : envios) {
            if (envio.getId() == id) {
                return envio;
            }
        }
        throw new NoSuchElementException("No existe ningún envío registrado con id " + id + ".");
    }

    // El gestor no duplica la lista de envíos del cliente: se la PREGUNTA al
    // cliente, que es el dueño de esa información (Tell, don't ask).
    public List<Envio> enviosDe(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente no puede ser nulo.");
        }
        return cliente.getEnvios();
    }

    public int cantidadEnvios() {
        return envios.size();
    }

    public double costoTotal() {
        return envios.stream().mapToDouble(Envio::calcularCosto).sum();
    }

    public double costoPromedio() {
        return envios.isEmpty() ? 0 : costoTotal() / envios.size();
    }

    public List<Envio> getEnvios() {
        return List.copyOf(envios);
    }
}

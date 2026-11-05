import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

// TP4, Parte 1: base de datos en memoria del sistema. Guarda los clientes, las
// sucursales y los envíos, y los encuentra cuando se los piden. Existe una
// sola instancia, creada en el main.
//
// Evoluciona al GestorEnvios del TP2: ahora registra todo el sistema, no sólo
// los envíos. Es el "archivo de la oficina": guarda las fichas y las entrega
// cuando se las piden, pero no hace el trabajo de nadie.
//
// Qué NO hace (y por qué):
//   - no realiza acciones del negocio (despachar, entregar, cancelar, demorar):
//     eso sigue en Envio y Sucursal, como en el TP2. Si el nombre del método
//     es una acción del negocio, no va acá;
//   - no valida reglas del negocio (por ejemplo, si un envío tiene paquetes):
//     esas reglas viven en los objetos que las conocen;
//   - no imprime menús ni pide datos por teclado: eso queda en el main.
//
// Lo propio del registro SÍ está acá: que no se registre dos veces el mismo
// código, y avisar con una excepción si algo buscado no existe.
//
// Detalle importante: guarda los MISMOS objetos, no copias. Si buscan un envío
// acá y lo entregan, ese envío queda entregado en todo el sistema; no hace
// falta "volver a guardarlo".
public class RegistroCentral {

    // Clientes en una lista: la Parte 4.a pide buscar por DNI con BÚSQUEDA
    // SECUENCIAL, que recorre la lista desde el principio.
    private final List<Cliente> clientes;

    // Sucursales por su código (Parte 2, opcional).
    private final Map<String, Sucursal> sucursales;

    // Envíos en un HashMap, usando el código de seguimiento (el id del envío)
    // como CLAVE. Dado el código, el valor se encuentra de forma directa, sin
    // recorrer toda la colección (Parte 2).
    private final Map<Integer, Envio> envios;

    public RegistroCentral() {
        this.clientes = new ArrayList<>();
        this.sucursales = new HashMap<>();
        this.envios = new HashMap<>();
    }

    // ── Clientes ────────────────────────────────────────────────────────────

    // Control del registro (no del negocio): que no haya dos clientes con el
    // mismo DNI, que es el dato con el que se los busca.
    public void registrarCliente(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente no puede ser nulo.");
        }
        for (Cliente existente : clientes) {
            if (existente.getDni() == cliente.getDni()) {
                throw new IllegalStateException(
                    "Ya hay un cliente registrado con DNI " + cliente.getDni()
                    + " (" + existente.getNombre() + "); no se registra dos veces.");
            }
        }
        clientes.add(cliente);
    }

    public List<Cliente> listarClientes() {
        return List.copyOf(clientes);
    }

    // Parte 4.a: búsqueda SECUENCIAL por DNI. Se recorre la lista desde el
    // principio hasta encontrar el cliente con ese DNI; si no está, se avisa.
    public Cliente buscarClienteSecuencial(int dni) {
        for (Cliente cliente : clientes) {
            if (cliente.getDni() == dni) {
                return cliente;
            }
        }
        throw new NoSuchElementException(
            "No hay ningún cliente registrado con DNI " + dni + ".");
    }

    // ── Sucursales ──────────────────────────────────────────────────────────

    public void registrarSucursal(Sucursal sucursal) {
        if (sucursal == null) {
            throw new IllegalArgumentException("La sucursal no puede ser nula.");
        }
        if (sucursales.containsKey(sucursal.getId())) {
            throw new IllegalStateException(
                "Ya hay una sucursal registrada con el código " + sucursal.getId() + ".");
        }
        sucursales.put(sucursal.getId(), sucursal);
    }

    public Sucursal buscarSucursal(String codigo) {
        Sucursal sucursal = sucursales.get(codigo);
        if (sucursal == null) {
            throw new NoSuchElementException(
                "No hay ninguna sucursal registrada con el código " + codigo + ".");
        }
        return sucursal;
    }

    public List<Sucursal> listarSucursales() {
        return List.copyOf(sucursales.values());
    }

    // ── Envíos (Parte 2: HashMap por código de seguimiento) ─────────────────

    // Control del registro: que no se registre dos veces el mismo código. La
    // clave del HashMap permite detectarlo de forma directa (containsKey).
    public void registrarEnvio(Envio envio) {
        if (envio == null) {
            throw new IllegalArgumentException("El envío no puede ser nulo.");
        }
        if (envios.containsKey(envio.getId())) {
            throw new IllegalStateException(
                "Ya hay un envío registrado con el código " + envio.getId()
                + "; no se registra dos veces.");
        }
        envios.put(envio.getId(), envio);
    }

    // Con el HashMap no se recorre nada: la clave lleva directo al valor. Si el
    // código no existe, se avisa (excepción, como en el TP2.1).
    public Envio buscarEnvio(int codigo) {
        Envio envio = envios.get(codigo);
        if (envio == null) {
            throw new NoSuchElementException(
                "No hay ningún envío registrado con el código " + codigo + ".");
        }
        return envio;
    }

    public List<Envio> listarEnvios() {
        return List.copyOf(envios.values());
    }

    // ── Consultas generales sobre lo guardado ───────────────────────────────
    //
    // Son CONSULTAS, no acciones del negocio: sumar y promediar no cambian el
    // estado de ningún envío. Los cálculos de cada envío siguen delegados en
    // Envio.calcularCosto().

    public int cantidadEnvios() {
        return envios.size();
    }

    public double costoTotal() {
        return envios.values().stream().mapToDouble(Envio::calcularCosto).sum();
    }

    public double costoPromedio() {
        return envios.isEmpty() ? 0 : costoTotal() / envios.size();
    }
}

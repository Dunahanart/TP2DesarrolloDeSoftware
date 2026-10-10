import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

// Parte 7 del TP1: clase abstracta base de EnvioEstandar, EnvioExpress y
// EnvioInternacional.
//
// TP2: el envío pasa a ser el OBJETO CENTRAL del modelo. Es el responsable de:
//   - conocer a su cliente (Information Expert: él sabe a quién le pertenece);
//   - coordinar a sus paquetes en cada etapa del recorrido (no lo hace App);
//   - administrar su propio historial de movimientos (dueño del agregado);
//   - determinar qué transiciones de estado son válidas (protege su estado).
public abstract class Envio {

    private static final int LIMITE_PAQUETES = 3;

    private static int contadorId = 1;

    private final int id;
    private final Cliente cliente;
    private final String destino;
    private final List<Paquete> paquetes;

    // Historial: UNO A MUCHOS, administrado por el propio envío. Es la
    // información que permite reconstruir "¿cómo llegó hasta acá?".
    private final List<Movimiento> historial;

    private Sucursal sucursalOrigen;   // dónde empezó el recorrido
    private Sucursal sucursalActual;   // qué sucursal lo está / estuvo procesando
    private EstadoEnvio estado;

    // El envío nace REGISTRADO, asociado a su cliente y con sus paquetes.
    public Envio(Cliente cliente, String destino, Paquete... paquetes) {
        if (cliente == null) {
            throw new IllegalArgumentException("El envío debe estar asociado a un cliente.");
        }
        if (destino == null || destino.isBlank()) {
            throw new IllegalArgumentException("El destino no puede estar vacío.");
        }
        this.id = contadorId++;
        this.cliente = cliente;
        this.destino = destino;
        this.paquetes = new ArrayList<>();
        this.historial = new ArrayList<>();
        this.estado = EstadoEnvio.REGISTRADO;

        for (Paquete paquete : paquetes) {
            agregarPaquete(paquete);
        }

        // ¿Quién registra que el cliente realizó un envío? El propio envío, al
        // crearse: es el que conoce a su cliente y a sí mismo. Así App nunca
        // toca la lista de envíos del cliente (Tell, don't ask + Creator).
        cliente.registrarEnvio(this);
    }

    // ── Reglas del negocio, con nombre propio ───────────────────────────────
    //
    // TP2.1: las comprobaciones se extraen como métodos privados con nombre,
    // en lugar de condiciones sueltas repetidas por el código. Son
    // reutilizables y hacen que cada operación exprese su intención:
//
    //     if (!todaviaNoSalio()) { ... }   en vez de   if (estado != REGISTRADO) { ... }
    //
    // Ninguno es público: son implementación interna del objeto. Los demás
    // piden el comportamiento (recibir/despachar/entregar) y no necesitan
    // saber con qué reglas el envío decide si puede hacerlo.

    private boolean todaviaNoSalio() {
        return estado == EstadoEnvio.REGISTRADO;
    }

    private boolean enPreparacion() {
        return estado == EstadoEnvio.EN_PREPARACION;
    }

    private boolean estaEnViaje() {
        return estado == EstadoEnvio.EN_VIAJE;
    }

    private boolean estaFinalizado() {
        return estado == EstadoEnvio.ENTREGADO;
    }

    private boolean hayLugarParaPaquetes() {
        return paquetes.size() < LIMITE_PAQUETES;
    }

    public void agregarPaquete(Paquete paquete) {
        if (paquete == null) {
            throw new IllegalArgumentException("El paquete no puede ser nulo.");
        }
        if (!todaviaNoSalio()) {
            throw new IllegalStateException(
                "No se pueden agregar paquetes a un envío que ya fue recibido por una sucursal.");
        }
        if (!hayLugarParaPaquetes()) {
            throw new IllegalStateException(
                "Un envío no puede tener más de " + LIMITE_PAQUETES + " paquetes.");
        }
        paquetes.add(paquete);
    }

    // ── Avance de estados ───────────────────────────────────────────────────
    //
    // TP2.1: el cambio de estado NO se resuelve con cadenas de if/else ni con
    // un switch que enumere todas las etapas. La transición válida la define
    // el objeto que conoce y protege el estado, con el mismo criterio usado en
    // Paquete: sólo se avanza DE UN PASO. Cualquier intento de saltarse o
    // retroceder una etapa cae en la excepción.
    private boolean transicionValida(EstadoEnvio nuevoEstado) {
        return nuevoEstado.ordinal() == estado.ordinal() + 1;
    }

    private void avanzarA(EstadoEnvio nuevoEstado) {
        if (!transicionValida(nuevoEstado)) {
            throw new IllegalStateException(
                "Transición inválida del envío: " + estado + " -> " + nuevoEstado);
        }
        estado = nuevoEstado;
    }

    // ── Ciclo de vida: lo mueven las sucursales, no App ─────────────────────
    //
    // Cadena de colaboración (TP2):
    //
    //     App --> Sucursal.recibir(envio) --> Envio.recibirEn(sucursal) --> Paquete.recibir()
    //
    // Estos métodos son package-private: fuera del modelo nadie debería poder
    // mover un envío por su recorrido. Sólo la sucursal que lo procesa (y
    // entregar(), público porque el destino final no es una sucursal).

    // El envío llega a una sucursal. Si estaba REGISTRADO es su primera parada
    // y pasa a preparación; si estaba EN_VIAJE es una sucursal intermedia y el
    // envío sigue de tránsito (la máquina de estados no retrocede): sólo se
    // registra el paso en el historial.
    void recibirEn(Sucursal sucursal) {
        validarSucursal(sucursal);
        if (paquetes.isEmpty()) {
            throw new IllegalStateException("No se puede recibir un envío sin paquetes.");
        }
        if (estaFinalizado()) {
            throw new IllegalStateException(
                "El envío #" + id + " ya fue entregado y no puede recibirse de nuevo.");
        }
        if (enPreparacion()) {
            throw new IllegalStateException(
                "El envío #" + id + " ya está en preparación en otra sucursal; "
                + "debe ser despachado antes de moverse.");
        }
        if (todaviaNoSalio()) {
            avanzarA(EstadoEnvio.EN_PREPARACION);
            sucursalOrigen = sucursal;
            for (Paquete paquete : paquetes) {
                paquete.recibir();
            }
        }
        sucursalActual = sucursal;
        registrarMovimiento(TipoMovimiento.RECIBIDO, sucursal);
        System.out.println("Envío #" + id + " recibido en la sucursal " + sucursal.getNombre()
            + " (estado: " + estado + ").");
    }

    // El envío sale de una sucursal hacia su próxima parada. Si estaba EN_VIAJE
    // (sucursal intermedia) sigue su tránsito: sólo se registra el movimiento.
    void despacharDesde(Sucursal sucursal) {
        validarSucursal(sucursal);
        if (todaviaNoSalio()) {
            throw new IllegalStateException(
                "El envío #" + id + " debe ser recibido por una sucursal antes de despacharlo.");
        }
        if (estaFinalizado()) {
            throw new IllegalStateException(
                "No se puede despachar el envío #" + id + ": ya fue entregado.");
        }
        if (enPreparacion()) {
            avanzarA(EstadoEnvio.EN_VIAJE);
            for (Paquete paquete : paquetes) {
                paquete.iniciarViaje();
            }
        }
        registrarMovimiento(TipoMovimiento.DESPACHADO, sucursal);
        System.out.println("Envío #" + id + " despachado desde la sucursal " + sucursal.getNombre()
            + " (estado: " + estado + ").");
    }

    // El envío llega a su destino final y se entrega. Es público porque el
    // destino no siempre es una sucursal de la red (por ejemplo: "Neuquén").
    public void entregar() {
        if (estaFinalizado()) {
            throw new IllegalStateException("El envío #" + id + " ya fue entregado.");
        }
        if (!estaEnViaje()) {
            throw new IllegalStateException(
                "El envío #" + id + " debe ser despachado antes de poder entregarse "
                + "(estado actual: " + estado + ").");
        }
        avanzarA(EstadoEnvio.ENTREGADO);
        for (Paquete paquete : paquetes) {
            paquete.entregar();
        }
        registrarMovimiento(TipoMovimiento.ENTREGADO, null);
        System.out.println("Envío #" + id + " entregado en " + destino + ".");
    }

    private void validarSucursal(Sucursal sucursal) {
        if (sucursal == null) {
            throw new IllegalArgumentException("La sucursal no puede ser nula.");
        }
    }

    // El envío es dueño de su historial (Information Expert + Creator): es el
    // único que sabe qué le pasó, cuándo y dónde, y el único que crea Movimiento.
    private void registrarMovimiento(TipoMovimiento tipo, Sucursal sucursal) {
        historial.add(new Movimiento(tipo, sucursal));
    }

    // ── Consultas de seguimiento (Nivel 2 / TP2.1) ──────────────────────────
    //
    // TP2.1: las consultas DEVUELVEN el dato; quien lo muestre es otro asunto
    // (por ejemplo App). No hay métodos cuyo único objetivo sea imprimir.
    //
    //     App --> envio.getUltimoMovimiento() --> Movimiento --> System.out
    //
    // El objeto sigue respondiendo por su propia información (Tell, don't ask):
    // nadie recorre el historial crudo para reconstruir el recorrido.

    // Copia inmutable: se puede consultar, pero no se modifica desde afuera.
    public List<Movimiento> getHistorial() {
        return List.copyOf(historial);
    }

    // Último movimiento registrado, o excepción si el envío no tiene recorrido.
    public Movimiento getUltimoMovimiento() {
        if (historial.isEmpty()) {
            throw new NoSuchElementException(
                "El envío #" + id + " no tiene movimientos registrados.");
        }
        return historial.get(historial.size() - 1);
    }

    // Sucursales por las que pasó el envío, en orden de primera pasada y sin
    // repetir. El cálculo vive acá (no en App): reconstruir el recorrido es
    // información que sólo el envío conoce.
    public List<Sucursal> getRecorrido() {
        List<Sucursal> recorrido = new ArrayList<>();
        for (Movimiento movimiento : historial) {
            Sucursal sucursal = movimiento.getSucursal();
            if (sucursal != null && !recorrido.contains(sucursal)) {
                recorrido.add(sucursal);
            }
        }
        return List.copyOf(recorrido);
    }

    // ── Partes 5 y 7 del TP1: costo delegado en cada subclase ──────────────

    public double calcularCosto() {
        double base = calcularCostoBase();
        double recargo = aplicarRecargo(base);
        return base + recargo;
    }

    protected abstract double calcularCostoBase();

    protected double aplicarRecargo(double costoBase) {
        return 0;
    }

    protected double calcularPesoTotal() {
        return paquetes.stream().mapToDouble(Paquete::getPesoKg).sum();
    }

    // Getters
    public int getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public String getDestino() { return destino; }
    public EstadoEnvio getEstado() { return estado; }
    public Sucursal getSucursalOrigen() { return sucursalOrigen; }
    public Sucursal getSucursalActual() { return sucursalActual; }
    public List<Paquete> getPaquetes() { return List.copyOf(paquetes); }

    @Override
    public String toString() {
        return String.format("Envío #%d hacia %s (%s) - Cliente: %s - Estado: %s - Costo: $%.2f",
            id, destino, getClass().getSimpleName(), cliente.getNombre(), estado, calcularCosto());
    }
}

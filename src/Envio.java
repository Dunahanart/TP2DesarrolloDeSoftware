import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Parte 7 del TP1: clase abstracta base de EnvioEstandar, EnvioExpress y
// EnvioInternacional.
//
// TP2: el envío pasa a ser el OBJETO CENTRAL del modelo. Es el responsable de:
//   - conocer a su cliente (Information Expert: él sabe a quién le pertenece);
//   - coordinar a sus paquetes en cada etapa del recorrido (no lo hace App);
//   - administrar su propio historial de movimientos (dueño del agregado).
public abstract class Envio {

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
    // Se eliminaron los constructores alternativos del TP1: un envío sin cliente
    // ya no tiene sentido en este modelo, y los paquetes llegan por varargs.
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

    public void agregarPaquete(Paquete paquete) {
        if (paquete == null) {
            throw new IllegalArgumentException("El paquete no puede ser nulo.");
        }
        if (estado != EstadoEnvio.REGISTRADO) {
            throw new IllegalStateException(
                "No se pueden agregar paquetes a un envío que ya fue recibido por una sucursal.");
        }
        if (paquetes.size() >= 3) {
            throw new IllegalStateException("Un envío no puede tener más de 3 paquetes.");
        }
        paquetes.add(paquete);
    }

    // ── Ciclo de vida: lo mueven las sucursales, no App ─────────────────────
    //
    // Se reemplazó el ciclo del TP1 (iniciar / salirADistribucion / finalizar)
    // por recibir / despachar / entregar, porque ahora las ETAPAS las define el
    // recorrido físico entre sucursales. La cadena de colaboración es:
    //
    //     App --> Sucursal.recibir(envio) --> Envio.recibirEn(sucursal) --> Paquete.recibir()
    //
    // Estos métodos son package-private: fuera del modelo nadie debería poder
    // mover un envío por su recorrido. Solo la sucursal que lo procesa (y
    // Envio.entregar(), público porque el destino final no es una sucursal).

    // El envío llega a una sucursal.
    //   - REGISTRADO    -> pasa a EN_PREPARACION y sus paquetes reciben (primer parada).
    //   - EN_VIAJE      -> es una sucursal intermedia: el envío sigue EN_VIAJE y
    //                      solo se registra el paso en el historial (los paquetes
    //                      no retroceden: la máquina de estados avanza de a un paso).
    //   - EN_PREPARACION/ENTREGADO -> etapa incorrecta, operación impedida.
    void recibirEn(Sucursal sucursal) {
        validarSucursal(sucursal);
        if (paquetes.isEmpty()) {
            throw new IllegalStateException("No se puede recibir un envío sin paquetes.");
        }
        switch (estado) {
            case REGISTRADO -> {
                estado = EstadoEnvio.EN_PREPARACION;
                sucursalOrigen = sucursal;
                for (Paquete paquete : paquetes) {
                    paquete.recibir();
                }
            }
            case EN_VIAJE -> {
                // tránsito: nada que cambiar de estado, solo registrar el paso
            }
            case EN_PREPARACION -> throw new IllegalStateException(
                "El envío #" + id + " ya está en preparación en otra sucursal; "
                + "debe ser despachado antes de moverse.");
            case ENTREGADO -> throw new IllegalStateException(
                "El envío #" + id + " ya fue entregado y no puede recibirse de nuevo.");
        }
        sucursalActual = sucursal;
        registrarMovimiento(TipoMovimiento.RECIBIDO, sucursal);
        System.out.println("Envío #" + id + " recibido en la sucursal " + sucursal.getNombre()
            + " (estado: " + estado + ").");
    }

    // El envío sale de una sucursal hacia su próxima parada.
    //   - EN_PREPARACION -> pasa a EN_VIAJE y sus paquetes inician el viaje.
    //   - EN_VIAJE       -> sigue en tránsito (parada intermedia).
    //   - REGISTRADO     -> nadie lo recibió todavía.
    //   - ENTREGADO      -> despachar algo entregado está impedido.
    void despacharDesde(Sucursal sucursal) {
        validarSucursal(sucursal);
        switch (estado) {
            case REGISTRADO -> throw new IllegalStateException(
                "El envío #" + id + " debe ser recibido por una sucursal antes de despacharlo.");
            case EN_PREPARACION -> {
                estado = EstadoEnvio.EN_VIAJE;
                for (Paquete paquete : paquetes) {
                    paquete.iniciarViaje();
                }
            }
            case EN_VIAJE -> {
                // sigue su tránsito
            }
            case ENTREGADO -> throw new IllegalStateException(
                "No se puede despachar el envío #" + id + ": ya fue entregado.");
        }
        registrarMovimiento(TipoMovimiento.DESPACHADO, sucursal);
        System.out.println("Envío #" + id + " despachado desde la sucursal " + sucursal.getNombre()
            + " (estado: " + estado + ").");
    }

    // El envío llega a su destino final y se entrega. Es público porque el
    // destino no siempre es una sucursal de la red (por ejemplo: "Neuquén").
    // Valida la etapa: entregar algo que no fue despachado está impedido.
    public void entregar() {
        if (estado == EstadoEnvio.ENTREGADO) {
            throw new IllegalStateException("El envío #" + id + " ya fue entregado.");
        }
        if (estado != EstadoEnvio.EN_VIAJE) {
            throw new IllegalStateException(
                "El envío #" + id + " debe ser despachado antes de poder entregarse "
                + "(estado actual: " + estado + ").");
        }
        estado = EstadoEnvio.ENTREGADO;
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
    // Private: los demás piden comportamiento, no fabrican registros.
    private void registrarMovimiento(TipoMovimiento tipo, Sucursal sucursal) {
        historial.add(new Movimiento(tipo, sucursal));
    }

    // ── Consultas de seguimiento (Nivel 2) ──────────────────────────────────
    //
    // Se le pregunta al envío en lugar de extraer sus movimientos y recorrerlos
    // desde afuera: "preguntar por comportamiento antes que pedir datos internos".

    public void mostrarHistorial() {
        System.out.println("Historial del envío #" + id + " (" + destino + "):");
        if (historial.isEmpty()) {
            System.out.println("  (sin movimientos registrados)");
            return;
        }
        for (Movimiento movimiento : historial) {
            System.out.println("  " + movimiento);
        }
    }

    public void mostrarRecorrido() {
        List<String> sucursales = new ArrayList<>();
        for (Movimiento movimiento : historial) {
            Sucursal sucursal = movimiento.getSucursal();
            if (sucursal != null && !sucursales.contains(sucursal.getNombre())) {
                sucursales.add(sucursal.getNombre());
            }
        }
        System.out.println("Envío #" + id + " pasó por: "
            + (sucursales.isEmpty() ? "(todavía no pasó por ninguna sucursal)"
                                    : String.join(" -> ", sucursales)));
    }

    public void mostrarUltimoMovimiento() {
        System.out.println(historial.isEmpty()
            ? "Envío #" + id + ": sin movimientos registrados."
            : "Último movimiento del envío #" + id + ": " + historial.get(historial.size() - 1));
    }

    // Copia inmutable del historial para consultas puntuales externas; la
    // administración sigue siendo exclusiva del envío.
    public List<Movimiento> getHistorial() {
        return List.copyOf(historial);
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

    public void mostrarResumen() {
        System.out.printf(
            "Envío #%d | Cliente: %-18s | Tipo: %-18s | Destino: %-16s | Origen: %-14s | Paquetes: %d | Estado: %-14s | Costo: $%.2f%n",
            id,
            cliente.getNombre(),
            getClass().getSimpleName(),
            destino,
            sucursalOrigen == null ? "(sin procesar)" : sucursalOrigen.getNombre(),
            paquetes.size(),
            estado,
            calcularCosto());
    }

    @Override
    public String toString() {
        return String.format("Envío #%d hacia %s (%s) - Estado: %s - Costo: $%.2f",
            id, destino, getClass().getSimpleName(), estado, calcularCosto());
    }
}

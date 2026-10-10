import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Registro de UN acontecimiento del recorrido de un envío.
//
// atributos:
//   - tipo:    ¿qué pasó?        (RECIBIDO / DESPACHADO / ENTREGADO)
//   - fecha:   ¿cuándo pasó?     (se toma sola, en el momento de crear el registro)
//   - sucursal ¿dónde pasó?      (null solo si el envío fue entregado en su destino
//                                 final, que no siempre es una sucursal de la red)
//
// Es INMUTABLE (clase final, campos final, sin setters): una vez registrado,
// la historia no se puede reescribir.
//
// Su constructor es package-private a propósito: nadie desde fuera del sistema
// debería fabricar movimientos. Solo quien administra el recorrido (Envio) puede
// registrar lo que le ocurre a su envío — ver Envio.registrarMovimiento(...).
public final class Movimiento {

    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final LocalDateTime fecha;
    private final TipoMovimiento tipo;
    private final Sucursal sucursal;

    Movimiento(TipoMovimiento tipo, Sucursal sucursal) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de movimiento no puede ser nulo.");
        }
        this.fecha = LocalDateTime.now();
        this.tipo = tipo;
        this.sucursal = sucursal;
    }

    public LocalDateTime getFecha() { return fecha; }
    public TipoMovimiento getTipo() { return tipo; }
    public Sucursal getSucursal() { return sucursal; }

    @Override
    public String toString() {
        String lugar = (sucursal == null) ? "en el destino final" : "en " + sucursal.getNombre();
        return String.format("%s | %s %s", fecha.format(FORMATO_FECHA), tipo, lugar);
    }
}

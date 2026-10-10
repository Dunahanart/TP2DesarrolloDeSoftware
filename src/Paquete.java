public class Paquete {

    // Partes 1 y 2 del TP1: identidad propia, info protegida, estado encapsulado
    private final String id;
    private final String descripcion;
    private final double pesoKg;
    private EstadoPaquete estado;

    // Parte 3 del TP1: constructor con información mínima
    public Paquete(String id, double pesoKg) {
        this(id, "Sin descripción", pesoKg);
    }

    // Parte 3 del TP1: constructor con información completa
    public Paquete(String id, String descripcion, double pesoKg) {
        validarPeso(pesoKg);
        this.id = id;
        this.descripcion = descripcion;
        this.pesoKg = pesoKg;
        this.estado = EstadoPaquete.RECIBIDO;
    }

    // Parte 5 del TP1: validación interna, no expuesta públicamente
    private void validarPeso(double pesoKg) {
        if (pesoKg <= 0) {
            throw new IllegalArgumentException("El peso debe ser mayor a cero.");
        }
    }

    // Parte 5 del TP1: verifica si la transición de estado es válida.
    // Método private porque es implementación interna del objeto: los demás solo
    // piden "avanzar" y no necesitan saber cómo el paquete decide si puede.
    private boolean transicionValida(EstadoPaquete nuevoEstado) {
        return nuevoEstado.ordinal() == this.estado.ordinal() + 1;
    }

    // ── TP2: cambios de estado del paquete ──────────────────────────────────
    //
    // Son package-private a propósito. Un paquete no cambia de estado porque un
    // objeto externo lo pida, sino porque su Envio coordina el recorrido:
    //
    //     Sucursal.recibir(envio)  ->  Envio.recibirEn(...)     ->  Paquete.recibir()
    //     Sucursal.despachar(...)  ->  Envio.despacharDesde(..) ->  Paquete.iniciarViaje()
    //     Envio.entregar()         ->                          ->  Paquete.entregar()
    //
    // Al no ser públicos queda explícito en el diseño quién manda sobre el
    // estado del paquete: su envío, nunca App ni una sucursal directamente.
    void recibir() {
        avanzarEstado(EstadoPaquete.EN_PREPARACION);
    }

    void iniciarViaje() {
        avanzarEstado(EstadoPaquete.EN_VIAJE);
    }

    void entregar() {
        avanzarEstado(EstadoPaquete.ENTREGADO);
    }

    // Parte 5 del TP1: lógica de transición encapsulada
    private void avanzarEstado(EstadoPaquete nuevoEstado) {
        if (!transicionValida(nuevoEstado)) {
            throw new IllegalStateException(
                "Transición inválida: " + this.estado + " -> " + nuevoEstado
            );
        }
        this.estado = nuevoEstado;
    }

    // Getters — sin setters directos para proteger el estado
    public String getId() { return id; }
    public String getDescripcion() { return descripcion; }
    public double getPesoKg() { return pesoKg; }
    public EstadoPaquete getEstado() { return estado; }

    @Override
    public String toString() {
        return String.format("[Paquete %s | %s | %.2f kg | %s]",
            id, descripcion, pesoKg, estado);
    }
}

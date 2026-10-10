public class Paquete {

    // Partes 1 y 2: identidad propia, info protegida, estado encapsulado
    private final String id;
    private final String descripcion;
    private final double pesoKg;
    private EstadoPaquete estado;

    // Parte 3: constructor con información mínima
    public Paquete(String id, double pesoKg) {
        this(id, "Sin descripción", pesoKg);
    }

    // Parte 3: constructor con información completa
    public Paquete(String id, String descripcion, double pesoKg) {
        validarPeso(pesoKg);
        this.id = id;
        this.descripcion = descripcion;
        this.pesoKg = pesoKg;
        this.estado = EstadoPaquete.RECIBIDO;
    }

    // Parte 5: validación interna, no expuesta públicamente
    private void validarPeso(double pesoKg) {
        if (pesoKg <= 0) {
            throw new IllegalArgumentException("El peso debe ser mayor a cero.");
        }
    }

    // Parte 5: verifica si la transición de estado es válida
    private boolean transicionValida(EstadoPaquete nuevoEstado) {
        return nuevoEstado.ordinal() == this.estado.ordinal() + 1;
    }

    // Parte 2 y 4: acciones reales que provocan cambios de estado
    public void iniciarPreparacion() {
        avanzarEstado(EstadoPaquete.EN_PREPARACION);
    }

    public void salirADistribucion() {
        avanzarEstado(EstadoPaquete.EN_DISTRIBUCION);
    }

    public void marcarEntregado() {
        avanzarEstado(EstadoPaquete.ENTREGADO);
    }

    // Parte 5: lógica de transición encapsulada
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

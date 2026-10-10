// Parte 7: envío internacional — tarifa alta + recargo porcentual por aduana
public class EnvioInternacional extends Envio {

    private static final double TARIFA_POR_KG = 500.0;
    private static final double PORCENTAJE_ADUANA = 0.20; // 20%

    private final String paisDestino;

    public EnvioInternacional(String ciudadDestino, String paisDestino) {
        super(ciudadDestino + ", " + paisDestino);
        this.paisDestino = paisDestino;
    }

    public EnvioInternacional(String ciudadDestino, String paisDestino, Paquete paquete) {
        this(ciudadDestino, paisDestino);
        agregarPaquete(paquete);
    }

    @Override
    protected double calcularCostoBase() {
        return calcularPesoTotal() * TARIFA_POR_KG;
    }

    // Parte 5: recargo de aduana encapsulado
    @Override
    protected double aplicarRecargo(double costoBase) {
        return costoBase * PORCENTAJE_ADUANA;
    }

    public String getPaisDestino() { return paisDestino; }
}

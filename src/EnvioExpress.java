// Parte 7: envío express — tarifa mayor + recargo fijo por urgencia
public class EnvioExpress extends Envio {

    private static final double TARIFA_POR_KG = 300.0;
    private static final double RECARGO_URGENCIA = 500.0;

    public EnvioExpress(String destino) {
        super(destino);
    }

    public EnvioExpress(String destino, Paquete paquete) {
        super(destino, paquete);
    }

    @Override
    protected double calcularCostoBase() {
        return calcularPesoTotal() * TARIFA_POR_KG;
    }

    // Parte 5: recargo interno por urgencia
    @Override
    protected double aplicarRecargo(double costoBase) {
        return RECARGO_URGENCIA;
    }
}

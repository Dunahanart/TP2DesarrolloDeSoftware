// Parte 7 del TP1: envío express — tarifa mayor + recargo fijo por urgencia.
// El TP2 solo cambia la firma del constructor: todo envío ahora tiene cliente.
public class EnvioExpress extends Envio {

    private static final double TARIFA_POR_KG = 300.0;
    private static final double RECARGO_URGENCIA = 500.0;

    public EnvioExpress(Cliente cliente, String destino, Paquete... paquetes) {
        super(cliente, destino, paquetes);
    }

    @Override
    protected double calcularCostoBase() {
        return calcularPesoTotal() * TARIFA_POR_KG;
    }

    // Parte 5 del TP1: recargo interno por urgencia
    @Override
    protected double aplicarRecargo(double costoBase) {
        return RECARGO_URGENCIA;
    }
}

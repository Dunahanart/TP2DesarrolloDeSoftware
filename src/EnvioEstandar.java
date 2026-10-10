// Parte 7: envío estándar — costo base por peso
public class EnvioEstandar extends Envio {

    private static final double TARIFA_POR_KG = 150.0;

    public EnvioEstandar(String destino) {
        super(destino);
    }

    public EnvioEstandar(String destino, Paquete paquete) {
        super(destino, paquete);
    }

    @Override
    protected double calcularCostoBase() {
        return calcularPesoTotal() * TARIFA_POR_KG;
    }
}

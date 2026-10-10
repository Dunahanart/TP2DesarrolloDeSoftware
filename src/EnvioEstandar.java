// Parte 7 del TP1: envío estándar — costo base por peso.
// El TP2 solo cambia la firma del constructor: todo envío ahora tiene cliente.
public class EnvioEstandar extends Envio {

    private static final double TARIFA_POR_KG = 150.0;

    public EnvioEstandar(Cliente cliente, String destino, Paquete... paquetes) {
        super(cliente, destino, paquetes);
    }

    @Override
    protected double calcularCostoBase() {
        return calcularPesoTotal() * TARIFA_POR_KG;
    }
}

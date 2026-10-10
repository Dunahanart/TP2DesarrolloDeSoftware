// Parte 7 del TP1: envío internacional — tarifa alta + recargo porcentual por aduana.
// El TP2 solo cambia la firma del constructor: todo envío ahora tiene cliente.
public class EnvioInternacional extends Envio {

    private static final double TARIFA_POR_KG = 500.0;
    private static final double PORCENTAJE_ADUANA = 0.20; // 20%

    private final String paisDestino;

    public EnvioInternacional(Cliente cliente, String ciudadDestino, String paisDestino, Paquete... paquetes) {
       
        super(cliente, ciudadDestino + ", " + validarPais(paisDestino), paquetes);
        this.paisDestino = paisDestino;
    }

    private static String validarPais(String paisDestino) {
        if (paisDestino == null || paisDestino.isBlank()) {
            throw new IllegalArgumentException("El país de destino no puede estar vacío.");
        }
        return paisDestino;
    }

    @Override
    protected double calcularCostoBase() {
        return calcularPesoTotal() * TARIFA_POR_KG;
    }

    // Parte 5 del TP1: recargo de aduana encapsulado
    @Override
    protected double aplicarRecargo(double costoBase) {
        return costoBase * PORCENTAJE_ADUANA;
    }

    public String getPaisDestino() { return paisDestino; }
}

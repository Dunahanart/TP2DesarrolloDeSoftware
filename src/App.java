import java.util.NoSuchElementException;

/* App es sólo la CAPA DE PRESENTACIÓN y de orquestación de la demostración:
crea los objetos, les manda mensajes y MUESTRA los datos que los objetos le
devuelven. Nunca recorre ni modifica el interior de otros objetos. */
public class App {

    public static void main(String[] args) {

        System.out.println("--- SISTEMA DE GESTIÓN DE ENVÍOS Y DISTRIBUCIÓN ---\n");

        // ── Nivel 1: clientes y sucursales ──────────────────────────────────
        System.out.println("-- Clientes y sucursales --");
        Cliente cliente1 = new Cliente("CLI-001", "María Gómez");
        Cliente cliente2 = new Cliente("CLI-002", "Distribuidora Sur");
        System.out.println(cliente1);
        System.out.println(cliente2);

        Sucursal viedma = new Sucursal("SUC-01", "Viedma");
        Sucursal bahiaBlanca = new Sucursal("SUC-02", "Bahía Blanca");
        Sucursal buenosAires = new Sucursal("SUC-03", "Buenos Aires");
        System.out.println(viedma);
        System.out.println(bahiaBlanca);
        System.out.println(buenosAires);

        // ── Nivel 1: alta de envíos (cada envío nace asociado a su cliente) ─
        System.out.println("\n-- Alta de envíos --");
        Envio envio1 = new EnvioEstandar(cliente1, "Neuquén",
            new Paquete("PKG-001", 2.5),
            new Paquete("PKG-002", "Libros de texto", 1.8));
        Envio envio2 = new EnvioExpress(cliente1, "Buenos Aires",
            new Paquete("PKG-003", 0.9));
        Envio envio3 = new EnvioInternacional(cliente2, "Santiago", "Chile",
            new Paquete("PKG-004", "Electrónica frágil", 3.2));

        GestorEnvios gestor = new GestorEnvios();
        gestor.registrar(envio1);
        gestor.registrar(envio2);
        gestor.registrar(envio3);

        // ── Nivel 1: el recorrido lo mueven las sucursales ──────────────────
        // Colaboración: App --> Sucursal --> Envio --> Paquete
        System.out.println("\n-- Recorrido del envío #1 (varias sucursales) --");
        viedma.recibir(envio1);         // REGISTRADO -> EN_PREPARACION
        viedma.despachar(envio1);       // EN_PREPARACION -> EN_VIAJE
        bahiaBlanca.recibir(envio1);    // sucursal intermedia: sigue EN_VIAJE
        bahiaBlanca.despachar(envio1);
        buenosAires.recibir(envio1);
        buenosAires.despachar(envio1);
        envio1.entregar();              // llega a su destino final

        System.out.println("\n-- Recorrido del envío #2 (express, una sola sucursal) --");
        buenosAires.recibir(envio2);
        buenosAires.despachar(envio2);
        envio2.entregar();

        System.out.println("\n-- Recorrido del envío #3 (internacional, queda en tránsito) --");
        viedma.recibir(envio3);
        viedma.despachar(envio3);

        // ── Nivel 2 y TP2.1: las consultas DEVUELVEN datos y App los muestra ─
        //     App --> envio.getUltimoMovimiento() --> Movimiento --> System.out
        System.out.println("\n-- Nivel 2: seguimiento del envío #1 --");
        System.out.println("Historial completo:");
        for (Movimiento movimiento : envio1.getHistorial()) {
            System.out.println("  " + movimiento);
        }
        System.out.println("Sucursales por las que pasó: " + envio1.getRecorrido());
        System.out.println("Último movimiento: " + envio1.getUltimoMovimiento());

        System.out.println("\n-- Nivel 2: seguimiento del envío #3 (aún en viaje) --");
        System.out.println("Historial completo:");
        for (Movimiento movimiento : envio3.getHistorial()) {
            System.out.println("  " + movimiento);
        }
        System.out.println("Sucursales por las que pasó: " + envio3.getRecorrido());
        System.out.println("Último movimiento: " + envio3.getUltimoMovimiento());

        // ── Nivel 1: cada cliente conoce sus propios envíos ─────────────────
        System.out.println("\nEnvíos de " + cliente1 + ":");
        for (Envio envio : cliente1.getEnvios()) {
            System.out.println("  " + envio);
        }
        System.out.println("Envíos de " + cliente2 + ":");
        for (Envio envio : cliente2.getEnvios()) {
            System.out.println("  " + envio);
        }

        // ── Nivel 3: información general del sistema ────────────────────────
        System.out.println("\n-- Resumen general del sistema --");
        System.out.println("  Envíos registrados: " + gestor.cantidadEnvios());
        System.out.printf("  Costo total:        $%.2f%n", gestor.costoTotal());
        System.out.printf("  Costo promedio:     $%.2f%n", gestor.costoPromedio());

        System.out.println("Envíos registrados:");
        for (Envio envio : gestor.getEnvios()) {
            System.out.println("  " + envio);
        }
        System.out.println();
        mostrarFicha(gestor.buscarPorId(2));

        // ── Nivel 3: operaciones inválidas, todas impedidas ─────────────────
        System.out.println("\n-- Pruebas de validación (errores esperados) --");

        // 1) recibir un envío que ya fue entregado
        try {
            buenosAires.recibir(envio1);
        } catch (IllegalStateException e) {
            System.out.println("1) " + e.getMessage());
        }

        // 2) registrar un envío sin paquetes
        try {
            gestor.registrar(new EnvioEstandar(cliente2, "Mendoza"));
        } catch (IllegalStateException e) {
            System.out.println("2) " + e.getMessage());
        }

        // 3) operar sobre un envío inexistente
        try {
            gestor.buscarPorId(999);
        } catch (NoSuchElementException e) {
            System.out.println("3) " + e.getMessage());
        }

        Envio envio4 = new EnvioEstandar(cliente2, "Bariloche",
            new Paquete("PKG-005", 1.0));
        gestor.registrar(envio4);

        // 4) despachar un envío que ninguna sucursal recibió todavía
        try {
            viedma.despachar(envio4);
        } catch (IllegalStateException e) {
            System.out.println("4) " + e.getMessage());
        }

        // 5) entregar un envío que no fue despachado
        try {
            envio4.entregar();
        } catch (IllegalStateException e) {
            System.out.println("5) " + e.getMessage());
        }

        // 6) mover un envío de sucursal antes de que sea despachado
        viedma.recibir(envio4);
        try {
            bahiaBlanca.recibir(envio4);
        } catch (IllegalStateException e) {
            System.out.println("6) " + e.getMessage());
        }

        // 7) límite de 3 paquetes por envío
        Envio envioLleno = new EnvioEstandar(cliente2, "Salta",
            new Paquete("PKG-006", 1.0),
            new Paquete("PKG-007", 1.0),
            new Paquete("PKG-008", 1.0));
        try {
            envioLleno.agregarPaquete(new Paquete("PKG-009", 1.0)); // 4to: debe fallar
        } catch (IllegalStateException e) {
            System.out.println("7) " + e.getMessage());
        }

        // 8) agregar paquetes a un envío que ya fue procesado por una sucursal
        viedma.despachar(envio4);
        try {
            envio4.agregarPaquete(new Paquete("PKG-010", 1.0));
        } catch (IllegalStateException e) {
            System.out.println("8) " + e.getMessage());
        }

        // 9) consultar el último movimiento de un envío sin recorrido
        try {
            envioLleno.getUltimoMovimiento();
        } catch (NoSuchElementException e) {
            System.out.println("9) " + e.getMessage());
        }

        // ── Estado final del sistema ────────────────────────────────────────
        System.out.println("\n-- Estado final de los envíos --");
        for (Envio envio : gestor.getEnvios()) {
            System.out.println("  " + envio);
        }
        System.out.println("Envíos en proceso en " + viedma + ": " + viedma.getEnviosEnProceso());
    }

    // Presentación de una ficha: App recupera cada dato con su getter y lo
    // formatea. No conoce ni manipula el interior del envío.
    private static void mostrarFicha(Envio envio) {
        System.out.printf(
            "Envío #%d | Cliente: %-18s | Tipo: %-18s | Destino: %-16s | Origen: %-14s | Paquetes: %d | Estado: %-14s | Costo: $%.2f%n",
            envio.getId(),
            envio.getCliente().getNombre(),
            envio.getClass().getSimpleName(),
            envio.getDestino(),
            envio.getSucursalOrigen() == null ? "(sin procesar)" : envio.getSucursalOrigen().getNombre(),
            envio.getPaquetes().size(),
            envio.getEstado(),
            envio.calcularCosto());
    }
}

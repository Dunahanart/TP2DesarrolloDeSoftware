import java.util.NoSuchElementException;

/* App es sólo la CAPA DE PRESENTACIÓN y de orquestación de la demostración:
crea los objetos, les manda mensajes y MUESTRA los datos que los objetos le
devuelven. Nunca recorre ni modifica el interior de otros objetos.

TP4: el main crea la ÚNICA instancia de RegistroCentral (la base de datos en
memoria) y le pide que guarde y encuentre los objetos del sistema. Las
acciones del negocio (recibir, despachar, entregar) siguen en sus clases:
el registro sólo encuentra el envío, la acción la hace el propio envío. */
public class App {

    public static void main(String[] args) {

        System.out.println("--- SISTEMA DE GESTIÓN DE ENVÍOS Y DISTRIBUCIÓN ---\n");

        // Base de datos en memoria del sistema: UNA sola instancia, creada acá.
        RegistroCentral registro = new RegistroCentral();

        // ── Clientes (con DNI) y sucursales ────────────────────────────────
        System.out.println("-- Clientes --");
        Cliente cliente1 = new Cliente("CLI-001", "María Gómez", 28456789);
        Cliente cliente2 = new Cliente("CLI-002", "Distribuidora Sur", 30123456);
        registro.registrarCliente(cliente1);
        registro.registrarCliente(cliente2);
        System.out.println(cliente1);
        System.out.println(cliente2);

        System.out.println("\n-- Sucursales --");
        Sucursal viedma = new Sucursal("SUC-01", "Viedma");
        Sucursal bahiaBlanca = new Sucursal("SUC-02", "Bahía Blanca");
        Sucursal buenosAires = new Sucursal("SUC-03", "Buenos Aires");
        registro.registrarSucursal(viedma);
        registro.registrarSucursal(bahiaBlanca);
        registro.registrarSucursal(buenosAires);
        System.out.println(registro.listarSucursales());

        // ── Alta de envíos: el registro central los guarda (Parte 1) ───────
        System.out.println("\n-- Alta de envíos --");
        Envio envio1 = new EnvioEstandar(cliente1, "Neuquén",
            new Paquete("PKG-001", 2.5),
            new Paquete("PKG-002", "Libros de texto", 1.8));
        Envio envio2 = new EnvioExpress(cliente1, "Buenos Aires",
            new Paquete("PKG-003", 0.9));
        Envio envio3 = new EnvioInternacional(cliente2, "Santiago", "Chile",
            new Paquete("PKG-004", "Electrónica frágil", 3.2));
        registro.registrarEnvio(envio1);
        registro.registrarEnvio(envio2);
        registro.registrarEnvio(envio3);

        // ── Parte 2: el registro encuentra envíos por código (HashMap) ─────
        System.out.println("\n-- Búsqueda por código de seguimiento (HashMap) --");
        System.out.println("Código " + envio1.getId() + " -> " + registro.buscarEnvio(envio1.getId()));
        System.out.println("Código " + envio3.getId() + " -> " + registro.buscarEnvio(envio3.getId()));

        // ── Parte 4.a: búsqueda secuencial de clientes por DNI ─────────────
        System.out.println("\n-- Búsqueda secuencial por DNI --");
        System.out.println("DNI " + cliente1.getDni() + " -> "
            + registro.buscarClienteSecuencial(cliente1.getDni()));
        System.out.println("DNI " + cliente2.getDni() + " -> "
            + registro.buscarClienteSecuencial(cliente2.getDni()));

        // ── Parte 3: recorrido con cola de despacho (FIFO) ─────────────────
        // Colaboración: App --> Sucursal --> Envio --> Paquete, pero ahora los
        // envíos esperan su turno en la cola de la sucursal y salen por orden
        // de llegada, hasta la capacidad del camión.
        System.out.println("\n-- Recorrido del envío #1 (cola y varias sucursales) --");
        viedma.recibirEnvio(envio1);
        viedma.recibirEnvio(envio2);        // entra después: saldrá después
        viedma.recibirEnvio(envio3);
        System.out.println("Cola en Viedma (orden de llegada): " + viedma.getEnviosEnProceso());

        // Camión con capacidad 2: sólo salen los dos primeros (FIFO).
        int salidaViedma = viedma.despachar(2);
        System.out.println("Despachados desde Viedma: " + salidaViedma
            + " | quedan esperando: " + viedma.getEnviosEnProceso());

        bahiaBlanca.recibirEnvio(envio1);    // sucursal intermedia: sigue EN_VIAJE
        bahiaBlanca.despachar(1);
        buenosAires.recibirEnvio(envio1);
        buenosAires.despachar(1);
        envio1.entregar();                   // llega a su destino final

        System.out.println("\n-- Recorrido del envío #2 (express, una sola sucursal) --");
        buenosAires.recibirEnvio(envio2);
        buenosAires.despachar(1);
        envio2.entregar();

        System.out.println("\n-- Recorrido del envío #3 (internacional, queda en tránsito) --");
        System.out.println("Sale de Viedma: " + viedma.despachar(1) + " envío despachado.");
        bahiaBlanca.recibirEnvio(envio3);    // sucursal intermedia: sigue EN_VIAJE
        bahiaBlanca.despachar(1);

        // ── Consultas: los objetos devuelven datos, App los muestra ────────
        //     App --> envio.getUltimoMovimiento() --> Movimiento --> System.out
        System.out.println("\n-- Seguimiento del envío #1 --");
        System.out.println("Historial completo:");
        for (Movimiento movimiento : envio1.getHistorial()) {
            System.out.println("  " + movimiento);
        }
        System.out.println("Sucursales por las que pasó: " + envio1.getRecorrido());
        System.out.println("Último movimiento: " + envio1.getUltimoMovimiento());

        System.out.println("\n-- Seguimiento del envío #3 (aún en viaje) --");
        System.out.println("Historial completo:");
        for (Movimiento movimiento : envio3.getHistorial()) {
            System.out.println("  " + movimiento);
        }
        System.out.println("Sucursales por las que pasó: " + envio3.getRecorrido());
        System.out.println("Último movimiento: " + envio3.getUltimoMovimiento());

        // ── Cada cliente conoce sus propios envíos ─────────────────────────
        System.out.println("\nEnvíos de " + cliente1 + ":");
        for (Envio envio : cliente1.getEnvios()) {
            System.out.println("  " + envio);
        }
        System.out.println("Envíos de " + cliente2 + ":");
        for (Envio envio : cliente2.getEnvios()) {
            System.out.println("  " + envio);
        }

        // ── Información general del sistema (consultas del registro) ───────
        System.out.println("\n-- Resumen general del sistema --");
        System.out.println("  Envíos registrados: " + registro.cantidadEnvios());
        System.out.printf("  Costo total:        $%.2f%n", registro.costoTotal());
        System.out.printf("  Costo promedio:     $%.2f%n", registro.costoPromedio());

        System.out.println("Envíos registrados:");
        for (Envio envio : registro.listarEnvios()) {
            System.out.println("  " + envio);
        }
        System.out.println();
        mostrarFicha(registro.buscarEnvio(envio2.getId()));

        // ── Operaciones inválidas, todas impedidas ─────────────────────────
        System.out.println("\n-- Pruebas de validación (errores esperados) --");

        // 1) registrar un envío con un código que ya existe (Parte 2)
        try {
            registro.registrarEnvio(envio1);
        } catch (IllegalStateException e) {
            System.out.println("1) " + e.getMessage());
        }

        // 2) registrar un cliente con un DNI que ya existe (Parte 1)
        try {
            registro.registrarCliente(new Cliente("CLI-003", "Clon", cliente1.getDni()));
        } catch (IllegalStateException e) {
            System.out.println("2) " + e.getMessage());
        }

        // 3) buscar un código de seguimiento inexistente (Parte 2)
        try {
            registro.buscarEnvio(999);
        } catch (NoSuchElementException e) {
            System.out.println("3) " + e.getMessage());
        }

        // 4) buscar un DNI inexistente (Parte 4.a)
        try {
            registro.buscarClienteSecuencial(99999999);
        } catch (NoSuchElementException e) {
            System.out.println("4) " + e.getMessage());
        }

        // 5) crear un envío sin paquetes: la regla vive en Envio, no en el
        //    registro (el TP4 le prohíbe validar reglas del negocio)
        try {
            new EnvioEstandar(cliente2, "Mendoza");
        } catch (IllegalStateException e) {
            System.out.println("5) " + e.getMessage());
        }

        // 6) recibir dos veces el mismo envío en la misma sucursal
        Envio envio4 = new EnvioEstandar(cliente2, "Bariloche",
            new Paquete("PKG-005", 1.0));
        registro.registrarEnvio(envio4);
        viedma.recibirEnvio(envio4);
        try {
            viedma.recibirEnvio(envio4);
        } catch (IllegalStateException e) {
            System.out.println("6) " + e.getMessage());
        }

        // 7) mover un envío a otra sucursal antes de que sea despachado
        try {
            bahiaBlanca.recibirEnvio(envio4);
        } catch (IllegalStateException e) {
            System.out.println("7) " + e.getMessage());
        }

        // 8) entregar un envío que no fue despachado
        try {
            envio4.entregar();
        } catch (IllegalStateException e) {
            System.out.println("8) " + e.getMessage());
        }

        // 9) despachar con la cola vacía (Parte 3)
        try {
            bahiaBlanca.despachar(3);
        } catch (IllegalStateException e) {
            System.out.println("9) " + e.getMessage());
        }

        // 10) despachar una cantidad inválida
        try {
            viedma.despachar(0);
        } catch (IllegalArgumentException e) {
            System.out.println("10) " + e.getMessage());
        }

        // 11) límite de 3 paquetes por envío
        Envio envioLleno = new EnvioEstandar(cliente2, "Salta",
            new Paquete("PKG-006", 1.0),
            new Paquete("PKG-007", 1.0),
            new Paquete("PKG-008", 1.0));
        try {
            envioLleno.agregarPaquete(new Paquete("PKG-009", 1.0)); // 4to: debe fallar
        } catch (IllegalStateException e) {
            System.out.println("11) " + e.getMessage());
        }

        // 12) agregar paquetes a un envío que ya fue despachado
        //     (el despacho saca al envío #4 de la cola de Viedma)
        viedma.despachar(1);
        try {
            envio4.agregarPaquete(new Paquete("PKG-010", 1.0));
        } catch (IllegalStateException e) {
            System.out.println("12) " + e.getMessage());
        }

        // 13) consultar el último movimiento de un envío sin recorrido
        try {
            envioLleno.getUltimoMovimiento();
        } catch (NoSuchElementException e) {
            System.out.println("13) " + e.getMessage());
        }

        // ── Estado final del sistema ───────────────────────────────────────
        System.out.println("\n-- Estado final de los envíos --");
        for (Envio envio : registro.listarEnvios()) {
            System.out.println("  " + envio);
        }
        System.out.println("Envíos esperando despacho en " + viedma + ": "
            + viedma.getEnviosEnProceso());
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

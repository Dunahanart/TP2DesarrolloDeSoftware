# Diagrama de Clases — Sistema de Gestión de Envíos (TP2)

> Los *getters* simples se omiten en el diagrama para mantenerlo legible;
> todos existen en el código fuente.

```mermaid
classDiagram
    direction TB

    class EstadoPaquete {
        <<enumeration>>
        RECIBIDO
        EN_PREPARACION
        EN_VIAJE
        ENTREGADO
    }

    class EstadoEnvio {
        <<enumeration>>
        REGISTRADO
        EN_PREPARACION
        EN_VIAJE
        ENTREGADO
    }

    class TipoMovimiento {
        <<enumeration>>
        RECIBIDO
        DESPACHADO
        ENTREGADO
    }

    class Paquete {
        -String id
        -String descripcion
        -double pesoKg
        -EstadoPaquete estado
        +Paquete(id, pesoKg)
        +Paquete(id, descripcion, pesoKg)
        ~recibir() void
        ~iniciarViaje() void
        ~entregar() void
        -validarPeso(peso) void
        -transicionValida(nuevo) boolean
        -avanzarEstado(nuevo) void
    }

    class Cliente {
        -String id
        -String nombre
        -List~Envio~ envios
        +Cliente(id, nombre)
        ~registrarEnvio(envio) void
        +getEnvios() List~Envio~
    }

    class Sucursal {
        -String id
        -String nombre
        -List~Envio~ enviosEnProceso
        +Sucursal(id, nombre)
        +recibir(envio) void
        +despachar(envio) void
        +getEnviosEnProceso() List~Envio~
    }

    class Movimiento {
        -LocalDateTime fecha
        -TipoMovimiento tipo
        -Sucursal sucursal
        ~Movimiento(tipo, sucursal)
        +getFecha() LocalDateTime
        +getTipo() TipoMovimiento
        +getSucursal() Sucursal
    }

    class Envio {
        <<abstract>>
        -int LIMITE_PAQUETES$ = 3
        -int contadorId$ = 1
        -int id
        -Cliente cliente
        -String destino
        -List~Paquete~ paquetes
        -List~Movimiento~ historial
        -Sucursal sucursalOrigen
        -Sucursal sucursalActual
        -EstadoEnvio estado
        +Envio(cliente, destino, paquetes...)
        +agregarPaquete(paquete) void
        ~recibirEn(sucursal) void
        ~despacharDesde(sucursal) void
        +entregar() void
        +getHistorial() List~Movimiento~
        +getRecorrido() List~Sucursal~
        +getUltimoMovimiento() Movimiento
        +calcularCosto() double
        -todaviaNoSalio() boolean
        -enPreparacion() boolean
        -estaEnViaje() boolean
        -estaFinalizado() boolean
        -hayLugarParaPaquetes() boolean
        -transicionValida(nuevo) boolean
        -avanzarA(nuevo) void
        -registrarMovimiento(tipo, sucursal) void
        -validarSucursal(sucursal) void
        #calcularCostoBase() double*
        #aplicarRecargo(costoBase) double
        #calcularPesoTotal() double
    }

    class GestorEnvios {
        -List~Envio~ envios
        +registrar(envio) void
        +buscarPorId(id) Envio
        +enviosDe(cliente) List~Envio~
        +cantidadEnvios() int
        +costoTotal() double
        +costoPromedio() double
    }

    class EnvioEstandar {
        -TARIFA_POR_KG$ = 150.0
        +EnvioEstandar(cliente, destino, paquetes...)
        #calcularCostoBase() double
    }

    class EnvioExpress {
        -TARIFA_POR_KG$ = 300.0
        -RECARGO_URGENCIA$ = 500.0
        +EnvioExpress(cliente, destino, paquetes...)
        #calcularCostoBase() double
        #aplicarRecargo(costoBase) double
    }

    class EnvioInternacional {
        -TARIFA_POR_KG$ = 500.0
        -PORCENTAJE_ADUANA$ = 0.20
        -paisDestino String
        +EnvioInternacional(cliente, ciudad, pais, paquetes...)
        #calcularCostoBase() double
        #aplicarRecargo(costoBase) double
        +getPaisDestino() String
    }

    class App {
        <<main>>
        +main(args String[]) void
    }

    Envio <|-- EnvioEstandar : hereda
    Envio <|-- EnvioExpress : hereda
    Envio <|-- EnvioInternacional : hereda

    Envio "1" o-- "*" Paquete : contiene (máx. 3)
    Envio "1" o-- "*" Movimiento : historial
    Cliente "1" o-- "*" Envio : solicita
    Sucursal "1" o-- "*" Envio : tiene en proceso

    Envio --> Cliente : pertenece a
    Envio --> EstadoEnvio : su etapa actual
    Envio --> Sucursal : origen / actual (0..1)
    Paquete --> EstadoPaquete : su estado actual
    Movimiento --> TipoMovimiento : qué ocurrió
    Movimiento --> Sucursal : dónde ocurrió (0..1)

    App ..> Cliente : crea
    App ..> Sucursal : crea
    App ..> GestorEnvios : usa
    GestorEnvios ..> Envio : registra y consulta
```

## Leyenda

| Símbolo | Significado |
|---------|-------------|
| `-` | Atributo / método `private` |
| `+` | Método `public` |
| `#` | Método `protected` |
| `~` | Método **package-private** (visible solo dentro del sistema) |
| `*` al final | Método **abstracto** (debe implementarse en las subclases) |
| `$` | Miembro `static` (compartido por todas las instancias) |
| `<<enumeration>>` | Enum de valores fijos |
| `<<abstract>>` | Clase que no se puede instanciar directamente |
| `<\|--` | Herencia (una subclase **es un** tipo de la superclase) |
| `o--` | Asociación (contención) |
| `..\>` | Dependencia (una clase **crea o usa** a otra) |

## Ciclo de vida del paquete

```
RECIBIDO ──recibir()──▶ EN_PREPARACION ──iniciarViaje()──▶ EN_VIAJE ──entregar()──▶ ENTREGADO
```

Cada etapa solo avanza **de a un paso**; saltarse una genera `IllegalStateException`.
Esos tres métodos son **package-private**: los invoca únicamente su `Envio`
(los paquetes no cambian de estado porque App se lo pida).

## Ciclo de vida del envío

```
REGISTRADO ──recibirEn()──▶ EN_PREPARACION ──despacharDesde()──▶ EN_VIAJE ──entregar()──▶ ENTREGADO
```

- `Sucursal.recibir(envio)` → `Envio.recibirEn(sucursal)` → `Paquete.recibir()`
- `Sucursal.despachar(envio)` → `Envio.despacharDesde(sucursal)` → `Paquete.iniciarViaje()`
- `Envio.entregar()` (el destino final no siempre es una sucursal)
- Una sucursal intermedia **recibe/despacha un envío EN_VIAJE sin cambiar su
  etapa**: sólo registra el paso en el historial (la máquina de estados no
  retrocede).

## Historial de movimientos

`Envio` es el dueño de su `List<Movimiento>`: él crea los registros
(constructor `Movimiento` package-private, campos `final`) y responde las
consultas **devolviendo datos** (TP2.1): `getHistorial()`, `getRecorrido()`,
`getUltimoMovimiento()`. Quien los muestre (App) sólo presenta lo recibido.

## Reglas del negocio con nombre propio (TP2.1)

Las comprobaciones reutilizables viven como métodos **privados** del objeto que
las usa — por ejemplo en `Envio`: `todaviaNoSalio()`, `enPreparacion()`,
`estaEnViaje()`, `estaFinalizado()`, `hayLugarParaPaquetes()`. El avance de
estados no usa cadenas de `if/else` ni `switch`: lo resuelven
`transicionValida(nuevo)` + `avanzarA(nuevo)`, igual que en `Paquete`.

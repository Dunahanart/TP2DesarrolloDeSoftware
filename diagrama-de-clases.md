# Diagrama de Clases — Sistema de Gestión de Envíos

> Los *getters* simples se omiten en el diagrama para mantenerlo legible;
> todos existen en el código fuente.

```mermaid
classDiagram
    direction TB

    class EstadoPaquete {
        <<enumeration>>
        RECIBIDO
        EN_PREPARACION
        EN_DISTRIBUCION
        ENTREGADO
    }

    class Paquete {
        -String id
        -String descripcion
        -double pesoKg
        -EstadoPaquete estado
        +Paquete(id, pesoKg)
        +Paquete(id, descripcion, pesoKg)
        +iniciarPreparacion()
        +salirADistribucion()
        +marcarEntregado()
        -validarPeso(peso) void
        -transicionValida(nuevo) boolean
        -avanzarEstado(nuevo) void
    }

    class Envio {
        <<abstract>>
        -int id$
        -String destino
        -List~Paquete~ paquetes
        -boolean iniciado
        -boolean distribuido
        -boolean finalizado
        +Envio(destino)
        +Envio(destino, paquete)
        +agregarPaquete(paquete) void
        +iniciar() void
        +salirADistribucion() void
        +finalizar() void
        +calcularCosto() double
        +mostrarResumen() void
        #calcularCostoBase() double*
        #aplicarRecargo(costoBase) double
        #calcularPesoTotal() double
    }

    class EnvioEstandar {
        -TARIFA_POR_KG$ = 150.0
        +EnvioEstandar(destino)
        +EnvioEstandar(destino, paquete)
        #calcularCostoBase() double
    }

    class EnvioExpress {
        -TARIFA_POR_KG$ = 300.0
        -RECARGO_URGENCIA$ = 500.0
        +EnvioExpress(destino)
        +EnvioExpress(destino, paquete)
        #calcularCostoBase() double
        #aplicarRecargo(costoBase) double
    }

    class EnvioInternacional {
        -TARIFA_POR_KG$ = 500.0
        -PORCENTAJE_ADUANA$ = 0.20
        -paisDestino String
        +EnvioInternacional(ciudad, pais)
        +EnvioInternacional(ciudad, pais, paquete)
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
    Paquete --> EstadoPaquete : su estado actual

    App ..> EnvioEstandar : crea
    App ..> EnvioExpress : crea
    App ..> EnvioInternacional : crea
    App ..> Paquete : crea
```

## Leyenda

| Símbolo | Significado |
|---------|-------------|
| `-` | Atributo / método `private` |
| `+` | Método `public` |
| `#` | Método `protected` |
| `*` al final | Método **abstracto** (debe implementarse en las subclases) |
| `$` | Miembro `static` (compartido por todas las instancias) |
| `<<enumeration>>` | Enum de valores fijos |
| `<<abstract>>` | Clase que no se puede instanciar directamente |
| `<\|--` | Herencia (una subclase **es un** tipo de la superclase) |
| `o--` | Asociación (un envío **contiene** paquetes) |
| `..\>` | Dependencia (una clase **crea o usa** a otra) |

## Ciclo de vida del paquete

```
RECIBIDO ──iniciarPreparacion()──▶ EN_PREPARACION ──salirADistribucion()──▶ EN_DISTRIBUCION ──marcarEntregado()──▶ ENTREGADO
```

Cada etapa solo avanza **de a un paso**; saltarse una genera `IllegalStateException`.
En `Envio` cada etapa tiene su propio método: `iniciar()` · `salirADistribucion()` · `finalizar()`.

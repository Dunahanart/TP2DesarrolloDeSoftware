# Diagrama de Actividad — TP2

> Flujo completo del **ciclo de vida de un envío**: desde su creación hasta la
> entrega, con cada regla de negocio como decisión.

## Definición

El **diagrama de actividad** es un diagrama de UML que representa el **flujo de
trabajo** (workflow) de un proceso: qué acciones se realizan, en qué orden, qué
decisiones se toman, qué partes se repiten y dónde termina el proceso.

Se enfoca en **qué se hace** para lograr una operación:

- no dice **qué objetos** participan ni en qué orden reciben los mensajes
  (eso lo muestra el **diagrama de secuencia**);
- ni describe **de qué está compuesto** el sistema (eso es el **diagrama de
  clases**);
- y no entra en el detalle interno del código (eso es el código fuente).

Se usa cuando interesa seguir un proceso **de principio a fin**, con
alternativas y ciclos, de manera legible para cualquier interesado.

## Notación

| Símbolo | Significado | Uso en este trabajo |
|---------|-------------|---------------------|
| `((Inicio))` | **Nodo inicial**: comienza la actividad | Se crea el envío con su cliente |
| Rectángulo `[…]` | **Acción / actividad**: algo que se hace | registrar, recibir, despachar, entregar |
| Rombo `{…}` | **Decisión**: pregunta con ramas alternativas | ¿Tiene paquetes?, ¿puede recibirse?… |
| Flecha `-->` | **Flujo de control**: indica el orden | De una acción a la siguiente o a una decisión |
| Flecha que vuelve atrás | **Ciclo / iteración**: la acción se repite | ¿Pasa por otra sucursal? → sí → se repite |
| `((Fin))` | **Nodo final**: termina el proceso | Envío entregado / operación impedida |

Cada acción del diagrama corresponde a **una operación real del modelo** (una
llamada entre objetos: `Sucursal → Envio → Paquete`), y cada rombo corresponde
a una **validación** del TP2.1: si la respuesta es "no", la operación termina
en una excepción `IllegalStateException`.

## Diagrama

```mermaid
flowchart TD
    S((Inicio))
    A[Se crea el Envío con su cliente y paquetes<br/>estado: REGISTRADO]
    B{¿Puede registrarse?<br/>tiene paquetes}
    C[GestorEnvios.registrar el envío]
    D{¿Puede recibirse?<br/>etapa REGISTRADO o EN_VIAJE}
    E[Sucursal.recibir a Envio.recibirEn<br/>Paquete.recibir + Movimiento RECIBIDO]
    F{¿Puede despacharse?<br/>etapa EN_PREPARACION}
    G[Sucursal.despachar a Envio.despacharDesde<br/>Paquete.iniciarViaje + Movimiento DESPACHADO]
    H{¿Pasa por otra<br/>sucursal?}
    I[La sucursal intermedia lo recibe y lo despacha<br/>sigue EN_VIAJE: sólo registra movimientos]
    J{¿Puede entregarse?<br/>etapa EN_VIAJE}
    K[Envio.entregar en el destino final<br/>Paquete.entregar + Movimiento ENTREGADO]
    X[Operación impedida<br/>IllegalStateException]
    Z((Fin))

    S --> A
    A --> B
    B -- No --> X
    B -- Sí --> C
    C --> D
    D -- No --> X
    D -- Sí --> E
    E --> F
    F -- No --> X
    F -- Sí --> G
    G --> H
    H -- Sí --> I
    I --> H
    H -- No --> J
    J -- No --> X
    J -- Sí --> K
    K --> Z

    style S fill:#111,stroke:#111,color:#fff
    style Z fill:#fff,stroke:#111,stroke-width:3px,color:#111
    style X fill:#fdecec,stroke:#b00000,stroke-width:2.5px,color:#800000
```

## Correspondencia con el código

| Actividad del diagrama | Operación en el código |
|------------------------|------------------------|
| Se crea el envío | `new EnvioEstandar(cliente, destino, paquetes...)` |
| ¿Puede registrarse? | `GestorEnvios.registrar()` — rechaza envíos sin paquetes |
| ¿Puede recibirse? | `Envio.recibirEn()` — sólo REGISTRADO o EN_VIAJE (tránsito) |
| Recibir | `Sucursal.recibir()` → `Envio.recibirEn()` → `Paquete.recibir()` |
| ¿Puede despacharse? | `Envio.despacharDesde()` — sólo EN_PREPARACION o EN_VIAJE |
| Despachar | `Sucursal.despachar()` → `Envio.despacharDesde()` → `Paquete.iniciarViaje()` |
| Ciclo de sucursales | sucursales intermedias: mismo flujo, el estado sigue EN_VIAJE |
| ¿Puede entregarse? | `Envio.entregar()` — exige etapa EN_VIAJE |
| Entregar | `Envio.entregar()` → `Paquete.entregar()` + `Movimiento(ENTREGADO)` |
| Operación impedida | excepción `IllegalStateException` / `NoSuchElementException` |

# Diagramas de Secuencia — TP2.1

> Dos funciones principales del sistema, según el enunciado 2.1: muestran el
> objeto que inicia la operación, los objetos que participan, los mensajes que
> se envían y el orden en que ocurren.

## 1. Recorrido de un envío (recibir → despachar → entregar)

La colaboración central del modelo: **App → Sucursal → Envio → Paquete**, con
el registro de `Movimiento` a cargo del envío.

```mermaid
sequenceDiagram
    autonumber
    participant App
    participant S as Sucursal Viedma
    participant E as Envío 1
    participant P as Paquetes del envío
    participant M as Movimiento

    Note over App,S: La sucursal recibe el envío
    App->>S: recibir(envio)
    S->>S: valida que el envío no esté ya procesado
    S->>E: recibirEn(sucursal)
    E->>E: todaviaNoSalio(): etapa REGISTRADO
    E->>E: avanzarA(EN_PREPARACION)
    loop por cada paquete del envío
        E->>P: recibir()
        P-->>E: RECIBIDO → EN_PREPARACION
    end
    E->>M: new Movimiento(RECIBIDO, sucursal)
    M-->>E: movimiento guardado en el historial
    E-->>S: ok
    S->>S: agrega el envío a sus envíos en proceso

    Note over App,S: La sucursal despacha el envío
    App->>S: despachar(envio)
    S->>E: despacharDesde(sucursal)
    E->>E: enPreparacion(): etapa EN_PREPARACION
    E->>E: avanzarA(EN_VIAJE)
    loop por cada paquete del envío
        E->>P: iniciarViaje()
        P-->>E: EN_PREPARACION → EN_VIAJE
    end
    E->>M: new Movimiento(DESPACHADO, sucursal)
    M-->>E: movimiento guardado en el historial
    E-->>S: ok
    S->>S: quita el envío de sus envíos en proceso

    Note over App,E: La entrega ocurre en el destino final (no es una sucursal)
    App->>E: entregar()
    E->>E: estaEnViaje(): etapa EN_VIAJE
    E->>E: avanzarA(ENTREGADO)
    loop por cada paquete del envío
        E->>P: entregar()
        P-->>E: EN_VIAJE → ENTREGADO
    end
    E->>M: new Movimiento(ENTREGADO, sin sucursal)
    M-->>E: movimiento guardado en el historial
```

## 2. Consulta del seguimiento de un envío

Las consultas **devuelven datos** (TP2.1); quien los muestra es App.

```mermaid
sequenceDiagram
    autonumber
    participant App
    participant E as Envío 1
    participant H as Historial de Movimientos

    Note over App,H: Consultar el historial completo
    App->>E: getHistorial()
    E->>H: copia inmutable de sus movimientos
    H-->>E: movimientos
    E-->>App: List de Movimiento
    App->>App: muestra cada Movimiento

    Note over App,H: Informar por qué sucursales pasó
    App->>E: getRecorrido()
    E->>H: recorre sus movimientos
    H-->>E: movimientos
    E-->>App: List de Sucursal (sin repetir, en orden)
    App->>App: muestra las sucursales

    Note over App,H: Obtener el último movimiento
    App->>E: getUltimoMovimiento()
    E->>H: toma el último de la lista
    H-->>E: movimiento
    E-->>App: Movimiento
    App->>App: muestra el movimiento

    Note over E: Sin movimientos: NoSuchElementException
```

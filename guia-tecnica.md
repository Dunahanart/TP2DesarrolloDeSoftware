# Guía técnica — Arquitectura y código del sistema

> Esta guía explica **todo el código tal como está hoy**, sin referencias a las
> instancias de evaluación. Primero la **arquitectura** (de qué se compone el
> sistema y por qué está organizado así), luego **clase por clase** (cada
> atributo y cada método), y al final **cuestionarios de autoevaluación** con
> sus respuestas.

---

## 1. El sistema en una página

Es un sistema de logística que modela **envíos de paquetes**: un **cliente**
solicita un **envío** (de 1 a 3 **paquetes**), que recorre **sucursales**
(recibimiento, preparación, despacho) hasta llegar a su destino y **entregarse**.
Cada acontecimiento del recorrido queda registrado como un **movimiento**, y
todo lo que existe en el sistema queda guardado en un **registro central** en
memoria.

```
                        ┌────────────┐
                        │    App     │  ← capa de presentación
                        └─────┬──────┘     (sólo crea, manda mensajes y muestra)
              ┌───────────────┼─────────────────────┐
              ▼               ▼                     ▼
     ┌────────────────┐ ┌───────────┐       ┌──────────────┐
     │ RegistroCentral│ │  Sucursal │       │   Cliente    │
     │ (en memoria)   │ │ (cola de  │       │ (dueño de    │
     │ guarda y busca │ │  despacho)│       │  sus envíos) │
     └───────┬────────┘ └─────┬─────┘       └──────┬───────┘
             │ guardas        │ recibe/despacha     │ posee
             ▼                ▼                     ▼
                      ┌─────────────────────────────────┐
                      │              Envio              │
                      │  coordina paquetes e historial, │
                      │  calcula su costo, protege su   │
                      │  estado y registra movimientos  │
                      └──────────┬───────────┬──────────┘
                                 ▼           ▼
                           ┌──────────┐ ┌────────────┐
                           │ Paquete  │ │ Movimiento │
                           └──────────┘ └────────────┘
```

**Idea central del diseño**: cada objeto **sólo hace lo que le corresponde** y
los demás le piden *comportamiento* (`envio.entregar()`), nunca le sacan datos
internos para manipularlos desde afuera (`envio.getPaquetes().get(0).setEstado(...)`).

---

## 2. Arquitectura

### 2.1 Capas

| Capa | Clases | Qué hace |
|---|---|---|
| **Presentación** | `App` | Crea los objetos de la demo, les manda mensajes y muestra en consola lo que le devuelven. No contiene reglas de negocio ni toca internos. |
| **Almacén** | `RegistroCentral` | Repositorio en memoria: guarda clientes, sucursales y envíos; los busca y los lista. Una sola instancia, creada en el `main`. |
| **Dominio** | `Cliente`, `Sucursal`, `Envio` + subclases, `Paquete`, `Movimiento` | Toda la lógica del negocio: quién tiene qué, quién procesa qué, cómo cambian los estados, cómo se calculan los costos. |
| **Catálogos de valores** | `EstadoPaquete`, `EstadoEnvio`, `TipoMovimiento` | Enumeraciones: conjuntos fijos de valores que hacen imposibles estados o eventos inválidos. |

### 2.2 Reglas de dependencia

- `App` **conoce** al resto, pero el resto **nunca** conoce a `App`.
- `RegistroCentral` **guarda** objetos del dominio, no los modifica.
- `Sucursal` y `Cliente` hablan con `Envio`; `Envio` habla con `Paquete` y
  `Movimiento`. La cadena nunca se invierte.
- Todas las clases están en el **paquete por defecto** de Java (no hay
  declaraciones `package`). Se compila con `javac -encoding UTF-8 -d classes src\*.java`
  y se ejecuta con `java -cp classes App`.
- **Java SE puro**: sin frameworks. Sólo se usan `java.util`
  (`List`, `ArrayList`, `Map`, `HashMap`, `Queue`, `ArrayDeque`,
  `NoSuchElementException`) y `java.time` (`LocalDateTime`, `DateTimeFormatter`).

### 2.3 Principios y patrones, y dónde están

| Principio / patró​n | Dónde está en el código |
|---|---|
| **Encapsulamiento** | Atributos `private` en todas las clases; sin setters de estado (`Paquete` no tiene `setEstado`) |
| **Tell, don't ask** | `envio.entregar()` en vez de sacar paquetes desde App para cambiarles el estado |
| **Information Expert** | `Envio` calcula su costo (conoce sus paquetes) y responde por su historial (es el dueño de los movimientos) |
| **Creator** | `Envio` crea sus `Movimiento`: su constructor es package-private para que sólo él los fabrique |
| **Template Method** | `Envio.calcularCosto()` combina `calcularCostoBase()` (abstracta) + `aplicarRecargo()` (default 0); cada subclase resuelve distinto |
| **Patrón Repositorio** | `RegistroCentral` centraliza el acceso a los datos: registrar, buscar, listar — y nada más |
| **Copia defensiva** | Todas las consultas de colecciones devuelven `List.copyOf(...)`: se puede consultar, no modificar |
| **Objeto de valor inmutable** | `Movimiento`: clase `final`, campos `final`, sin setters. La historia no se reescribe |

### 2.4 Las dos máquinas de estado

`Paquete` y `Envio` tienen estados que **sólo avanzan de a un paso**:

```
Paquete:  RECIBIDO ──→ EN_PREPARACION ──→ EN_VIAJE ──→ ENTREGADO

Envio:    REGISTRADO ──→ EN_PREPARACION ──→ EN_VIAJE ──→ ENTREGADO
          (alta en el     (una sucursal       (fue despachado,   (llegó al
           sistema)        lo recibió)         en tránsito)       destino)
```

- La regla está implementada en `transicionValida()`:
  `nuevoEstado.ordinal() == estado.ordinal() + 1`. Si no avanza exactamente
  un paso, lanza `IllegalStateException`.
- Ese método es **privado**: los demás objetos sólo piden la acción
  (`avanzarA(...)`, `paquete.recibir()`), no saben cómo se decide.
- En `Envio`, una sucursal **intermedia** (el envío ya está `EN_VIAJE`) no
  cambia la etapa: sólo registra un movimiento en el historial.

### 2.5 Estructuras de datos

| Dónde | Estructura | Por qué |
|---|---|---|
| `RegistroCentral.envios` | `HashMap<Integer, Envio>` | Clave = código de seguimiento: la búsqueda es directa, sin recorrer |
| `RegistroCentral.sucursales` | `HashMap<String, Sucursal>` | Clave = código de sucursal (id) |
| `RegistroCentral.clientes` | `ArrayList<Cliente>` | Lista: permite la búsqueda secuencial por DNI recorriéndola |
| `Sucursal.colaDeDespacho` | `Queue<Envio>` (`ArrayDeque`) | FIFO: entra al final (`offer`), sale el primero (`poll`) |
| `Cliente.envios` | `ArrayList<Envio>` | Un cliente puede tener muchos envíos |
| `Envio.paquetes` / `Envio.historial` | `ArrayList<...>` | Colecciones con orden (los paquetes se agregan; el historial es cronológico) |

### 2.6 Estrategia de errores: tres excepciones

| Excepción | Cuándo | Ejemplos en el código |
|---|---|---|
| `IllegalArgumentException` | El **argumento** está mal desde el inicio | peso ≤ 0, DNI ≤ 0, envío nulo, `despachar(0)` |
| `IllegalStateException` | El **objeto está en un estado** que no permite la operación | transición inválida, despachar con cola vacía, código de envío duplicado |
| `NoSuchElementException` | Se **busca algo que no existe** | `buscarEnvio(999)`, `buscarClienteSecuencial(dni inexistente)`, `getUltimoMovimiento()` sin recorrido |

Quien **detecta** el error lanza la excepción; quien **presenta** (App) la
captura con `try/catch` e imprime `e.getMessage()`. El modelo no decide cómo
mostrar errores: sólo los señala.

### 2.7 Flujo de ejecución de `App`

1. Crea la **única** instancia de `RegistroCentral`.
2. Crea clientes (con DNI) y sucursales; los **registra** en el registro.
3. Crea los envíos (los constructores los asocian a su cliente); los **registra**.
4. **Búsqueda** por código de seguimiento (HashMap) y por DNI (secuencial).
5. **Recorrido con cola**: tres envíos entran a la cola de Viedma;
   `despachar(2)` saca los dos primeros en orden de llegada; el resto sigue su
   ruta por sucursales hasta `entregar()` (o queda en tránsito).
6. **Seguimiento**: pide historial, recorrido y último movimiento a los envíos.
7. **Resumen**: totales y promedios desde el registro.
8. **13 pruebas de validación**: operaciones inválidas atrapadas con
   `try/catch` para mostrar que el sistema las impide.
9. **Estado final**: lista de todos los envíos y de los que esperan despacho.

---

## 3. Código explicado clase por clase

### 3.1 `EstadoPaquete`, `EstadoEnvio`, `TipoMovimiento` — enumeraciones

```
EstadoPaquete:   RECIBIDO → EN_PREPARACION → EN_VIAJE → ENTREGADO
EstadoEnvio:     REGISTRADO → EN_PREPARACION → EN_VIAJE → ENTREGADO
TipoMovimiento:  RECIBIDO | DESPACHADO | ENTREGADO
```

- Son `enum`: **tipos seguros**. No se puede escribir un estado inventado;
  `ordinal()` devuelve la posición, y eso es lo que usa la regla "avanza de a
  un paso".
- `TipoMovimiento` existe para que un movimiento sea **siempre coherente**: no
  hay forma de registrar "algo raro ocurrió".

### 3.2 `Paquete` — el objeto físico

| Atributo | Tipo | Por qué |
|---|---|---|
| `id` | `String final` | Identidad propia: no cambia |
| `descripcion` | `String final` | Información descriptiva |
| `pesoKg` | `double final` | Base del cálculo de costo |
| `estado` | `EstadoPaquete` | El único campo que cambia |

| Método | Visibilidad | Qué hace |
|---|---|---|
| `Paquete(id, pesoKg)` | público | Constructor **mínimo**: delega en el completo con `this(id, "Sin descripción", pesoKg)` |
| `Paquete(id, descripcion, pesoKg)` | público | Constructor **completo**: valida y asigna todo; nace `RECIBIDO` |
| `validarPeso(pesoKg)` | `private` | Si el peso es ≤ 0 lanza `IllegalArgumentException`. Interna: nadie más la usa |
| `transicionValida(nuevo)` | `private` | `nuevo.ordinal() == estado.ordinal() + 1` |
| `avanzarEstado(nuevo)` | `private` | Si la transición no es válida, `IllegalStateException`; si sí, cambia el estado |
| `recibir()` / `iniciarViaje()` / `entregar()` | package-private | Los **únicos** caminos para cambiar el estado. Son package-private a propósito: sólo su `Envio` puede invocarlos |
| getters + `toString()` | público | Sólo lectura; sin setters |

### 3.3 `Movimiento` — un acontecimiento inmutable

| Atributo | Tipo | Significado |
|---|---|---|
| `fecha` | `LocalDateTime final` | **Cuándo** pasó: se toma sola al crear el registro |
| `tipo` | `TipoMovimiento final` | **Qué** pasó (RECIBIDO / DESPACHADO / ENTREGADO) |
| `sucursal` | `Sucursal final` | **Dónde** pasó; `null` sólo si fue entregado en un destino que no es sucursal |

- Clase `final`, campos `final`, sin setters: **la historia no se reescribe**.
- El constructor es **package-private**: sólo quien administra el recorrido
  (`Envio`) puede fabricar movimientos.
- `toString()` formatea `dd/MM/yyyy HH:mm:ss | TIPO | en <sucursal>`.
- No tiene relación directa con el envío: la asociación es al revés — el envío
  guarda sus movimientos (uno a muchos).

### 3.4 `Envio` — el objeto central

**Atributos**

| Atributo | Tipo | Por qué |
|---|---|---|
| `LIMITE_PAQUETES` | `static final int = 3` | Regla del negocio: máximo 3 paquetes |
| `contadorId` | `static int` | Autoincremental: da el **código de seguimiento** único |
| `id` | `int final` | Código de seguimiento |
| `cliente` | `Cliente final` | Quién lo solicitó |
| `destino` | `String final` | Hacia dónde va |
| `paquetes` | `List<Paquete>` | Lo que transporta (agregado) |
| `historial` | `List<Movimiento>` | Su recorrido cronológico (agregado) |
| `sucursalOrigen` / `sucursalActual` | `Sucursal` | Dónde empezó / qué sucursal lo procesa |
| `estado` | `EstadoEnvio` | Etapa actual (protegido por su máquina de estados) |

**Constructor** `Envio(Cliente, String destino, Paquete... paquetes)`:

1. Valida cliente y destino (`IllegalArgumentException` si faltan).
2. Asigna el `id` del contador y el estado inicial `REGISTRADO`.
3. Agrega cada paquete con `agregarPaquete(...)`.
4. **Si no tiene paquetes → `IllegalStateException`** (la regla vive acá, en el
   objeto; el registro central no valida reglas de negocio).
5. Se registra a sí mismo en su cliente: `cliente.registrarEnvio(this)`
   (el envío es quien conoce a su cliente; App nunca toca la lista del cliente).

**Predicados privados** — nombres propios para las reglas, reutilizables:

```
todaviaNoSalio()       estado == REGISTRADO        ¿todavía no salió?
enPreparacion()        estado == EN_PREPARACION    ¿está en preparación?
estaEnViaje()          estado == EN_VIAJE          ¿fue despachado?
estaFinalizado()       estado == ENTREGADO         ¿ya llegó?
hayLugarParaPaquetes() paquetes.size() < 3         ¿hay lugar?
```

**Operaciones**

| Método | Visibilidad | Qué hace y por qué |
|---|---|---|
| `agregarPaquete(p)` | público | Valida nulo, `todaviaNoSalio()` y `hayLugarParaPaquetes()`; agrega. Cada condición es un predicado con nombre, no un `if` suelto |
| `transicionValida(nuevo)` / `avanzarA(nuevo)` | `private` | Única puerta de cambio de etapa: valida "de a un paso" con `ordinal()` |
| `recibirEn(sucursal)` | package-private | Llega a una sucursal: valida etapa; si era `REGISTRADO` avanza a `EN_PREPARACION` y hace recibir a sus paquetes; si era `EN_VIAJE` sólo registra el paso; siempre agrega movimiento `RECIBIDO` |
| `despacharDesde(sucursal)` | package-private | Sale de una sucursal: valida etapa; si estaba `EN_PREPARACION` avanza a `EN_VIAJE` y pone a sus paquetes en viaje; siempre registra `DESPACHADO` |
| `entregar()` | **público** | Llega al destino: exige `EN_VIAJE`, avanza a `ENTREGADO`, entrega paquetes, registra `ENTREGADO` con sucursal `null`. Público porque el destino final no siempre es una sucursal |
| `registrarMovimiento(tipo, sucursal)` | `private` | Crea el `Movimiento` y lo agrega al historial (Creator) |
| `getHistorial()` | público | Copia inmutable del historial |
| `getUltimoMovimiento()` | público | Último movimiento; `NoSuchElementException` si no hay |
| `getRecorrido()` | público | Sucursales sin repetir, en orden de primera pasada |
| `calcularCosto()` | público | `calcularCostoBase() + aplicarRecargo(...)` (Template Method) |
| `calcularCostoBase()` | `protected abstract` | La fórmula la decide cada subclase |
| `aplicarRecargo(base)` | `protected` | Por defecto devuelve 0; sólo lo sobreescribe quien tiene recargo |
| `calcularPesoTotal()` | `protected` | Suma de pesos; lo usan las subclases |

### 3.5 Las tres subclases — un mismo mensaje, tres respuestas

| Subclase | `calcularCostoBase()` | `aplicarRecargo()` | Extra |
|---|---|---|---|
| `EnvioEstandar` | peso × **$150/kg** | 0 (heredado) | — |
| `EnvioExpress` | peso × **$300/kg** | **+$500** fijo de urgencia | — |
| `EnvioInternacional` | peso × **$500/kg** | **20%** del base (aduana) | `paisDestino` + `validarPais()` privado; arma `"Ciudad, País"` antes del `super(...)` |

**No existe un solo `if (tipo...)` en el sistema**: el costo se resuelve por
polimorfismo. Agregar un cuarto tipo de envío = crear una subclase; no tocar
las existentes.

### 3.6 `Cliente` — dueño de su historia

| Atributo | Tipo | Por qué |
|---|---|---|
| `id` | `String final` | Identificador interno ("CLI-001") |
| `nombre` | `String final` | Nombre para mostrar |
| `dni` | `int final` | **Identificador de búsqueda**: con él lo encuentra el registro (búsqueda secuencial) |
| `envios` | `List<Envio>` | Todos los envíos que solicitó |

| Método | Visibilidad | Qué hace |
|---|---|---|
| Constructor `(id, nombre, dni)` | público | Valida todo (DNI debe ser > 0) |
| `registrarEnvio(envio)` | **package-private** | Lo llama el `Envio` al crearse. Valida que el envío le pertenezca (`envio.getCliente() != this` → error) y evita duplicados. Package-private: nadie de afuera debe registrar a mano |
| `getEnvios()` | público | `List.copyOf`: se consulta, no se modifica desde afuera |

### 3.7 `Sucursal` — la cola de despacho

| Atributo | Tipo | Por qué |
|---|---|---|
| `id` | `String final` | Código ("SUC-01") — también es la clave del registro |
| `nombre` | `String final` | Nombre para mostrar |
| `colaDeDespacho` | `Queue<Envio>` (`ArrayDeque`) | Los envíos esperan turno **en orden de llegada** |

| Método | Qué hace |
|---|---|
| `recibirEnvio(envio)` | Valida nulo y que no esté ya esperando; **primero** delega en `envio.recibirEn(this)` y **recién después** entra a la cola con `offer()`: si el envío rechaza, la sucursal no queda con un envío fantasma |
| `despachar(int cantidad)` | Valida `cantidad > 0`; con `poll()` saca hasta `cantidad` envíos **respetando FIFO** y a cada uno lo despacha con `envio.despacharDesde(this)`. Devuelve cuántos salieron. Si no sale ninguno: `IllegalStateException` |
| `getEnviosEnProceso()` | `List.copyOf(cola)`: copia en orden de llegada (el primero es el próximo en salir) |

### 3.8 `RegistroCentral` — el repositorio en memoria

| Método | Qué hace | Control propio |
|---|---|---|
| `registrarCliente(c)` | Agrega a la `ArrayList` | **DNI duplicado** → `IllegalStateException` |
| `listarClientes()` | Copia inmutable de la lista | — |
| `buscarClienteSecuencial(dni)` | **Recorre la lista desde el principio** hasta encontrar el DNI | No está → `NoSuchElementException` |
| `registrarSucursal(s)` | Agrega al `HashMap` por código | Código repetido → `IllegalStateException` |
| `buscarSucursal(codigo)` | `HashMap.get` directo | No existe → `NoSuchElementException` |
| `listarSucursales()` | Copia inmutable | — |
| `registrarEnvio(e)` | `HashMap.put` con el código de seguimiento como clave | **Código repetido** → `IllegalStateException` |
| `buscarEnvio(codigo)` | `HashMap.get`: **sin recorrer nada** | No existe → `NoSuchElementException` |
| `listarEnvios()` | Copia inmutable | — |
| `cantidadEnvios()` / `costoTotal()` / `costoPromedio()` | Consultas agregadas (suma/promedio de `calcularCosto()`) | — |

**Lo que NO hace**: acciones del negocio (despachar, entregar), validación de
reglas de negocio (eso vive en los objetos), ni impresión de menús. Sólo
**guarda, busca y lista** — y guarda **los mismos objetos, no copias**: si
buscás un envío y lo entregás, queda entregado en todo el sistema.

### 3.9 `App` — la capa de presentación

- `main(String[] args)`: orquesta la demo completa (ver flujo 2.7).
- `mostrarFicha(envio)` (`private static`): recupera cada dato con getters y
  formatea una línea. **Presenta, no calcula ni modifica.**
- Las 13 pruebas usan `try { ... } catch (Excepcion e) { System.out.println(...) }`:
  el error es esperado y se muestra con `e.getMessage()`.
- **No tiene** un solo `if` de estados, ni listas de envíos propias, ni
  accesos a paquetes: toda la información la pide con getters a los objetos.

---

## 4. Cuestionarios de autoevaluación

> Respondé **sin mirar las respuestas** (están en la sección 5) y después
> corregí. Marcá la opción correcta o respondé en una frase.

### Cuestionario A — Arquitectura

1. ¿Cuántas capas tiene el sistema y cuáles son?
2. ¿Qué clase pertenece a la capa de presentación y qué dos cosas no debería hacer?
3. ¿Qué patrón describe mejor a `RegistroCentral`: controlador, repositorio o modelo? ¿Por qué?
4. ¿Dónde se crea la instancia de `RegistroCentral` y por qué ahí?
5. ¿Se usa algún framework? Nombra las dos librerías del JDK que sí se usan.
6. ¿Hay paquetes (`package`) declarados? ¿Qué comanda de compilación aprovecha eso?
7. ¿Qué patrón se usa en `Envio.calcularCosto()` y cómo se llama técnicamente?
8. Nombra cuatro "dueños" de colecciones y qué colección posee cada uno.
9. ¿Por qué `App` nunca es conocida por las clases del dominio?
10. ¿Qué significa "copia defensiva" y dónde se aplica?

### Cuestionario B — `Paquete` y estados

1. Escribí los cuatro estados de `Paquete` en orden.
2. ¿Existe `setEstado()`? ¿Por qué?
3. ¿Quién cambia el estado de un paquete y con qué métodos?
4. ¿Qué le pasa al paquete si alguien intenta `ENTREGADO → EN_VIAJE`?
5. ¿Por qué `transicionValida()` es `private` y no `public`?
6. ¿Qué valida `validarPeso()` y con qué excepción?
7. ¿Qué hace exactamente `this(id, "Sin descripción", pesoKg)` y por qué existe?
8. ¿Cuál es la visibilidad de `recibir()`, `iniciarViaje()` y `entregar()`, y qué garantiza eso?
9. ¿Qué es un `enum` y qué gana el sistema con que los estados lo sean?
10. Un paquete nace en ¿qué estado?

### Cuestionario C — `Envio`

1. ¿Qué tres cosas coordina un `Envio`?
2. ¿Cuántos paquetes admite como máximo y en qué método se controla?
3. ¿De dónde sale el código de seguimiento (`id`)?
4. ¿Qué pasa si se crea un envío sin paquetes y en qué momento se entera el sistema?
5. ¿Por qué el constructor llama `cliente.registrarEnvio(this)`?
6. Nombrá cuatro predicados `private` de `Envio` y qué pregunta cada uno.
7. ¿Cómo comprueba `avanzarA()` que la transición sea válida? Escribí la idea en una línea.
8. En una sucursal intermedia, ¿qué cambia el envío y qué no?
9. ¿Por qué `entregar()` es público y `recibirEn()` no?
10. ¿Qué hace `registrarMovimiento()` y quién puede invocarlo?
11. ¿Qué diferencia hay entre `getHistorial()` y `getRecorrido()`?
12. ¿Qué pasa si se pide `getUltimoMovimiento()` a un envío sin recorrido?

### Cuestionario D — Colaboración entre objetos

1. Escribí la cadena completa de mensajes cuando una sucursal **recibe** un envío.
2. Escribí la cadena cuando una sucursal lo **despacha**.
3. ¿Por qué `Sucursal.recibirEnvio()` pide permiso al envío *antes* de agregarlo a su cola?
4. ¿Qué haría mal App si hiciera `envio.getPaquetes().get(0).setEstado(...)`?
5. Explicá "Tell, don't ask" con un ejemplo del código.
6. ¿Quién crea los `Movimiento` y por qué su constructor es package-private?
7. ¿Qué representa `sucursal == null` dentro de un `Movimiento`?
8. ¿Qué pregunta deberías hacerte ante cada funcionalidad nueva?
9. ¿Puede una `Sucursal` hablar directamente con un `Paquete`? Justificá.
10. ¿Quién responde "¿por qué sucursales pasó este envío?" y cómo lo hace?

### Cuestionario E — Colecciones, registro y búsquedas

1. ¿Qué estructura guarda los envíos `RegistroCentral` y cuál es su clave?
2. ¿Por qué `HashMap` para envíos y `ArrayList` para clientes?
3. ¿Qué significa FIFO? ¿Qué colección lo implementa en el sistema?
4. Con `despachar(5)` y 3 envíos en la cola, ¿qué sale y qué devuelve el método?
5. ¿Qué pasa si la cola está vacía y se llama `despachar(2)`?
6. ¿Qué hacen `offer()` y `poll()` sobre la cola?
7. ¿Por qué los getters de colecciones usan `List.copyOf(...)`?
8. Si dos envíos tuvieran el mismo código de seguimiento, ¿qué hace el registro?
9. ¿En qué orden busca el cliente `buscarClienteSecuencial(dni)`? ¿Y qué hace si no lo encuentra?
10. Si el registro guardara copias en vez de referencias, ¿qué problema tendría el sistema?

### Cuestionario F — Excepciones

1. ¿Cuáles son las tres excepciones del sistema y cuándo corresponde cada una?
2. Escribí un ejemplo de `IllegalArgumentException` y por qué no es `IllegalStateException`.
3. Escribí dos ejemplos de `IllegalStateException`.
4. Escribí dos ejemplos de `NoSuchElementException`.
5. ¿Por qué el modelo lanza excepciones en vez de imprimir el error?
6. ¿Quién decide cómo se muestra un error en consola?
7. ¿Qué le ocurre al programa si nadie captura una excepción?
8. En las pruebas de App, ¿para qué sirve el `catch`?

### Cuestionario G — Herencia y polimorfismo

1. Escribí la jerarquía: superclase, subclases y quién es abstracta.
2. ¿Qué método es abstracto y cuál tiene implementación por defecto?
3. Escribí la fórmula de `calcularCosto()` en pseudocódigo.
4. ¿Qué calcula distinto cada subclase y en qué se diferencian?
5. ¿Por qué no hace falta un `if (tipo.equals(...))` para calcular costos?
6. Si mañana aparece un "envío refrigerado", ¿qué habría que tocar en el código?
7. ¿Por qué `EnvioInternacional` valida el país en un método `private static` antes del `super(...)`?
8. ¿Se puede instanciar `new Envio(...)` directamente? ¿Por qué?

---

## 5. Respuestas

### A — Arquitectura

1. Tres: **presentación** (`App`), **almacén** (`RegistroCentral`) y **dominio**
   (`Cliente`, `Sucursal`, `Envio`, `Paquete`, `Movimiento`); más los enums como
   catálogos de valores.
2. `App`. No debe contener reglas de negocio ni manipular el interior de los
   objetos (sólo crear, mandar mensajes y mostrar).
3. **Repositorio**: centraliza el acceso a los datos (registrar, buscar, listar)
   y no ejecuta la lógica del negocio.
4. En el `main`, con `new RegistroCentral()`: así hay **una sola instancia**
   accesible para toda la demo y el modelo no depende de quién la crea.
5. No: Java SE puro. Se usan `java.util` y `java.time`.
6. No hay `package`: todas las clases están en el paquete por defecto; se
   compila con `javac -encoding UTF-8 -d classes src\*.java`.
7. **Template Method**: el molde (`calcularCosto`) está en la superclase y las
   piezas variables (`calcularCostoBase`, `aplicarRecargo`) se delegan.
8. `Cliente` → `List<Envio>`; `Envio` → `List<Paquete>` + `List<Movimiento>`;
   `Sucursal` → `Queue<Envio>`; `RegistroCentral` → `List<Cliente>` + dos `Map`.
9. Por arquitectura: la dependencia apunta hacia el modelo; si el dominio
   conociera a `App`, no podría usarse sin ella (acoplamiento innecesario).
10. Devolver una copia que no se puede modificar (`List.copyOf`): se puede
    consultar la colección, pero no agregar ni borrar desde afuera.

### B — `Paquete`

1. `RECIBIDO → EN_PREPARACION → EN_VIAJE → ENTREGADO`.
2. No: el estado sólo cambia por acciones reales del negocio; un setter
   permitiría saltarse etapas.
3. Su `Envío`, mediante `recibir()`, `iniciarViaje()` y `entregar()`
   (package-private).
4. `transicionValida()` falla (no es +1) y se lanza `IllegalStateException`.
5. Es implementación interna: los demás piden la acción, no necesitan saber
   cómo decide el paquete.
6. Que el peso sea mayor a cero; si no, `IllegalArgumentException`.
7. Es la llamada al otro constructor (**delegación con `this(...)`**): un solo
   lugar donde se valida y asigna; el constructor mínimo sólo aporta el valor
   por defecto.
8. Son package-private: sólo las invoca su `Envio`; App ni las ve.
9. Conjunto fijado de valores y tipado: no puede existir un estado inventado,
   y su posición (`ordinal`) permite la regla "avanza de a un paso".
10. `RECIBIDO`.

### C — `Envio`

1. Sus **paquetes**, su **historial** de movimientos y su **estado**.
2. Tres, en `agregarPaquete()` vía `hayLugarParaPaquetes()`.
3. Del contador estático `contadorId` (autoincremental): es el código de
   seguimiento.
4. Lanza `IllegalStateException` **en el constructor**, antes de registrarse en
   el cliente: no queda rastro de un envío inexistente.
5. Porque es el que conoce a su cliente y a sí mismo: así la lista del cliente
   se mantiene consistente sin que App la toque.
6. `todaviaNoSalio()`, `enPreparacion()`, `estaEnViaje()`, `estaFinalizado()`
   (y `hayLugarParaPaquetes()`): nombran las reglas para reutilizarlas.
7. Compara posiciones: el nuevo estado debe ser exactamente el siguiente
   (`ordinal() == actual + 1`).
8. **No cambia la etapa** (sigue `EN_VIAJE`); sólo registra los movimientos
   `RECIBIDO`/`DESPACHADO` en el historial.
9. Porque el destino final no siempre es una sucursal; en cambio, entrar y
   salir de sucursales lo mueven sólo las sucursales (métodos package-private).
10. Crea el `Movimiento` y lo agrega al historial; sólo lo invoca el propio
    `Envio` (método `private`).
11. `getHistorial()` devuelve **todos** los movimientos; `getRecorrido()`
    devuelve sólo las **sucursales sin repetir**, en orden.
12. `NoSuchElementException`.

### D — Colaboración

1. `App → Sucursal.recibirEnvio(envio) → Envio.recibirEn(sucursal) → Paquete.recibir()`
   (y al entrar a la cola: `cola.offer(envio)`).
2. `App → Sucursal.despachar(cantidad) → cola.poll() → Envio.despacharDesde(sucursal) → Paquete.iniciarViaje()`.
3. Si el envío rechaza la operación (etapa inválida), la sucursal no queda con
   un envío fantasma en su cola.
4. Rompería el encapsulamiento: App decidiría el estado de un objeto que no
   conoce; además saltaría la validación de transiciones.
5. Pedir comportamiento en vez de datos: `envio.entregar()` en vez de sacar los
   paquetes y cambiarles el estado a mano.
6. Los crea `Envio` (Creator) y su constructor es package-private para que
   nadie desde afuera fabrique movimientos falsos.
7. Que la entrega ocurrió en el destino final, que no siempre es una sucursal
   de la red.
8. "¿Qué objeto debería ser responsable de esto y por qué?"
9. No: la cadena es `Sucursal → Envio → Paquete`; el envío es quien coordina a
   sus paquetes.
10. El propio `Envio`, con `getHistorial()` / `getRecorrido()` /
    `getUltimoMovimiento()`; App sólo muestra lo que le devuelve.

### E — Colecciones

1. `HashMap<Integer, Envio>`; la clave es el **código de seguimiento** (`id`).
2. El código es un identificador único: el HashMap lo encuentra directo. Los
   clientes están en lista para practicar la **búsqueda secuencial** por DNI.
3. *First In, First Out*: primero que entra, primero que sale. La
   `Queue<Envio>` de `Sucursal` (`ArrayDeque`).
4. Salen los 3 (los que haya) en orden de llegada y devuelve `3`.
5. Lanza `IllegalStateException` ("cola vacía").
6. `offer()` agrega **al final**; `poll()` saca **el primero**.
7. Copia defensiva: se consulta pero no se modifica desde afuera.
8. No lo registra y lanza `IllegalStateException` (código repetido).
9. Desde el principio, elemento por elemento; si no está,
   `NoSuchElementException`.
10. Modificar una copia no se reflejaría en el sistema: perdería la noción de
    "la misma instancia que el resto".

### F — Excepciones

1. `IllegalArgumentException` (argumento inválido), `IllegalStateException`
   (el objeto no está en estado para la operación), `NoSuchElementException`
   (la búsqueda no tuvo resultado).
2. Peso ≤ 0 o DNI ≤ 0 en un constructor: el problema está en el **dato que se
   pasa**, no en el estado previo del objeto.
3. Transición inválida de estado; despachar con la cola vacía; código de envío
   o DNI duplicado; agregar paquetes a un envío que ya salió.
4. `buscarEnvio(999)`, `buscarClienteSecuencial(inexistente)`,
   `getUltimoMovimiento()` sin movimientos.
5. Porque señalar el problema es responsabilidad del objeto que lo detecta;
   cómo mostrarlo es asunto de la capa de presentación.
6. `App`, capturándola e imprimiendo `e.getMessage()`.
7. Se interrumpe la ejecución y el programa termina con el mensaje de error.
8. Para que la demo **continúe** y muestre el error esperado: la operación era
   inválida a propósito.

### G — Herencia

1. `Envio` (abstracta) ← `EnvioEstandar`, `EnvioExpress`, `EnvioInternacional`.
2. `calcularCostoBase()`; `aplicarRecargo()` tiene default (devuelve 0).
3. `costo = calcularCostoBase() + aplicarRecargo(base)`.
4. Estándar: peso × $150. Express: peso × $300 + $500. Internacional: peso ×
   $500 + 20% de aduana.
5. Por **polimorfismo**: cada subclase responde distinto al mismo mensaje.
6. Sólo crear la subclase e implementar sus dos métodos; el resto no cambia.
7. Porque el `super(...)` necesita el destino ya armado ("Ciudad, País"): la
   validación debe ocurrir **antes** de construir la superclase.
8. No: `Envio` es `abstracta`; sólo se instancian sus subclases.

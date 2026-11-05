# Guía de estudio — Sistema de Gestión de Envíos y Distribución

> Instancias evaluadas: **TP1 · TP2 · TP2.1 · TP4** (el TP3 no forma parte de esta guía).
> Cada sección explica **qué pedía el enunciado**, **cómo se resolvió en el código** y
> **qué tenés que poder explicar** si te preguntan por qué está así.

---

## 0. Vista general del proyecto

### Cómo se compila y ejecuta

```
javac -encoding UTF-8 -d classes src\*.java
java -Dstdout.encoding=UTF-8 -cp classes App
```

La salida muestra el ciclo completo: alta de objetos → recorrido → seguimiento →
13 pruebas de validación (errores esperados).

### Inventario de clases

| Clase | Responsabilidad | Nació en |
|---|---|---|
| `Paquete` | Objeto físico transportado: identidad, descripción, peso y estado propio | TP1 |
| `EstadoPaquete` (enum) | RECIBIDO → EN_PREPARACION → EN_VIAJE → ENTREGADO | TP1 |
| `Envio` (abstracta) | Operación logística: coordina sus paquetes, calcula costo, guarda su historial | TP1 |
| `EnvioEstandar` / `EnvioExpress` / `EnvioInternacional` | Tres formas distintas de calcular el costo (polimorfismo) | TP1 |
| `Cliente` | Quién solicita los envíos: DNI + lista propia de envíos | TP2 (DNI en TP4) |
| `Sucursal` | Procesa envíos: los recibe y los despacha desde su **cola** FIFO | TP2 (cola en TP4) |
| `Movimiento` + `TipoMovimiento` (enum) | Un acontecimiento del recorrido: qué, cuándo y dónde; inmutable | TP2 / TP2.1 |
| `EstadoEnvio` (enum) | REGISTRADO → EN_PREPARACION → EN_VIAJE → ENTREGADO | TP2 |
| `RegistroCentral` | Base de datos en memoria: guarda y encuentra clientes, sucursales y envíos | TP4 (antes `GestorEnvios`, TP2) |
| `App` | Sólo presenta: crea objetos, les manda mensajes y muestra lo que le devuelven | TP1 |

### Los principios transversales (el hilo de todos los TP)

1. **Encapsulamiento**: atributos `private`; el estado sólo cambia por acciones reales
   del negocio (`Paquete.recibir()`, no `paquete.setEstado(...)`).
2. **Tell, don't ask**: se pide *comportamiento*, no datos internos para resolverlo
   desde afuera. `envio.entregar()` en vez de `envio.getPaquetes().get(0).setEstado(...)`.
3. **Information Expert**: la responsabilidad vive donde está la información.
   `Envio` calcula su costo porque conoce sus paquetes; `Envio` administra su
   historial porque es el dueño de los movimientos.
4. **Creator**: quien crea un objeto es el que lo administra. `Envio` crea sus
   `Movimiento` (constructor package-private).
5. **Polimorfismo**: `calcularCostoBase()` responde distinto en cada subclase sin
   un solo `if (tipo.equals(...))` en todo el sistema.

---

## 1. TP1 — Modelado inicial

**Idea central**: representar bien los objetos del dominio, sus responsabilidades y
cómo colaboran. No alcanza con que "funcione": se evalúa **dónde vive cada cosa**.

### Parte 1 — Modelado inicial

| Pregunta del enunciado | Respuesta en el código |
|---|---|
| ¿Qué información conoce un paquete? | `id`, `descripcion`, `pesoKg` — todos `private` y `final` salvo el estado |
| ¿Qué debe quedar protegido? | Todo: sólo hay getters, **no hay setters** |
| ¿Quién lo modifica? | Su `Envio`, mediante mensajes `recibir()`, `iniciarViaje()`, `entregar()` |
| ¿Cómo cambia su estado? | Avanzando **de a un paso** con `avanzarEstado()` (ver Parte 2) |
| ¿Qué necesita conocer un envío? | Sus paquetes (1 a 3), su destino, su costo y — desde TP2 — su cliente |
| ¿Quién calcula el costo? | El propio `Envio`, delegando la fórmula en cada subclase |

### Parte 2 — Encapsulamiento y estado

- Estados del paquete: `EstadoPaquete` (enum): `RECIBIDO → EN_PREPARACION → EN_VIAJE → ENTREGADO`
  (en el TP1 se llamaba "salió a distribución"; en TP2 se renombró a `EN_VIAJE`).
- **No se cambia el estado con setters**: el enunciado lo advierte ("¿Conviene usar
  setters para todo?"). Las acciones reales son `recibir()`, `iniciarViaje()`,
  `entregar()` — y son **package-private**: sólo su `Envio` puede invocarlas.
- `transicionValida()` comprueba `nuevo.ordinal() == estado.ordinal() + 1`: sólo se
  avanza un paso. Intentar `ENTREGADO → EN_VIAJE` lanza `IllegalStateException`.

### Parte 3 — Constructores y sobrecarga

```java
public Paquete(String id, double pesoKg) {            // mínimo
    this(id, "Sin descripción", pesoKg);              // delega en el completo
}
public Paquete(String id, String descripcion, double pesoKg) { ... }
```

- La sobrecarga **delega** con `this(...)`: un solo lugar donde se valida y asigna.
- Datos obligatorios: `id` y `pesoKg` (sin peso no hay costo posible); opcional: `descripcion`.
- `EnvioInternacional` agrega un constructor con `paisDestino` y arma el destino
  como `"Ciudad, País"` antes de llamar a `super(...)`.

### Parte 4 — Mensajes entre objetos

La cadena central del sistema (ya aparece en el TP1 y es la misma hasta el TP4):

```
App → Sucursal.recibirEnvio(envio) → Envio.recibirEn(sucursal) → Paquete.recibir()
App → Sucursal.despachar(cantidad) → Envio.despacharDesde(suc.) → Paquete.iniciarViaje()
App → envio.entregar()                                → Paquete.entregar()
```

**Pregunta clave del TP1**: "si algo debe ocurrir, ¿qué objeto debería tener esa
responsabilidad?" → El que coordina la operación: el `Envio` coordina a sus paquetes;
`App` sólo dispara.

### Parte 5 — Lógica interna y ocultamiento

| Método `private` | Por qué |
|---|---|
| `Paquete.validarPeso()` | Regla interna: el objeto no puede existir con peso inválido |
| `Paquete.transicionValida()` | Sólo el paquete sabe cómo decide su máquina de estados |
| `EnvioInternacional.validarPais()` | Validación propia de ese tipo de envío |
| `Envio.transicionValida()` / `avanzarA()` | (TP2/TP2.1) mismos criterios, en el envío |
| Predicados TP2.1 (`todaviaNoSalio()`, etc.) | Ver sección 3 |

**Criterio**: si sólo lo usa el propio objeto → `private`. Si otros objetos necesitan
pedir la acción (no el cómo) → método público/package-private de la acción.

### Parte 6 — Colecciones

- En el TP1 la lista de envíos vivía **en el main** (requisito puntual: "esta lógica
  la dejaremos en el main").
- Hoy esa responsabilidad migró: cada `Cliente` tiene su `List<Envio>` y
  `RegistroCentral` guarda todos los envíos (evolución natural — el TP2 la empuja
  hacia los objetos).

### Parte 7 — Herencia

- `Envio` es **abstracta**: define el molde y dos puntos de variación:

```java
protected abstract double calcularCostoBase();   // cada tipo: su fórmula
protected double aplicarRecargo(double base)      // por defecto: 0
```

- `calcularCosto()` (público, en la base) combina ambos: **Template Method**.
- Tarifas: Estándar `$150/kg` · Express `$300/kg + $500` fijo · Internacional
  `$500/kg + 20%` de aduana. **Ni un solo `if (tipo...)` en el sistema.**

### Preguntas de reflexión (readme del TP1) — cómo responderlas con este código

1. *¿Qué responsabilidades asignaste?* → Paquete cuida su estado; Envio coordina y
   calcula; Cliente administra su historia; Sucursal procesa; App presenta.
2. *¿Decisiones difíciles?* → típicas: quién cambia el estado (el Envio, no App) y
   dónde poner la lista de envíos (luego migró a Cliente).
3. *¿Métodos privados y por qué?* → validarPeso y transicionValida: implementación
   interna; si fuera público, cualquiera podría romper invariantes.
4. *¿Setters evitados?* → todos los de estado (`setEstado`) y los de campos finales
   (`setId`, `setPesoKg`): el estado cambia sólo por acciones reales.
5. *¿Qué cambió con la herencia?* → el costo salió de `Envio` a las subclases; antes
   habría que agregar un `if` por tipo cada vez que apareciera uno nuevo.

---

## 2. TP2 — Evolución del diseño (clientes, sucursales, historial, reglas)

**Idea central**: no es un proyecto nuevo; es **analizar qué objetos faltan y a quién
le corresponde cada cosa**. El enunciado no da atributos ni métodos: eso se decide.

### Nivel 1 — Clientes

- `Cliente` tiene `List<Envio> envios` porque un cliente puede hacer muchos envíos.
- **¿Quién registra que el cliente hizo un envío?** El propio `Envio`, en su
  constructor: `cliente.registrarEnvio(this)`. Así `App` nunca toca la lista
  (método `registrarEnvio` es package-private y valida que el envío le pertenezca).
- Respuesta esperada a "¿debería App modificar la lista?" → **no**.

### Nivel 1 — Sucursales y colaboración

- `Sucursal` tiene `id` y `nombre`, y sabe qué envíos tiene en curso.
- `recibir(envio)` y `despachar(envio)` **primero le piden permiso al envío**
  (`envio.recibirEn(this)` / `envio.despacharDesde(this)`) y recién después tocan su
  propia colección: si el envío rechaza, la sucursal no queda con fantasmas.
- La sucursal **no** cambia estados de paquetes directamente: delega en el envío,
  que delega en sus paquetes. Cadena: `App → Sucursal → Envio → Paquete`.
- Ejemplo que el enunciado marca como incorrecto: `envio.getPaquete().setEstado(...)`
  desde App — eso no es orientado a objetos.

### Nivel 2 — Seguimiento e historial

- **`Movimiento`** = un acontecimiento: `fecha` (se toma sola), `tipo`
  (`TipoMovimiento`), `sucursal` (`null` sólo en la entrega final, que no siempre es
  una sucursal). Es **inmutable**: `final`, sin setters.
- **¿Quién administra el historial?** `Envio`: es el dueño del agregado y el que
  crea movimientos (Creator). Su constructor es package-private: nadie desde afuera
  fabrica movimientos.
- Consultas (TP2.1 las afina): `getHistorial()`, `getRecorrido()` (sucursales sin
  repetir, en orden), `getUltimoMovimiento()` (lanza `NoSuchElementException` si no
  hay recorrido).
- Sucursales intermedias: **no cambian la etapa** (la máquina de estados sólo avanza),
  pero sí registran movimiento — así se reconstruye el recorrido completo.

### Nivel 3 — Reglas de negocio y validaciones

| Sitión inválida del enunciado | Dónde se impide hoy |
|---|---|
| Entregar un paquete/envío sin despachar | `Envio.entregar()` → `!estaEnViaje()` |
| Despachar un envío ya entregado | `Envio.despacharDesde()` → `estaFinalizado()` |
| Registrar un envío sin paquetes | `Envio` (constructor) — desde TP4 |
| Recibir un envío en otra etapa | `Envio.recibirEn()` → `estaFinalizado()` / `enPreparacion()` |
| Operar sobre un envío inexistente | `RegistroCentral.buscarEnvio()` → `NoSuchElementException` |
| Transición arbitraria de estado | `avanzarA()` / `avanzarEstado()` → `transicionValida()` |

- La validación está **cerca del objeto que conoce el estado**: `Envio` para su
  etapa, `Paquete` para la suya. `App` no tiene un solo `if` de estados.

### El criterio de evaluación del TP2

> "No se evaluará sólo que funcione: se evaluará **cómo distribuyeron las
> responsabilidades**. Ante cada operación: ¿qué objeto debería ser responsable y por qué?"

Fórmula para defender cualquier parte del sistema:
**"X es responsable porque es quien conoce Y (Information Expert) y porque lo crea/posee
(Creator); los demás le piden comportamiento (Tell, don't ask)."**

---

## 3. TP2.1 — Correcciones (checklist enunciado ↔ código)

| Requisito del enunciado | Cómo está resuelto |
|---|---|
| El tipo de movimiento **no** puede ser un `String` | `enum TipoMovimiento { RECIBIDO, DESPACHADO, ENTREGADO }` — evita movimientos incoherentes |
| Las consultas **devuelven datos**, no imprimen | `getHistorial()`, `getRecorrido()`, `getUltimoMovimiento()`; **App** es quien imprime. Se eliminaron todos los métodos `mostrar*` |
| Excepciones por operaciones inválidas | 6 situaciones del enunciado → `IllegalStateException` / `NoSuchElementException` / `IllegalArgumentException` (tabla de la sección 2) |
| Validaciones en **métodos privados con nombre** | `todaviaNoSalio()`, `enPreparacion()`, `estaEnViaje()`, `estaFinalizado()`, `hayLugarParaPaquetes()`, `transicionValida()` |
| **Sin** cadenas de `if/else` ni "simplemente un switch" | `avanzarA(nuevo)` valida `nuevo.ordinal() == estado.ordinal() + 1`; la transición la decide el objeto dueño del estado |
| App **no manipula** datos internos | App sólo llama `recibirEnvio`/`despachar`/`entregar` y getters; nunca toca paquetes ni estados |
| `Movimiento` como objeto de dominio | Clase inmutable con constructor package-private (sección 2) |
| 2 diagramas de secuencia + diagrama de clases | Entregados como PDF (con carátula); el diagrama de actividad se agregó después por interés propio |

**Los 6 predicados privados y dónde se usan:**

```
todaviaNoSalio()      → agregarPaquete(), recibirEn()      (¿todavía no salió?)
enPreparacion()       → recibirEn(), despacharDesde()      (¿está en preparación?)
estaEnViaje()         → entregar()                         (¿fue despachado?)
estaFinalizado()      → recibirEn(), despacharDesde(), entregar()
hayLugarParaPaquetes()→ agregarPaquete()                   (¿hay lugar? ≤ 3)
transicionValida()    → avanzarA() / Paquete.avanzarEstado() (¿avanza de a un paso?)
```

---

## 4. TP4 — Estructuras de datos

**Idea central**: hasta el TP2 modelamos *qué hace* cada objeto; el TP4 agrega **dónde
se guardan y cómo se encuentran**. No hay diagramas; hay una parte teórica por escrito.

### 4.1 `RegistroCentral` (Parte 1)

Base de datos en memoria: **una sola instancia, creada en el main**.

| Sí hace | No hace |
|---|---|
| Guarda clientes, sucursales y envíos | No despacha, entrega ni demora envíos |
| Encuentra por código de seguimiento, DNI o código de sucursal | No cambia el estado ni los datos de lo que guarda |
| Lista lo que tiene guardado | No se reemplaza la lógica de `Envio`/`Sucursal`/`Cliente` |
| Controla que no haya códigos repetidos y avisa si algo no existe | No valida reglas del negocio (eso vive en los objetos) |
| | No imprime menús ni pide datos por teclado |

**Regla práctica**: si el nombre del método es acción del negocio (despachar,
entregar, cancelar) → no va acá. Si es `registrar`, `buscar` o `listar` → sí.

Consecuencia visible en el código: la regla *"un envío necesita paquetes"* se mudó de
`GestorEnvios.registrar()` al **constructor de `Envio`**.

### 4.2 `HashMap` de envíos (Parte 2)

```java
private final Map<Integer, Envio> envios;   // clave: código de seguimiento
envios.put(envio.getId(), envio);           // registrar
envios.get(codigo);                         // buscar: directo, sin recorrer
envios.containsKey(codigo);                 // control de duplicado
```

- **¿Por qué HashMap y no lista?** El código de seguimiento es un dato único que
  identifica al envío: con la clave se encuentra el valor de forma directa, sin
  recorrer toda la colección ni ordenar nada.
- Se guardan **los mismos objetos, no copias**: si buscás un envío y lo entregás,
  queda entregado en todo el sistema; no hace falta "volver a guardarlo".

### 4.3 Cola de despacho en `Sucursal` (Parte 3)

```java
private final Queue<Envio> colaDeDespacho;   // ArrayDeque (FIFO)

recibirEnvio(envio)  →  cola.offer(envio)    // entra al FINAL
despachar(cantidad)  →  cola.poll()          // sale el PRIMERO, hasta `cantidad`
```

- **FIFO**: primero que entra, primero que sale (la fila del banco).
- Si hay menos envíos que la capacidad → **salen los que haya** (el método devuelve
  cuántos salieron). Si la cola está vacía → `IllegalStateException`.
- Reemplaza a la `List<Envio>` del TP2: misma información pero **con orden de
  llegada**. En la demo: llegan #1, #2, #3 a Viedma y con `despachar(2)` salen #1 y
  #2; #3 espera su turno.

### 4.4 Búsqueda secuencial por DNI (Parte 4.a)

```java
private final List<Cliente> clientes;        // ArrayList

buscarClienteSecuencial(int dni) → recorre la lista desde el principio;
                                    si no está: NoSuchElementException
```

Los clientes se guardan en **lista** (no en HashMap) justamente para practicar la
búsqueda secuencial que pide el TP.

### 4.5 Repaso teórico (borrador — reescribilo con tus palabras y citá las fuentes)

> El enunciado pide las respuestas **con sus palabras** y aplicadas a nuestro sistema.
> Esto es un modelo para estudiar, no una respuesta para copiar.

**Parte 2 — HashMap**: estructura clave→valor; calcula un hash de la clave y accede
directo al valor. En nuestro sistema: `codigo → Envio`, `DNI` sería lo mismo si
quisieramos búsqueda directa de clientes.

**Parte 3 — Preguntas de la cola**

1. *¿Qué pasaría si los envíos se despacharan en otro orden?* Se rompería la
   expectativa de orden de llegada: el envío #1, registrado primero, podría esperar
   más que el #3 sin motivo; además sería imposible predecir qué sale en el próximo
   despacho. En logística real, el orden de llegada es el compromiso con el cliente.
2. *¿Dónde más podría usarse una cola?* Por ejemplo: pedidos de un depósito que se
   preparan en orden de recepción, o llamadas al centro de atención. (Proponé una
   idea propia, sin implementarla.)

**Parte 4.b — Búsqueda binaria**

1. *Qué es*: algoritmo para buscar en una lista **ordenada**: mira el elemento del
   medio y descarta la mitad donde el valor no puede estar; repite con la mitad
   restante hasta encontrarlo o quedarse sin rango.
2. *Por qué necesita orden*: la lógica "si el medio es menor, está a la derecha"
   sólo es válida si la lista está ordenada; sin orden no se puede descartar nada.
3. *Ejemplo con 8 DNIs ordenados*: 8 elementos → se mira el 4º → quedan 4 → se mira
   el 2º → quedan 2 → se mira el 1º... en el peor caso: 3 comparaciones
   (log₂ 8 = 3).
4. *1000 clientes*: secuencial ≈ **1000** comparaciones; binaria ≈ **10**
   (log₂ 1000 ≈ 10). De 1000 a 10: por qué conviene ordenar una vez.
5. *Dónde iría*: en `RegistroCentral`, que es dueño de la lista; trabajaría sobre
   `ArrayList<Cliente>`; habría que **ordenar por DNI** porque es el dato de búsqueda.
6. *Qué cambiaría*: hay que elegir cuándo ordenar (al registrar o antes de cada
   búsqueda); si se agrega un cliente **al final**, la lista queda desordenada y
   habría que reordenar; `buscarClienteSecuencial` seguiría existiendo (el TP la
   pide) — en la práctica, con un HashMap por DNI se evitaría ordenar.
7. *Por qué HashMap y no lista ordenada con binaria*: el código de seguimiento es un
   identificador único; el HashMap lo resuelve en un paso directo, sin ordenar ni
   comparar. La binaria sirve cuando el dato está en una lista y no tenemos clave.
8. *Dónde más ordenamiento*: listar los envíos por costo (de mayor a menor) para
   priorizar; ordenar los clientes por apellido para el padrón.

**Parte 5 — Pilas (LIFO)**

1. Una **pila** entra y sale por el mismo extremo (último en entrar, primero en
   salir); una **cola** entra por un extremo y sale por el otro (FIFO).
2. Ejemplos: "deshacer" de un editor, el botón "atrás" del navegador, la pila de
   platos de una cocina.
3. *En nuestro sistema*: una pila de "deshacer" para retroceder la última operación
   sobre un envío (por ejemplo, deshacer el último despacho registrado).

**Parte 6 — Grafos**

1. Un **grafo** es un conjunto de **nodos** (entidades) unidos por **aristas**
   (relaciones entre ellas).
2. **Dirigido**: las aristas tienen sentido único (seguir a alguien en una red
   social). **No dirigido**: la relación es mutua (amistades). **Ponderado**: cada
   arista tiene un peso (distancia en km entre ciudades). **No ponderado**: sólo
   importa si existe la conexión.
3. Tres usos reales: mapas (rutas), redes sociales (vínculos), redes de transporte
   (líneas de colectivo).
4. *En nuestro sistema*: sucursales = nodos, rutas = aristas; el **peso** podría ser
   la distancia en km, el tiempo de viaje o el costo de combustible. Sería **no
   dirigido** si el camión puede ir y volver por la misma ruta (o dirigido si hay
   sentido único en las calles).
5. *Dónde podría usarse*: para calcular la **ruta más corta** entre sucursales
   (nombrar "camino más corto" y el algoritmo de **Dijkstra**, sin implementarlo).

**Fuentes para citar** (elegí y resumí con tus palabras): documentación oficial de
Java (Oracle Tutorials → Collections / HashMap), Wikipedia (búsqueda binaria,
algoritmo de Dijkstra), GeeksforGeeks.

---

## 5. Preguntas de defensa (cheatsheet)

**TP1**
- *¿Por qué Paquete no tiene setters?* → El estado sólo cambia por acciones
  reales; un setter permitiría saltarse etapas.
- *¿Quién cambia el estado del paquete?* → Su `Envio`, mediante mensajes
  package-private (`recibir`, `iniciarViaje`, `entregar`).
- *¿Cómo se calcula el costo sin `if` por tipo?* → `calcularCosto()` en la base
  llama a `calcularCostoBase()` (abstracto) y `aplicarRecargo()`; cada subclase
  responde distinto: polimorfismo.
- *¿Por qué `private` en `validarPeso`?* → Es implementación interna; el objeto
  protege sus invariantes.

**TP2**
- *¿Quién registra los envíos de un cliente?* → El `Envio`, en su constructor
  (`cliente.registrarEnvio(this)`): es quien conoce a su cliente.
- *¿Por qué no `envio.getPaquete().setEstado(...)`?* → Eso expone la interna de
  otro objeto; se pide el comportamiento `envio.despachar()` y él coordina.
- *¿Quién administra el historial?* → `Envio` (crea y guarda sus `Movimiento`,
  inmutables).
- *¿Por qué las sucursales intermedias no cambian la etapa?* → La máquina de
  estados sólo avanza de a un paso; el tránsito se registra en el historial igual.
- *¿Dónde se valida una transición?* → En el objeto dueño del estado
  (`transicionValida()`), nunca en `App`.

**TP2.1**
- *¿Por qué `TipoMovimiento` y no `String`?* → El enunciado lo prohíbe; el enum
  hace imposibles los movimientos incoherentes.
- *¿Por qué `getUltimoMovimiento()` y no `mostrarUltimoMovimiento()`?* → Las
  consultas devuelven datos; quién muestra es la capa de presentación (App).
- *¿Por qué `NoSuchElementException`?* → Es una operación inválida (no hay
  movimientos), igual que las demás excepciones del sistema.
- *Nombrá los predicados privados y su uso* → tabla de la sección 3.

**TP4**
- *¿Qué hace y qué no hace `RegistroCentral`?* → tabla de la sección 4.1.
- *¿Por qué HashMap para envíos y lista para clientes?* → clave única en tiempo
  directo vs. práctica deliberada de búsqueda secuencial.
- *¿Dónde está la cola y cómo respeta el orden?* → `Queue<Envio>` en `Sucursal`;
  `offer` al final, `poll` desde el principio.
- *¿Y la regla "sin paquetes"?* → se mudó al constructor de `Envio`: el registro
  no valida reglas de negocio.
- *¿El registro guarda copias?* → No: los mismos objetos; una vez modificados,
  se refleja en todo el sistema.

---

## 6. Cómo estudiar en la práctica

1. **Leer el enunciado y el código en paralelo**: para cada parte del TP, buscar el
   comentario en el código que dice "Parte N del TP1" / "TP2" / "TP4".
2. **Correr la demo y seguir la salida**: `java -Dstdout.encoding=UTF-8 -cp classes App`
   — la salida está comentada por bloques (alta, cola, seguimiento, validaciones).
3. **Romperlo a propósito**: cambiar una transición, registrar un código repetido,
   despachar con la cola vacía, y leer el mensaje de error que sale.
4. **Autoexplicar en voz alta** el "por qué" de cada responsabilidad usando la
   fórmula de la sección 2 (Expert + Creator + Tell don't ask).

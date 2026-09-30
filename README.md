# Sistema de Gestión de Pedidos

## Unidad 6 — Diagnóstico y Refactorización de Antipatrones

## 1. Contexto

El proyecto corresponde a un servicio Spring Boot para la gestión y procesamiento de pedidos.

La implementación inicial concentraba en `GestorPedidos` diferentes responsabilidades relacionadas con:

* Validación del pedido.
* Validación de stock.
* Validación del cliente y control de mora.
* Cálculo del subtotal.
* Cálculo de descuentos.
* Cálculo de impuestos y total.
* Persistencia del pedido.
* Registro del detalle del pedido.
* Actualización del inventario.
* Construcción y envío de notificaciones.
* Registro de eventos mediante logs.

El objetivo de la actividad es identificar los antipatrones presentes en la implementación inicial y aplicar patrones de diseño que permitan separar responsabilidades, manteniendo el comportamiento observable del sistema.

La refactorización implementada utiliza:

* **Chain of Responsibility** para la secuencia de validaciones.
* **Strategy** para encapsular las reglas de descuento.
* **Repository** para separar la persistencia.
* Un servicio independiente para las notificaciones.
* `GestorPedidos` como orquestador del flujo principal.

---

# 2. Implementación inicial

La implementación inicial tenía como punto de entrada el método:

```java
public ResultadoPedido procesarPedido(PedidoRequest request)
```

Dentro del mismo método se ejecutaban directamente consultas SQL mediante `JdbcTemplate`, reglas de negocio, ciclos, condiciones, persistencia y construcción de correos.

Entre las operaciones realizadas se encontraban:

1. Validación de existencia de productos en el pedido.
2. Consulta y validación del stock.
3. Consulta del tipo de cliente.
4. Validación de clientes morosos.
5. Aplicación de una regla horaria para clientes con deuda.
6. Consulta de precios.
7. Cálculo del subtotal.
8. Determinación del descuento según el tipo de cliente.
9. Cálculo del impuesto.
10. Cálculo del total.
11. Inserción del pedido.
12. Inserción de los detalles.
13. Actualización del inventario.
14. Construcción de la notificación.
15. Envío del correo.
16. Registro mediante logs.

Esta concentración de responsabilidades constituye el punto de partida para el diagnóstico.

---

# 3. Diagnóstico de antipatrones

A partir de la revisión de la implementación inicial de `GestorPedidos`, se identificaron principalmente los siguientes antipatrones:

* **God Object**
* **Spaghetti Code**

El diagnóstico se realiza a partir de responsabilidades y estructuras concretas presentes en el código, y no únicamente mediante la asignación de un nombre al problema.

---

## 3.1 God Object

`GestorPedidos` presenta características de **God Object** porque una sola clase concentra responsabilidades que pertenecen a diferentes componentes de la aplicación.

### Responsabilidades identificadas

Se pueden agrupar las operaciones de `GestorPedidos` en seis conjuntos principales:

### 1. Validación

Incluye:

* Verificación de que el pedido tenga elementos.
* Consulta del stock.
* Validación de cantidades disponibles.
* Consulta de existencia del cliente.
* Consulta de deuda pendiente.
* Aplicación de la regla de horario para clientes morosos.

### 2. Cálculo de precios

Incluye:

* Consulta del precio de cada producto.
* Cálculo del subtotal.
* Determinación del porcentaje de descuento.
* Cálculo del subtotal después del descuento.
* Cálculo del impuesto.
* Cálculo del total.

### 3. Reglas específicas de descuentos

El método contiene directamente las reglas para clientes VIP y frecuentes:

```java
if ("VIP".equals(tipoCliente)) {
    if (subtotal > 1_000_000) {
        descuento = 0.15;
    } else if (subtotal > 500_000) {
        descuento = 0.10;
    } else {
        descuento = 0.05;
    }
} else if ("FRECUENTE".equals(tipoCliente)) {
    ...
}
```

### 4. Persistencia

El mismo método ejecuta directamente operaciones SQL para:

* Insertar el pedido.
* Obtener el identificador generado.
* Insertar los detalles del pedido.
* Actualizar el inventario.

Por ejemplo:

```java
jdbcTemplate.update(
    "INSERT INTO pedidos ...",
    ...
);
```

y:

```java
jdbcTemplate.update(
    "UPDATE inventario SET stock = stock - ? WHERE producto_id = ?",
    ...
);
```

### 5. Notificación

`GestorPedidos` también construye el asunto y el cuerpo del correo:

```java
String asunto = "Confirmacion de pedido #" + pedidoId;
StringBuilder cuerpo = new StringBuilder();
```

Posteriormente utiliza `EmailService` para enviar la notificación.

### 6. Registro y control del flujo

La clase también contiene operaciones de logging y controla directamente todos los caminos de ejecución del procesamiento del pedido.

### Evidencia del problema

Por lo anterior, una modificación en cualquiera de las siguientes áreas podría obligar a modificar `GestorPedidos`:

* reglas de validación;
* reglas de descuento;
* cálculo de impuestos;
* estructura de persistencia;
* actualización del inventario;
* formato de correo;
* mecanismo de notificación.

Esto demuestra que la clase posee múltiples razones para cambiar y concentra responsabilidades de diferentes niveles de la aplicación.

---

# 3.2 Spaghetti Code

Además de concentrar responsabilidades, el método `procesarPedido` presenta características de **Spaghetti Code**, debido a que diferentes caminos de ejecución, consultas SQL, ciclos y reglas de negocio se encuentran entrelazados dentro del mismo flujo.

## Anidamiento de la validación de mora

Una de las estructuras más representativas es la validación del cliente moroso:

```java
if ("MOROSO".equals(tipoCliente)) {

    Double deudaPendiente = jdbcTemplate.queryForObject(
        "SELECT SUM(monto) FROM facturas "
            + "WHERE cliente_id = ? AND pagada = false",
        Double.class,
        request.getClienteId()
    );

    if (deudaPendiente != null && deudaPendiente > 0) {

        LocalTime ahora = LocalTime.now();

        if (ahora.isBefore(LocalTime.of(20, 0))) {

            return ResultadoPedido.rechazado(
                "Cliente con deuda pendiente: $" + deudaPendiente
            );

        } else {
            // Se permite el pedido excepcionalmente
        }
    }
}
```

Este bloque alcanza **tres niveles de anidamiento condicional**:

1. Cliente moroso.
2. Existencia de deuda.
3. Validación del horario de corte.

La decisión de negocio se encuentra mezclada con la consulta SQL y con el control del flujo del pedido.

---

## Anidamiento del cálculo de descuentos

La lógica de descuento también presenta varios niveles de decisión:

```java
if ("VIP".equals(tipoCliente)) {

    if (subtotal > 1_000_000) {
        descuento = 0.15;

    } else if (subtotal > 500_000) {
        descuento = 0.10;

    } else {
        descuento = 0.05;
    }

} else if ("FRECUENTE".equals(tipoCliente)) {

    Integer pedidosPrevios = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM pedidos WHERE cliente_id = ?",
        Integer.class,
        request.getClienteId()
    );

    if (pedidosPrevios != null && pedidosPrevios > 10) {
        descuento = 0.08;

    } else if (pedidosPrevios != null && pedidosPrevios > 3) {
        descuento = 0.04;
    }
}
```

Aquí se combinan:

* Tipo de cliente.
* Rangos de subtotal.
* Cantidad de pedidos previos.
* Consultas SQL.
* Reglas específicas de descuento.

Esto aumenta los caminos posibles dentro del método y dificulta aislar cada regla para modificarla o probarla de manera independiente.

---

# 4. Evidencia de diferentes niveles de abstracción

El método `procesarPedido` trabaja simultáneamente en diferentes niveles de abstracción.

## 4.1 Acceso a datos

Se ejecutan consultas SQL directamente mediante `JdbcTemplate`:

```java
SELECT stock FROM inventario WHERE producto_id = ?
```

También se realizan operaciones de inserción:

```java
INSERT INTO pedidos
(cliente_id, subtotal, descuento, impuesto, total, fecha, estado)
VALUES (?, ?, ?, ?, ?, ?, ?)
```

Y actualización del inventario:

```java
UPDATE inventario
SET stock = stock - ?
WHERE producto_id = ?
```

---

## 4.2 Reglas de negocio

En el mismo flujo se encuentran reglas como:

* Un pedido sin productos debe rechazarse.
* Un pedido con stock insuficiente debe rechazarse.
* Un cliente inexistente debe rechazarse.
* Un cliente moroso con deuda pendiente es rechazado antes de las 20:00.
* Después de las 20:00 se permite excepcionalmente el pedido.
* Un cliente VIP obtiene descuentos de 5 %, 10 % o 15 % dependiendo del subtotal.
* Un cliente frecuente obtiene descuentos de 4 % u 8 % dependiendo de sus pedidos anteriores.
* El impuesto aplicado corresponde al 19 %.

---

## 4.3 Construcción de la notificación

El mismo método también trabaja con el formato textual del correo:

```java
String asunto = "Confirmacion de pedido #" + pedidoId;

StringBuilder cuerpo = new StringBuilder();

cuerpo.append("Estimado cliente,\n\n");
cuerpo.append("Su pedido ha sido confirmado.\n");
```

Por lo tanto, una única secuencia de código combina:

```text
SQL → reglas de negocio → cálculos → persistencia → texto de correo → notificación
```

La mezcla de estos niveles dificulta mantener, probar y modificar cada responsabilidad de manera independiente.

---

# 5. Impacto de agregar un nuevo tipo de cliente

En la implementación inicial, las reglas de descuento dependen directamente de condiciones como:

```java
if ("VIP".equals(tipoCliente)) {
    ...
} else if ("FRECUENTE".equals(tipoCliente)) {
    ...
}
```

Si se agregara un nuevo tipo de cliente, por ejemplo `CORPORATIVO`, sería necesario modificar directamente el método `procesarPedido`.

Además de agregar una nueva condición para `CORPORATIVO`, habría que incorporar dentro del mismo método las reglas específicas de cálculo de ese cliente.

Por ejemplo:

```java
else if ("CORPORATIVO".equals(tipoCliente)) {
    // nuevas reglas
}
```

Esto implica modificar código existente y aumentar el tamaño y la complejidad del método.

La consecuencia es un mayor acoplamiento entre `GestorPedidos` y las reglas específicas de cada tipo de cliente.

La refactorización mediante **Strategy** permite que una nueva regla de descuento se incorpore como una nueva implementación de la estrategia, reduciendo la necesidad de modificar el flujo principal de `GestorPedidos`.

---

# 6. Refactorización mediante patrones de diseño

A partir del diagnóstico se separaron las responsabilidades en componentes especializados.

La estructura resultante es:

```text
com.tienda.pedidos
├── dto
│   ├── PedidoRequest.java
│   ├── ItemPedido.java
│   └── ResultadoPedido.java
│
├── validacion
│   ├── ContextoPedido.java
│   ├── ValidadorPedido.java
│   ├── ValidadorStock.java
│   └── ValidadorCliente.java
│
├── descuento
│   ├── EstrategiaDescuento.java
│   ├── DescuentoVip.java
│   ├── DescuentoFrecuente.java
│   ├── DescuentoEstandar.java
│   └── SelectorEstrategiaDescuento.java
│
└── service
    ├── GestorPedidos.java
    ├── PedidoRepository.java
    ├── NotificacionPedidoService.java
    └── EmailService.java
```

---

# 7. Decisión de diseño: Chain of Responsibility

Se utilizó **Chain of Responsibility** para separar las validaciones del pedido.

La cadena está formada por:

```text
ValidadorStock
      ↓
ValidadorCliente
```

Cada validador tiene una responsabilidad concreta.

`ValidadorStock` verifica:

* Existencia de productos en el pedido.
* Disponibilidad del stock.

`ValidadorCliente` verifica:

* Existencia del cliente.
* Tipo de cliente.
* Deuda pendiente.
* Regla de horario para clientes morosos.

La cadena se construye mediante:

```java
this.validadorPedido =
        validadorStock.encadenar(validadorCliente);
```

El método de validación permite detener la ejecución cuando un validador rechaza el pedido:

```java
public void validar(ContextoPedido contexto) {

    validarActual(contexto);

    if (!contexto.fueRechazado()
            && siguiente != null) {

        siguiente.validar(contexto);
    }
}
```

Por ejemplo, si `ValidadorStock` detecta stock insuficiente, `ValidadorCliente` no se ejecuta.

Esto conserva el orden de validación existente en la implementación original.

---

## 7.1 Alternativa considerada

Se consideró utilizar una lista de:

```java
List<Predicate<ContextoPedido>>
```

con diferentes predicados de validación.

Sin embargo, Chain of Responsibility resulta más apropiado para este caso porque las validaciones tienen una dependencia real de orden y necesitan un mecanismo explícito de corte.

Si el stock es insuficiente, no tiene sentido continuar consultando y validando la situación del cliente.

La cadena permite que cada validador decida explícitamente si el procesamiento puede continuar hacia el siguiente elemento.

---

# 8. Decisión de diseño: Strategy

Se utilizó **Strategy** para encapsular las reglas de descuento.

La interfaz utilizada es:

```java
public interface EstrategiaDescuento {

    double calcular(
        double subtotal,
        Long clienteId,
        PedidoRequest request
    );
}
```

Las implementaciones son:

```text
EstrategiaDescuento
├── DescuentoVip
├── DescuentoFrecuente
└── DescuentoEstandar
```

La selección se realiza mediante:

```text
SelectorEstrategiaDescuento
```

El selector obtiene la estrategia correspondiente al tipo de cliente y utiliza `DescuentoEstandar` como comportamiento por defecto.

De esta manera, `GestorPedidos` ya no contiene directamente las reglas específicas de cada tipo de cliente.

---

## 8.1 Alternativa considerada

No se utilizó Chain of Responsibility para los descuentos porque estas reglas no representan una secuencia de validaciones.

En el caso de los descuentos, se necesita seleccionar una regla según el tipo de cliente:

```text
VIP          → DescuentoVip
FRECUENTE    → DescuentoFrecuente
otros        → DescuentoEstandar
```

Las estrategias no dependen de un orden de ejecución entre ellas y no necesitan que una estrategia delegue a otra.

Por esta razón, Strategy proporciona una separación más directa de las reglas de descuento.

---

# 9. Separación de persistencia

La persistencia fue trasladada a:

```text
PedidoRepository
```

Esta clase concentra las operaciones relacionadas con:

* Inserción del pedido.
* Obtención del identificador.
* Inserción del detalle.
* Actualización del inventario.

El objetivo es evitar que `GestorPedidos` conozca directamente todos los detalles de las consultas SQL utilizadas para almacenar el pedido.

`GestorPedidos` únicamente solicita al repositorio la operación:

```java
pedidoRepository.guardarPedido(
    request.getClienteId(),
    subtotal,
    descuento,
    impuesto,
    total,
    request.getItems()
);
```

---

# 10. Separación de notificaciones

La construcción y envío de la confirmación se trasladó a:

```text
NotificacionPedidoService
```

Esta clase es responsable de:

1. Construir el asunto.
2. Construir el cuerpo del correo.
3. Utilizar `EmailService`.
4. Registrar el error si el envío falla.

De esta manera, `GestorPedidos` no necesita conocer los detalles de construcción del mensaje.

---

# 11. Responsabilidad final de GestorPedidos

Después de la refactorización, `GestorPedidos` actúa principalmente como **orquestador**.

El flujo principal es:

```text
PedidoRequest
     ↓
Chain of Responsibility
     ↓
Validación
     ↓
Cálculo del subtotal
     ↓
SelectorEstrategiaDescuento
     ↓
Strategy de descuento
     ↓
Impuesto y total
     ↓
PedidoRepository
     ↓
NotificacionPedidoService
     ↓
ResultadoPedido
```

El método `procesarPedido` coordina las operaciones sin contener directamente todas las reglas de validación, descuento, persistencia y notificación.

---

# 12. Pruebas de integración

Se cuenta con la clase:

```text
GestorPedidosIntegrationTest
```

Las pruebas verifican los principales comportamientos del sistema:

### Stock insuficiente

Se verifica que un pedido sea rechazado cuando la cantidad solicitada supera el stock disponible.

### Cliente moroso antes del horario de corte

Se verifica el rechazo de un cliente con deuda pendiente antes de las 20:00.

### Cliente moroso después del horario de corte

Se verifica el comportamiento excepcional después de las 20:00.

### Cliente inexistente

Se verifica que un cliente que no existe sea rechazado.

### Cliente VIP

Se verifica la aplicación del descuento correspondiente.

Para un subtotal de:

```text
6 × $100.000 = $600.000
```

se aplica el descuento VIP del 10 % y posteriormente el impuesto del 19 %.

El total esperado es:

```text
$642.600
```

### Cliente frecuente

Se crean cuatro pedidos previos para activar el descuento del 4 %.

Para un pedido de:

```text
$100.000
```

el cálculo esperado es:

```text
$100.000 - 4 % = $96.000
$96.000 + 19 % = $114.240
```

---

# 13. Compatibilidad con H2

Durante la ejecución de las pruebas de integración se presentó un problema con la sentencia:

```sql
CALL IDENTITY()
```

La versión de H2 utilizada actualmente en el proyecto no reconoce esta función mediante dicha sentencia.

Por esta razón, en `PedidoRepository` se utiliza:

```sql
SELECT MAX(id) FROM pedidos
```

para obtener el identificador generado después de insertar el pedido.

La modificación permite mantener la ejecución del ejercicio con la versión actual de H2 y las pruebas de integración finalizan correctamente.

Esta solución se utiliza como adaptación al entorno del ejercicio. En un escenario productivo con concurrencia, la obtención del identificador debería realizarse mediante un mecanismo de generación y recuperación de claves apropiado para la base de datos utilizada.

---

# 14. Resultado de la refactorización

La implementación final separa las responsabilidades de acuerdo con las necesidades identificadas durante el diagnóstico:

| Responsabilidad               | Componente                    |
| ----------------------------- | ----------------------------- |
| Validación de stock           | `ValidadorStock`              |
| Validación del cliente y mora | `ValidadorCliente`            |
| Coordinación de validaciones  | `ValidadorPedido`             |
| Contexto de validación        | `ContextoPedido`              |
| Descuento VIP                 | `DescuentoVip`                |
| Descuento frecuente           | `DescuentoFrecuente`          |
| Descuento estándar            | `DescuentoEstandar`           |
| Selección del descuento       | `SelectorEstrategiaDescuento` |
| Persistencia                  | `PedidoRepository`            |
| Notificación                  | `NotificacionPedidoService`   |
| Flujo general                 | `GestorPedidos`               |

La refactorización mantiene el comportamiento funcional esperado y reduce la concentración de responsabilidades que existía inicialmente en `GestorPedidos`.

---

# 15. Verificación

La aplicación se verifica mediante:

```powershell
mvn clean test
```

El resultado esperado es:

```text
BUILD SUCCESS
```

Las pruebas permiten comprobar que el comportamiento principal del sistema se conserva después de aplicar los patrones de diseño.

---

# 16. Conclusión

El diagnóstico permitió identificar que la implementación inicial de `GestorPedidos` concentraba múltiples responsabilidades y mezclaba diferentes niveles de abstracción, características asociadas a los antipatrones **God Object** y **Spaghetti Code**.

La refactorización separó las responsabilidades mediante **Chain of Responsibility** para las validaciones y **Strategy** para las reglas de descuento. Además, se utilizaron `PedidoRepository` y `NotificacionPedidoService` para separar persistencia y notificación.

Como resultado, `GestorPedidos` queda reducido principalmente a la coordinación del flujo del pedido, mientras que las reglas y operaciones específicas se encuentran encapsuladas en componentes especializados.

La solución mantiene el comportamiento funcional del sistema y facilita la incorporación de nuevas validaciones y estrategias de descuento sin aumentar innecesariamente la complejidad del método principal.

# Sistema de Gestión de Pedidos

## Unidad 6 — Diagnóstico y Refactorización de Antipatrones

### Contexto

El proyecto corresponde a un servicio Spring Boot para la gestión y procesamiento de pedidos. La lógica principal se encuentra inicialmente concentrada en la clase `GestorPedidos`, donde se realizan diferentes operaciones relacionadas con la validación, cálculo de valores, persistencia y notificación del pedido.

El objetivo de esta actividad es identificar los problemas de diseño presentes en la implementación inicial y posteriormente aplicar patrones de diseño que permitan separar responsabilidades, manteniendo el comportamiento observable del sistema.

---

## Diagnóstico inicial

A partir de la revisión del código de `GestorPedidos`, se identifican principalmente los siguientes antipatrones:

### 1. God Object

La clase `GestorPedidos` concentra múltiples responsabilidades que pertenecen a diferentes niveles de la aplicación.

Entre las responsabilidades identificadas se encuentran:

* Validar que el pedido contenga productos.
* Consultar y validar el stock disponible.
* Consultar la información del cliente.
* Validar clientes con deuda pendiente.
* Aplicar la regla de horario para clientes morosos.
* Calcular el subtotal del pedido.
* Consultar precios de los productos.
* Determinar el porcentaje de descuento.
* Calcular impuestos y total.
* Registrar el pedido en la base de datos.
* Registrar el detalle del pedido.
* Actualizar el inventario.
* Construir el contenido del correo electrónico.
* Enviar la notificación al cliente.
* Registrar información mediante logs.

Esto evidencia que `GestorPedidos` tiene múltiples razones para cambiar. Por ejemplo, una modificación en las reglas de descuento, en la estructura de persistencia o en el formato de las notificaciones obligaría a modificar la misma clase.

### 2. Spaghetti Code

La lógica del método `procesarPedido` combina consultas SQL, ciclos, validaciones, condiciones anidadas, cálculos y operaciones de persistencia dentro de un único flujo de ejecución.

Un ejemplo se encuentra en la validación de clientes morosos:

```java
if ("MOROSO".equals(tipoCliente)) {
    ...
    if (deudaPendiente != null && deudaPendiente > 0) {
        ...
        if (ahora.isBefore(LocalTime.of(20, 0))) {
            ...
        } else {
            ...
        }
    }
}
```

También se presenta una estructura condicional para determinar los descuentos:

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

Estas condiciones hacen que el método tenga diferentes caminos de ejecución y dificultan incorporar nuevas reglas sin modificar la lógica existente.

---

## Evidencia de diferentes niveles de abstracción

Dentro del mismo método se mezclan diferentes niveles de abstracción.

### Acceso a datos

Se ejecutan consultas SQL directamente mediante `JdbcTemplate`, por ejemplo:

```java
SELECT stock FROM inventario WHERE producto_id = ?
```

y:

```java
INSERT INTO pedidos
(cliente_id, subtotal, descuento, impuesto, total, fecha, estado)
VALUES (?, ?, ?, ?, ?, ?, ?)
```

### Reglas de negocio

En el mismo método se encuentran reglas como:

* Un cliente moroso con deuda pendiente es rechazado antes de las 20:00.
* Después de las 20:00 se permite excepcionalmente el pedido.
* Un cliente VIP puede obtener descuentos de 5 %, 10 % o 15 %.
* Un cliente frecuente puede obtener descuentos de 4 % u 8 %.
* El impuesto aplicado es del 19 %.

### Construcción y envío de notificaciones

También se construye directamente el contenido del correo:

```java
String asunto = "Confirmacion de pedido #" + pedidoId;
StringBuilder cuerpo = new StringBuilder();
```

Posteriormente se utiliza `EmailService` para realizar el envío.

La mezcla de estos niveles dentro de `GestorPedidos` dificulta mantener y probar cada responsabilidad de forma independiente.

---

## Impacto de agregar un nuevo tipo de cliente

Actualmente las reglas de descuento dependen de condiciones como:

```java
if ("VIP".equals(tipoCliente)) {
    ...
} else if ("FRECUENTE".equals(tipoCliente)) {
    ...
}
```

Por lo tanto, para agregar un nuevo tipo de cliente sería necesario modificar directamente el método `procesarPedido` y añadir otra condición.

Además, si el nuevo tipo requiere reglas adicionales, la cantidad de condiciones dentro del método aumentaría.

Esto incrementa el acoplamiento entre `GestorPedidos` y las reglas específicas de cada tipo de cliente.

---

# Decisiones de diseño

## Chain of Responsibility para las validaciones

Se utilizará **Chain of Responsibility** para separar las diferentes validaciones que debe superar un pedido.

La cadena estará compuesta por validadores independientes, entre ellos:

* `ValidadorStock`
* `ValidadorCliente`

Cada validador tendrá una responsabilidad concreta y podrá rechazar el pedido o delegar la ejecución al siguiente elemento de la cadena.

Esta estructura permite mantener explícito el orden de las validaciones y detener el procesamiento cuando una regla no se cumple.

### Alternativa considerada

Una alternativa sería utilizar una lista de `Predicate<ContextoPedido>`.

Sin embargo, la cadena permite representar de manera más explícita la responsabilidad de cada validación, el orden de ejecución y el punto en el que puede detenerse el procesamiento.

---

## Strategy para los descuentos

Se utilizará **Strategy** para separar las reglas de descuento según el tipo de cliente.

Las estrategias serán:

* `DescuentoVip`
* `DescuentoFrecuente`
* `DescuentoEstandar`

La selección de la estrategia será realizada mediante `SelectorEstrategiaDescuento`.

Esta solución permite agregar nuevas estrategias de descuento sin convertir `GestorPedidos` en una estructura cada vez más grande de condiciones `if/else`.

### Alternativa considerada

Chain of Responsibility no resulta apropiado para esta parte porque las reglas de descuento representan alternativas según el tipo de cliente y no una secuencia de validaciones que deban ejecutarse una después de otra.

Por esta razón, Strategy resulta más adecuado para encapsular las diferentes formas de cálculo del descuento.

---

## Separación de persistencia

La persistencia de los pedidos se trasladará a:

```text
PedidoRepository
```

Esta clase será responsable de ejecutar las operaciones relacionadas con la base de datos de pedidos, detalles e inventario.

De esta manera, `GestorPedidos` no tendrá que conocer directamente todos los detalles de las operaciones SQL.

---

## Separación de notificaciones

La construcción y envío de la notificación se trasladará a:

```text
NotificacionPedidoService
```

Esta clase será responsable de construir el mensaje de confirmación y utilizar `EmailService` para enviarlo.

Así se evita que `GestorPedidos` tenga que manejar directamente el formato del correo.

---

## Responsabilidad final de GestorPedidos

Después de la refactorización, `GestorPedidos` tendrá principalmente una función de **orquestación**.

Su responsabilidad será coordinar:

1. La validación del pedido.
2. El cálculo del subtotal.
3. La selección de la estrategia de descuento.
4. El cálculo del impuesto y total.
5. La persistencia del pedido.
6. La notificación al cliente.

La intención es reducir la cantidad de responsabilidades concentradas en una sola clase y mejorar la mantenibilidad del código sin modificar el comportamiento funcional esperado.

---

## Corrección de compatibilidad con H2

Durante la ejecución inicial de las pruebas de integración se presentó un error con:

```sql
CALL IDENTITY()
```

La versión de H2 utilizada por el proyecto no reconoce esta función mediante dicha sentencia.

Para mantener la ejecución de las pruebas se sustituyó por:

```sql
SELECT MAX(id) FROM pedidos
```

Con esta modificación, las pruebas de integración finalizaron correctamente con:

```text
BUILD SUCCESS
```

Esta modificación corresponde a una corrección de compatibilidad con la versión de H2 utilizada en el entorno del ejercicio.

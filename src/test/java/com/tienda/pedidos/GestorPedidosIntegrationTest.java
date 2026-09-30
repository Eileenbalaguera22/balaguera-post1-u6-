package com.tienda.pedidos;
import com.tienda.pedidos.service.GestorPedidos;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GestorPedidosIntegrationTest {

    @Autowired
    private GestorPedidos gestorPedidos;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararDatos() {

        jdbcTemplate.update("DELETE FROM detalle_pedido");
        jdbcTemplate.update("DELETE FROM pedidos");

        jdbcTemplate.update(
                "UPDATE inventario SET stock = 10 WHERE producto_id = 101"
        );

        jdbcTemplate.update(
                "UPDATE inventario SET stock = 5 WHERE producto_id = 102"
        );

        jdbcTemplate.update(
                "UPDATE inventario SET stock = 20 WHERE producto_id = 103"
        );
    }

    private PedidoRequest crearPedido(
            Long clienteId,
            Long productoId,
            int cantidad) {

        ItemPedido item = new ItemPedido();
        item.setProductoId(productoId);
        item.setCantidad(cantidad);

        PedidoRequest request = new PedidoRequest();
        request.setClienteId(clienteId);
        request.setClienteEmail("prueba@correo.com");
        request.setItems(List.of(item));

        return request;
    }

    @Test
    void debeRechazarPedidoPorStockInsuficiente() {

        PedidoRequest request = crearPedido(1L, 101L, 15);

        ResultadoPedido resultado =
                gestorPedidos.procesarPedido(request);

        assertFalse(resultado.isConfirmado());
        assertTrue(
                resultado.getMotivoRechazo()
                        .contains("Stock insuficiente")
        );
    }

    @Test
    void debeRechazarClienteMorosoAntesDeLasOcho() {

        // Esta prueba solo espera rechazo si se ejecuta antes de las 20:00.
        if (java.time.LocalTime.now()
                .isBefore(java.time.LocalTime.of(20, 0))) {

            PedidoRequest request = crearPedido(3L, 101L, 1);

            ResultadoPedido resultado =
                    gestorPedidos.procesarPedido(request);

            assertFalse(resultado.isConfirmado());
            assertTrue(
                    resultado.getMotivoRechazo()
                            .contains("deuda pendiente")
            );
        }
    }

    @Test
    void debeProcesarClienteMorosoFueraDelHorarioDeCorte() {

        // Esta prueba solo ejecuta el caso excepcional después de las 20:00.
        if (!java.time.LocalTime.now()
                .isBefore(java.time.LocalTime.of(20, 0))) {

            PedidoRequest request = crearPedido(3L, 101L, 1);

            ResultadoPedido resultado =
                    gestorPedidos.procesarPedido(request);

            assertTrue(resultado.isConfirmado());
        }
    }

    @Test
    void debeRechazarClienteInexistente() {

        PedidoRequest request = crearPedido(99L, 101L, 1);

        ResultadoPedido resultado =
                gestorPedidos.procesarPedido(request);

        assertFalse(resultado.isConfirmado());
        assertEquals(
                "Cliente no registrado",
                resultado.getMotivoRechazo()
        );
    }

    @Test
    void debeAplicarDescuentoVip() {

        // 6 unidades x $100.000 = $600.000.
        // El descuento VIP esperado es del 10 %.
        PedidoRequest request = crearPedido(1L, 101L, 6);

        ResultadoPedido resultado =
                gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());

        // $600.000 - 10 % + 19 % de impuesto sobre el subtotal descontado.
        assertEquals(
                642600.0,
                resultado.getTotal(),
                0.01
        );
    }

    @Test
    void debeProcesarClienteFrecuente() {

        // Se crean cuatro pedidos previos para activar el 4 %.
        for (int i = 0; i < 4; i++) {
            jdbcTemplate.update(
                    "INSERT INTO pedidos "
                            + "(cliente_id, subtotal, descuento, impuesto, "
                            + "total, fecha, estado) "
                            + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)",
                    2L, 1000.0, 0.0, 190.0, 1190.0, "CONFIRMADO"
            );
        }

        PedidoRequest request = crearPedido(2L, 101L, 1);

        ResultadoPedido resultado =
                gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());

        // $100.000 - 4 % = $96.000.
        // $96.000 + 19 % de impuesto = $114.240.
        assertEquals(
                114240.0,
                resultado.getTotal(),
                0.01
        );
    }
}
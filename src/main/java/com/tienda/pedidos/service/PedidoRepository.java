package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.ItemPedido;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PedidoRepository {

    private final JdbcTemplate jdbcTemplate;

    public PedidoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long guardarPedido(
            Long clienteId,
            double subtotal,
            double descuento,
            double impuesto,
            double total,
            List<ItemPedido> items) {

        jdbcTemplate.update(
                "INSERT INTO pedidos "
                        + "(cliente_id, subtotal, descuento, impuesto, "
                        + "total, fecha, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                clienteId,
                subtotal,
                descuento,
                impuesto,
                total,
                Timestamp.valueOf(LocalDateTime.now()),
                "CONFIRMADO"
        );

        /*
         * Se mantiene esta consulta porque la versión de H2
         * utilizada actualmente en el proyecto no reconoce
         * CALL IDENTITY().
         */
        Long pedidoId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM pedidos",
                Long.class
        );

        for (ItemPedido item : items) {

            jdbcTemplate.update(
                    "INSERT INTO detalle_pedido "
                            + "(pedido_id, producto_id, cantidad) "
                            + "VALUES (?, ?, ?)",
                    pedidoId,
                    item.getProductoId(),
                    item.getCantidad()
            );

            jdbcTemplate.update(
                    "UPDATE inventario "
                            + "SET stock = stock - ? "
                            + "WHERE producto_id = ?",
                    item.getCantidad(),
                    item.getProductoId()
            );
        }

        return pedidoId;
    }
}
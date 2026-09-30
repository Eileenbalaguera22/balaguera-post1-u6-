package com.tienda.pedidos.descuento;

import com.tienda.pedidos.dto.PedidoRequest;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("descuentoFrecuente")
public class DescuentoFrecuente implements EstrategiaDescuento {

    private final JdbcTemplate jdbcTemplate;

    public DescuentoFrecuente(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public double calcular(
            double subtotal,
            Long clienteId,
            PedidoRequest request) {

        Integer pedidosPrevios = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pedidos WHERE cliente_id = ?",
                Integer.class,
                clienteId
        );

        if (pedidosPrevios != null && pedidosPrevios > 10) {
            return 0.08;
        }

        if (pedidosPrevios != null && pedidosPrevios > 3) {
            return 0.04;
        }

        return 0.0;
    }
}
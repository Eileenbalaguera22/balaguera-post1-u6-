package com.tienda.pedidos.descuento;

import com.tienda.pedidos.dto.PedidoRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DescuentoCorporativo implements EstrategiaDescuento {

    private final JdbcTemplate jdbcTemplate;

    public DescuentoCorporativo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public double calcular(
            double subtotal,
            Long clienteId,
            PedidoRequest request) {

        Integer cantidad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clientes "
                        + "WHERE id = ? AND nit IS NOT NULL "
                        + "AND TRIM(nit) <> ''",
                Integer.class,
                clienteId
        );

        return cantidad != null && cantidad > 0 ? 0.10 : 0.0;
    }
}
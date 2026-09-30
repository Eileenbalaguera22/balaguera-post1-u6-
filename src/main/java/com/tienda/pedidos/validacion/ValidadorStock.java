package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.ResultadoPedido;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ValidadorStock extends ValidadorPedido {

    private static final Logger log =
            LoggerFactory.getLogger(ValidadorStock.class);

    private final JdbcTemplate jdbcTemplate;

    public ValidadorStock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void validarActual(ContextoPedido contexto) {

        if (contexto.getRequest().getItems() == null
                || contexto.getRequest().getItems().isEmpty()) {

            log.warn(
                    "Pedido rechazado: sin items. Cliente {}",
                    contexto.getRequest().getClienteId()
            );

            contexto.rechazar(
                    ResultadoPedido.rechazado(
                            "El pedido no contiene items"
                    )
            );

            return;
        }

        for (ItemPedido item : contexto.getRequest().getItems()) {

            Integer stockDisponible = jdbcTemplate.queryForObject(
                    "SELECT stock FROM inventario WHERE producto_id = ?",
                    Integer.class,
                    item.getProductoId()
            );

            if (stockDisponible == null
                    || stockDisponible < item.getCantidad()) {

                log.warn(
                        "Stock insuficiente para producto {}",
                        item.getProductoId()
                );

                contexto.rechazar(
                        ResultadoPedido.rechazado(
                                "Stock insuficiente: producto "
                                        + item.getProductoId()
                        )
                );

                return;
            }
        }
    }
}
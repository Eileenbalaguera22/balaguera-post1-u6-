package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ResultadoPedido;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class ValidadorCliente extends ValidadorPedido {

    private static final Logger log =
            LoggerFactory.getLogger(ValidadorCliente.class);

    private final JdbcTemplate jdbcTemplate;

    public ValidadorCliente(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void validarActual(ContextoPedido contexto) {

        Long clienteId = contexto.getRequest().getClienteId();

        String tipoCliente;

        try {

            tipoCliente = jdbcTemplate.queryForObject(
                    "SELECT tipo_cliente FROM clientes WHERE id = ?",
                    String.class,
                    clienteId
            );

        } catch (EmptyResultDataAccessException e) {

            log.warn(
                    "Cliente no encontrado: {}",
                    clienteId
            );

            contexto.rechazar(
                    ResultadoPedido.rechazado(
                            "Cliente no registrado"
                    )
            );

            return;
        }

        contexto.setTipoCliente(tipoCliente);

        if ("MOROSO".equals(tipoCliente)) {

            Double deudaPendiente = jdbcTemplate.queryForObject(
                    "SELECT SUM(monto) FROM facturas "
                            + "WHERE cliente_id = ? AND pagada = false",
                    Double.class,
                    clienteId
            );

            if (deudaPendiente != null && deudaPendiente > 0) {

                LocalTime ahora = LocalTime.now();

                if (ahora.isBefore(LocalTime.of(20, 0))) {

                    log.warn(
                            "Cliente moroso con deuda pendiente: {}",
                            deudaPendiente
                    );

                    contexto.rechazar(
                            ResultadoPedido.rechazado(
                                    "Cliente con deuda pendiente: $"
                                            + deudaPendiente
                            )
                    );

                    return;

                } else {

                    log.info(
                            "Cliente moroso fuera del horario de corte; "
                                    + "se permite el pedido excepcionalmente"
                    );
                }
            }
        }
    }
}
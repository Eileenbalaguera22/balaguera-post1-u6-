package com.tienda.pedidos.service;


import com.tienda.pedidos.descuento.CalculadorDescuentoFinal;
import com.tienda.pedidos.descuento.EstrategiaDescuento;
import com.tienda.pedidos.descuento.SelectorEstrategiaDescuento;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.ValidadorCliente;
import com.tienda.pedidos.validacion.ValidadorPedido;
import com.tienda.pedidos.validacion.ValidadorStock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GestorPedidos {

    private static final Logger log =
            LoggerFactory.getLogger(GestorPedidos.class);

    private final JdbcTemplate jdbcTemplate;
    private final ValidadorPedido validadorPedido;
    private final CalculadorDescuentoFinal calculadorDescuentoFinal;
    private final PedidoRepository pedidoRepository;
    private final NotificacionPedidoService notificacionPedidoService;

    public GestorPedidos(
            JdbcTemplate jdbcTemplate,
            ValidadorStock validadorStock,
            ValidadorCliente validadorCliente,
            CalculadorDescuentoFinal calculadorDescuentoFinal,
            PedidoRepository pedidoRepository,
            NotificacionPedidoService notificacionPedidoService) {

        this.jdbcTemplate = jdbcTemplate;
        this.calculadorDescuentoFinal = calculadorDescuentoFinal;
        this.pedidoRepository = pedidoRepository;
        this.notificacionPedidoService = notificacionPedidoService;

        

        this.validadorPedido =
                validadorStock.encadenar(validadorCliente);
    }

    public ResultadoPedido procesarPedido(PedidoRequest request) {

        log.info(
                "Iniciando procesamiento de pedido para cliente {}",
                request.getClienteId()
        );

        // 1. Validaciones
        ContextoPedido contexto =
                new ContextoPedido(request);

      
        validadorPedido.validar(contexto);

        if (contexto.fueRechazado()) {
            return contexto.getResultado();
        }

        // 2. Calcular subtotal
        double subtotal = calcularSubtotal(request);

        // 3. Calcular el mayor descuento entre el cliente y las campañas
        double descuento = calculadorDescuentoFinal.calcular(
        subtotal,
        request.getClienteId(),
        contexto.getTipoCliente(),
        request
);
        // 4. Calcular impuesto y total
        double subtotalConDescuento =
                subtotal - (subtotal * descuento);

        double impuesto =
                subtotalConDescuento * 0.19;

        double total =
                subtotalConDescuento + impuesto;

        // 5. Persistir pedido
        Long pedidoId =
                pedidoRepository.guardarPedido(
                        request.getClienteId(),
                        subtotal,
                        descuento,
                        impuesto,
                        total,
                        request.getItems()
                );

        // 6. Notificar
        notificacionPedidoService.enviarConfirmacion(
                request.getClienteEmail(),
                pedidoId,
                subtotal,
                descuento,
                impuesto,
                total
        );

        log.info(
                "Pedido {} confirmado. Total: {}",
                pedidoId,
                total
        );

        return ResultadoPedido.confirmado(
                pedidoId,
                total
        );
    }

    private double calcularSubtotal(PedidoRequest request) {

        double subtotal = 0;

        for (ItemPedido item : request.getItems()) {

            Double precioUnitario =
                    jdbcTemplate.queryForObject(
                            "SELECT precio FROM productos WHERE id = ?",
                            Double.class,
                            item.getProductoId()
                    );

            subtotal +=
                    precioUnitario * item.getCantidad();
        }

        return subtotal;
    }
}
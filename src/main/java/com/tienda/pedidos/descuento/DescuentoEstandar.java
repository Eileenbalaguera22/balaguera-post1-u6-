package com.tienda.pedidos.descuento;

import com.tienda.pedidos.dto.PedidoRequest;

import org.springframework.stereotype.Component;

@Component("descuentoEstandar")
public class DescuentoEstandar implements EstrategiaDescuento {

    @Override
    public double calcular(
            double subtotal,
            Long clienteId,
            PedidoRequest request) {

        return 0.0;
    }
}
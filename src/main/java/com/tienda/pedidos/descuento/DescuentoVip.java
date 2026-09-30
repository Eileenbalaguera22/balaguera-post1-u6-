package com.tienda.pedidos.descuento;

import com.tienda.pedidos.dto.PedidoRequest;

import org.springframework.stereotype.Component;

@Component("descuentoVip")
public class DescuentoVip implements EstrategiaDescuento {

    @Override
    public double calcular(
            double subtotal,
            Long clienteId,
            PedidoRequest request) {

        if (subtotal > 1_000_000) {
            return 0.15;
        }

        if (subtotal > 500_000) {
            return 0.10;
        }

        return 0.05;
    }
}
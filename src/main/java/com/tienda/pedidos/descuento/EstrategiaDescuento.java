package com.tienda.pedidos.descuento;

import com.tienda.pedidos.dto.PedidoRequest;

public interface EstrategiaDescuento {

    double calcular(
            double subtotal,
            Long clienteId,
            PedidoRequest request
    );
}
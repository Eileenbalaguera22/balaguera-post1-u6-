package com.tienda.pedidos.descuento;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import org.springframework.stereotype.Component;

@Component
public class DescuentoVolumen implements EstrategiaDescuento {

    @Override
    public double calcular(
            double subtotal,
            Long clienteId,
            PedidoRequest request) {

        if (request.getItems() == null) {
            return 0.0;
        }

        int unidades = 0;

        for (ItemPedido item : request.getItems()) {
            unidades += item.getCantidad();
        }

        return unidades > 20 ? 0.12 : 0.0;
    }
}
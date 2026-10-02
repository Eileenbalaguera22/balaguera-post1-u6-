package com.tienda.pedidos.descuento;

import com.tienda.pedidos.dto.PedidoRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DescuentoBlackFriday implements EstrategiaDescuento {

    private final boolean activa;

    public DescuentoBlackFriday(
            @Value("${promo.black-friday.activa:false}")
            boolean activa) {
        this.activa = activa;
    }

    @Override
    public double calcular(
            double subtotal,
            Long clienteId,
            PedidoRequest request) {

        return activa ? 0.25 : 0.0;
    }
}
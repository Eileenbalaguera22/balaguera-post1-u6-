package com.tienda.pedidos.descuento;

import com.tienda.pedidos.dto.PedidoRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CalculadorDescuentoFinal {

    private final SelectorEstrategiaDescuento selectorEstrategiaDescuento;
    private final List<EstrategiaDescuento> campanas;

    public CalculadorDescuentoFinal(
            SelectorEstrategiaDescuento selectorEstrategiaDescuento,
            DescuentoBlackFriday descuentoBlackFriday,
            DescuentoCorporativo descuentoCorporativo,
            DescuentoVolumen descuentoVolumen) {

        this.selectorEstrategiaDescuento = selectorEstrategiaDescuento;

        this.campanas = List.of(
                descuentoBlackFriday,
                descuentoCorporativo,
                descuentoVolumen
        );
    }

    public double calcular(
            double subtotal,
            Long clienteId,
            String tipoCliente,
            PedidoRequest request) {

        EstrategiaDescuento estrategiaCliente =
                selectorEstrategiaDescuento.seleccionar(tipoCliente);

        double descuentoFinal = estrategiaCliente.calcular(
                subtotal,
                clienteId,
                request
        );

        for (EstrategiaDescuento campana : campanas) {
            double descuentoCampana = campana.calcular(
                    subtotal,
                    clienteId,
                    request
            );

            descuentoFinal = Math.max(
                    descuentoFinal,
                    descuentoCampana
            );
        }

        return descuentoFinal;
    }
}
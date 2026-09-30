package com.tienda.pedidos.descuento;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SelectorEstrategiaDescuento {

    private final Map<String, EstrategiaDescuento> estrategias;

    public SelectorEstrategiaDescuento(
            Map<String, EstrategiaDescuento> estrategias) {

        this.estrategias = estrategias;
    }

    public EstrategiaDescuento seleccionar(String tipoCliente) {

        if (tipoCliente == null) {
            return estrategias.get("descuentoEstandar");
        }

        return estrategias.getOrDefault(
                "descuento" + capitalizar(tipoCliente),
                estrategias.get("descuentoEstandar")
        );
    }

    private String capitalizar(String texto) {

        if (texto.isEmpty()) {
            return texto;
        }

        return texto.substring(0, 1).toUpperCase()
                + texto.substring(1).toLowerCase();
    }
}
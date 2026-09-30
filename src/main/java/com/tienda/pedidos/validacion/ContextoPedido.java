package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;

public class ContextoPedido {

    private final PedidoRequest request;
    private String tipoCliente;
    private ResultadoPedido resultado;

    public ContextoPedido(PedidoRequest request) {
        this.request = request;
    }

    public PedidoRequest getRequest() {
        return request;
    }

    public String getTipoCliente() {
        return tipoCliente;
    }

    public void setTipoCliente(String tipoCliente) {
        this.tipoCliente = tipoCliente;
    }

    public ResultadoPedido getResultado() {
        return resultado;
    }

    public void rechazar(ResultadoPedido resultado) {
        this.resultado = resultado;
    }

    public boolean fueRechazado() {
        return resultado != null;
    }
}
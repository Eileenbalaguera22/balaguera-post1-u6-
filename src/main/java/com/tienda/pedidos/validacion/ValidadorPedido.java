package com.tienda.pedidos.validacion;

public abstract class ValidadorPedido {

    private ValidadorPedido siguiente;

    public ValidadorPedido encadenar(ValidadorPedido siguiente) {
        this.siguiente = siguiente;
        return this;
    }

    public void validar(ContextoPedido contexto) {

        validarActual(contexto);

        if (!contexto.fueRechazado()
                && siguiente != null) {

            siguiente.validar(contexto);
        }
    }

    protected abstract void validarActual(
            ContextoPedido contexto
    );
}
package com.miapp.pedido.excepciones;

public class PedidoNoEncontradoException extends RuntimeException {

    public PedidoNoEncontradoException(Long id) {
        super("No existe un pedido con id " + id);
    }

}

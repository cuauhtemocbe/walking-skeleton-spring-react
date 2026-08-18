package com.miapp.pedido.excepciones;

public class CantidadInvalidaException extends RuntimeException {

    public CantidadInvalidaException(Integer cantidad) {
        super("La cantidad debe ser mayor a cero: " + cantidad);
    }

}

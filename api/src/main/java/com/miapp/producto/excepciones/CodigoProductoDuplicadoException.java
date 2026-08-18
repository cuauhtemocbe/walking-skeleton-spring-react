package com.miapp.producto.excepciones;

public class CodigoProductoDuplicadoException extends RuntimeException {

    public CodigoProductoDuplicadoException(String codigo) {
        super("Ya existe un producto con código " + codigo);
    }

}

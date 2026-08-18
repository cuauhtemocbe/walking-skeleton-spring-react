package com.miapp.producto.excepciones;

public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException(Long id) {
        super("No existe un producto con id " + id);
    }

}

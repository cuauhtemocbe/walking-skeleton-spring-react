package com.miapp.cliente.excepciones;

public class ClienteNoEncontradoException extends RuntimeException {

    public ClienteNoEncontradoException(Long id) {
        super("No existe un cliente con id " + id);
    }

}

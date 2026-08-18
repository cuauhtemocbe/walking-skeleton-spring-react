package com.miapp.cliente.excepciones;

public class RfcDuplicadoException extends RuntimeException {

    public RfcDuplicadoException(String rfc) {
        super("Ya existe un cliente con RFC " + rfc);
    }

}

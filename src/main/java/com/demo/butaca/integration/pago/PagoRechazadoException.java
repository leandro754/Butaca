package com.demo.butaca.integration.pago;

public class PagoRechazadoException extends IllegalStateException {

    public PagoRechazadoException(String message) {
        super(message);
    }
}

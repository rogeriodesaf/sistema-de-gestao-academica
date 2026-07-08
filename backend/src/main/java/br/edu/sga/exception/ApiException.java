package br.edu.sga.exception;

import jakarta.ws.rs.core.Response;

public class ApiException extends RuntimeException {
    public final Response.Status status;

    public ApiException(Response.Status status, String mensagem) {
        super(mensagem);
        this.status = status;
    }
}

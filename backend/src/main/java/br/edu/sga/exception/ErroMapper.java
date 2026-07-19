package br.edu.sga.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.OffsetDateTime;
import java.util.Map;

@Provider
public class ErroMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable erro) {
        if (erro instanceof ApiException api) {
            return resposta(api.status, api.getMessage());
        }
        if (erro instanceof ConstraintViolationException validacao) {
            return resposta(Response.Status.BAD_REQUEST, validacao.getConstraintViolations().stream()
                    .map(v -> v.getMessage()).distinct().toList());
        }
        if (erro instanceof WebApplicationException web) {
            return resposta(Response.Status.fromStatusCode(web.getResponse().getStatus()), web.getMessage());
        }
        return resposta(Response.Status.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro ao processar a solicitação. Tente novamente.");
    }

    private Response resposta(Response.Status status, Object mensagem) {
        return Response.status(status).entity(Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "status", status.getStatusCode(),
                "mensagem", mensagem
        )).build();
    }
}

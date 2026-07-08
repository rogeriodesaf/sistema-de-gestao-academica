package br.edu.sga.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Map;

@Path("/api/status")
public class StatusResource {
    @GET
    public Map<String, Object> status() {
        return Map.of("sistema", "SGA - Sistema de Gestao Academica", "status", "online");
    }
}

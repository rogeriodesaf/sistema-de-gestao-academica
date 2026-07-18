package br.edu.sga.service;

import br.edu.sga.entity.Professor;
import br.edu.sga.entity.Usuario;
import br.edu.sga.enums.Perfil;
import br.edu.sga.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.util.List;

@ApplicationScoped
public class ProfessorUsuarioService {
    public Professor identificarProfessor(Long usuarioId) {
        Usuario usuario = Usuario.findById(usuarioId);
        if (usuario == null || usuario.perfil != Perfil.PROFESSOR) {
            throw cadastroNaoVinculado();
        }

        Professor professor = Professor.find("usuario", usuario).firstResult();
        if (professor != null) return professor;

        List<Professor> candidatos = Professor.<Professor>find(
                "usuario is null and lower(email) = ?1 order by id", normalizar(usuario.email))
                .page(0, 2).list();
        if (candidatos.size() > 1) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Mais de um cadastro de professor possui o e-mail do usuario autenticado");
        }
        if (candidatos.size() == 1) return candidatos.getFirst();
        throw cadastroNaoVinculado();
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private ApiException cadastroNaoVinculado() {
        return new ApiException(Response.Status.NOT_FOUND,
                "Cadastro de professor nao vinculado ao usuario autenticado");
    }
}

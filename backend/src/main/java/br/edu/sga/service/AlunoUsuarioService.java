package br.edu.sga.service;

import br.edu.sga.entity.Aluno;
import br.edu.sga.entity.Usuario;
import br.edu.sga.enums.Perfil;
import br.edu.sga.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import java.util.List;

@ApplicationScoped
public class AlunoUsuarioService {
    @Transactional
    public Aluno identificarAluno(Long usuarioId) {
        Usuario usuario = Usuario.findById(usuarioId);
        if (usuario == null || usuario.perfil != Perfil.ALUNO) {
            throw cadastroNaoVinculado();
        }

        Aluno aluno = Aluno.find("usuario", usuario).firstResult();
        if (aluno != null) return aluno;

        vincularCadastroCompativel(usuario);
        aluno = Aluno.find("usuario", usuario).firstResult();
        if (aluno == null) throw cadastroNaoVinculado();
        return aluno;
    }

    @Transactional
    public void vincularUsuarioCompativel(Aluno aluno) {
        if (aluno.usuario != null && aluno.usuario.id != null) {
            Usuario usuario = Usuario.findById(aluno.usuario.id);
            validarVinculo(aluno, usuario);
            aluno.usuario = usuario;
            return;
        }
        if (aluno.email == null || aluno.email.isBlank()) return;

        Usuario usuario = Usuario.find("lower(email) = ?1 and perfil = ?2",
                normalizar(aluno.email), Perfil.ALUNO).firstResult();
        if (usuario != null) validarVinculo(aluno, usuario);
        aluno.usuario = usuario;
    }

    @Transactional
    public void vincularCadastroCompativel(Usuario usuario) {
        if (usuario == null || usuario.perfil != Perfil.ALUNO) return;
        if (Aluno.count("usuario", usuario) > 0) return;

        List<Aluno> candidatos = Aluno.<Aluno>find(
                "usuario is null and lower(email) = ?1 order by id", normalizar(usuario.email))
                .page(0, 2).list();
        if (candidatos.size() > 1) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Mais de um cadastro de aluno possui o e-mail do usuario");
        }
        if (candidatos.size() == 1) candidatos.getFirst().usuario = usuario;
    }

    private void validarVinculo(Aluno aluno, Usuario usuario) {
        if (usuario == null || usuario.perfil != Perfil.ALUNO) {
            throw new ApiException(Response.Status.BAD_REQUEST,
                    "O cadastro deve ser vinculado a um usuario com perfil ALUNO");
        }
        long outrosVinculos = aluno.id == null
                ? Aluno.count("usuario", usuario)
                : Aluno.count("usuario = ?1 and id <> ?2", usuario, aluno.id);
        if (outrosVinculos > 0) {
            throw new ApiException(Response.Status.CONFLICT,
                    "O usuario ja esta vinculado a outro cadastro de aluno");
        }
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private ApiException cadastroNaoVinculado() {
        return new ApiException(Response.Status.NOT_FOUND,
                "Cadastro de aluno nao vinculado ao usuario autenticado");
    }
}

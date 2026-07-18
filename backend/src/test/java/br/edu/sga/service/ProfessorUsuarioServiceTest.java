package br.edu.sga.service;

import br.edu.sga.entity.Professor;
import br.edu.sga.entity.Usuario;
import br.edu.sga.enums.Perfil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class ProfessorUsuarioServiceTest {
    @Inject ProfessorUsuarioService service;
    @Inject EntityManager entityManager;

    @Test
    @Transactional
    void devePersistirVinculoQuandoEmailIdentificaUmUnicoProfessor() {
        String email = "professor-" + UUID.randomUUID() + "@sga.local";
        Usuario usuario = new Usuario();
        usuario.nome = "Professor legado";
        usuario.email = email;
        usuario.senhaHash = "hash-teste";
        usuario.perfil = Perfil.PROFESSOR;
        usuario.persist();

        Professor professor = new Professor();
        professor.nome = "Professor legado";
        professor.email = email;
        professor.persist();
        entityManager.flush();

        Professor identificado = service.identificarProfessor(usuario.id);
        assertEquals(professor.id, identificado.id);

        entityManager.flush();
        entityManager.clear();
        Professor persistido = Professor.findById(professor.id);
        assertNotNull(persistido.usuario);
        assertEquals(usuario.id, persistido.usuario.id);
    }
}

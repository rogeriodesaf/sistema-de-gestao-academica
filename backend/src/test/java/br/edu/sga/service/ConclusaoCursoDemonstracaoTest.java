package br.edu.sga.service;

import br.edu.sga.entity.Aluno;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.Professor;
import br.edu.sga.entity.Usuario;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.enums.StatusOfertaDisciplina;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ConclusaoCursoDemonstracaoTest {
    @Inject FechamentoDiarioService fechamentoService;
    @Inject IntegralizacaoCursoService integralizacaoService;

    @Test
    @TestTransaction
    void deveConcluirCursoDepoisDoFechamentoEHomologacaoDoUltimoDiario() {
        OfertaDisciplina oferta = OfertaDisciplina.find(
                "disciplina.codigo", "DEMO-CONCLUSAO").firstResult();
        Professor professor = Professor.find("email", "professor@sga.local").firstResult();
        Usuario coordenador = Usuario.find("email", "admin@sga.local").firstResult();
        Aluno aluno = Aluno.find("email", "aluno.conclusao@sga.local").firstResult();
        MatriculaDisciplina matricula = MatriculaDisciplina.find(
                "aluno = ?1 and ofertaDisciplina = ?2", aluno, oferta).firstResult();

        var antes = integralizacaoService.consultar(aluno);
        assertEquals(0, antes.disciplinasConcluidas());
        assertEquals(1, antes.disciplinasObrigatorias());
        assertTrue(fechamentoService.pendencias(oferta).isEmpty());

        fechamentoService.encerrar(oferta, professor);
        assertEquals(StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO, oferta.status);

        var resultado = fechamentoService.homologar(oferta, coordenador);
        var depois = integralizacaoService.consultar(aluno);
        assertEquals(StatusOfertaDisciplina.CONCLUIDA, oferta.status);
        assertEquals(StatusMatriculaDisciplina.CONCLUIDA, matricula.status);
        assertEquals(1, depois.disciplinasConcluidas());
        assertEquals("CONCLUIDO", depois.situacaoCurso());
        assertTrue(resultado.mensagem().contains("Aluno Concluinte Demonstracao"));
    }
}

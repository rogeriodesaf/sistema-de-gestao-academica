package br.edu.sga.service;

import br.edu.sga.entity.Aluno;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.Professor;
import br.edu.sga.entity.Usuario;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.enums.ResultadoAcademico;
import br.edu.sga.exception.ApiException;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ConclusaoCursoDemonstracaoTest {
    @Inject FechamentoDiarioService fechamentoService;
    @Inject IntegralizacaoCursoService integralizacaoService;
    @Inject AcademicoService academicoService;

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
        assertEquals(ResultadoAcademico.EM_ANDAMENTO, matricula.resultadoAcademico);
        assertEquals(0, HistoricoEscolar.count("matriculaDisciplina", matricula));
        assertTrue(fechamentoService.pendencias(oferta).isEmpty());

        fechamentoService.encerrar(oferta, professor);
        assertEquals(StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO, oferta.status);
        assertEquals(StatusMatriculaDisciplina.ATIVA, matricula.status);
        assertEquals(ResultadoAcademico.EM_ANDAMENTO, matricula.resultadoAcademico);
        assertEquals(0, HistoricoEscolar.count("matriculaDisciplina", matricula));

        var resultado = fechamentoService.homologar(oferta, coordenador);
        var depois = integralizacaoService.consultar(aluno);
        assertEquals(StatusOfertaDisciplina.CONCLUIDA, oferta.status);
        assertEquals(StatusMatriculaDisciplina.ATIVA, matricula.status);
        assertEquals(ResultadoAcademico.APROVADO, matricula.resultadoAcademico);
        HistoricoEscolar historico = HistoricoEscolar.find("matriculaDisciplina", matricula).firstResult();
        assertEquals("Componente Final de Demonstracao", historico.disciplinaNome);
        assertEquals("DEMO-CONCLUSAO", historico.disciplinaCodigo);
        assertEquals(professor.nome, historico.professorNome);
        assertEquals(oferta.dataHomologacao, historico.dataHomologacao);
        assertEquals(1, depois.disciplinasConcluidas());
        assertEquals("CONCLUIDO", depois.situacaoCurso());
        assertTrue(resultado.mensagem().contains("Aluno Concluinte Demonstracao"));
    }

    @Test
    @TestTransaction
    void deveImpedirDuplicidadeENovaMatriculaAcimaDasVagasDaOferta() {
        OfertaDisciplina oferta = OfertaDisciplina.find(
                "disciplina.codigo", "DEMO-CONCLUSAO").firstResult();
        Aluno concluinte = Aluno.find("email", "aluno.conclusao@sga.local").firstResult();

        MatriculaDisciplina duplicada = new MatriculaDisciplina();
        duplicada.aluno = concluinte;
        duplicada.ofertaDisciplina = oferta;
        ApiException conflitoDuplicidade = assertThrows(ApiException.class,
                () -> academicoService.matricularEmDisciplina(duplicada));
        assertTrue(conflitoDuplicidade.getMessage().contains("já está matriculado"));

        oferta.vagas = 1;
        Aluno outroAluno = Aluno.find("email", "aluno@sga.local").firstResult();
        MatriculaDisciplina excedente = new MatriculaDisciplina();
        excedente.aluno = outroAluno;
        excedente.ofertaDisciplina = oferta;
        ApiException conflitoVagas = assertThrows(ApiException.class,
                () -> academicoService.matricularEmDisciplina(excedente));
        assertTrue(conflitoVagas.getMessage().contains("Não há vagas"));
    }
}

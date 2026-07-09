package br.edu.sga.service;

import br.edu.sga.entity.*;
import br.edu.sga.enums.*;
import br.edu.sga.security.SenhaService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@ApplicationScoped
public class DadosAcademicosIniciaisService {
    @Inject
    SenhaService senhaService;

    @ConfigProperty(name = "sga.upload.dir")
    String uploadDir;

    @Transactional
    void criarDadosAcademicos(@Observes StartupEvent evento) {
        if (Curso.count("nome", "Curso de Teologia") > 0) {
            return;
        }

        Curso curso = new Curso();
        curso.nome = "Curso de Teologia";
        curso.descricao = "Curso base para testes do fluxo academico do seminario.";
        curso.ativo = true;
        anexarGradePlaceholder(curso);
        curso.persist();

        Modulo modulo = new Modulo();
        modulo.curso = curso;
        modulo.nome = "Modulo 1";
        modulo.descricao = "Hermenêutica, Homilética e Apologética.";
        modulo.ordem = 1;
        modulo.status = StatusModulo.ABERTO;
        modulo.ativo = true;
        modulo.persist();

        Usuario usuarioProfessor = usuario("Professor Exemplo", "professor@sga.local", Perfil.PROFESSOR);
        Professor professor = new Professor();
        professor.nome = "Professor Exemplo";
        professor.email = "professor@sga.local";
        professor.formacao = "Teologia";
        professor.usuario = usuarioProfessor;
        professor.persist();

        Disciplina hermeneutica = disciplina(curso, modulo, professor, "Hermenêutica", "HER-101", 40, 2);
        Disciplina homiletica = disciplina(curso, modulo, professor, "Homilética", "HOM-101", 40, 2);
        Disciplina apologetica = disciplina(curso, modulo, professor, "Apologética", "APO-101", 40, 2);

        Usuario usuarioAluno = usuario("Aluno Exemplo", "aluno@sga.local", Perfil.ALUNO);
        Aluno aluno = new Aluno();
        aluno.nome = "Aluno Exemplo";
        aluno.email = "aluno@sga.local";
        aluno.status = StatusAluno.ATIVO;
        aluno.dataIngresso = LocalDate.now();
        aluno.curso = curso;
        aluno.usuario = usuarioAluno;
        aluno.persist();

        Turma turma = new Turma();
        turma.nome = "Turma Teologia 2026";
        turma.curso = curso;
        turma.anoPeriodo = "2026";
        turma.status = StatusTurma.ABERTA;
        turma.persist();

        AnoLetivo ano = new AnoLetivo();
        ano.turma = turma;
        ano.ano = 2026;
        ano.status = StatusAnoLetivo.EM_ANDAMENTO;
        ano.persist();

        PeriodoLetivo periodo = new PeriodoLetivo();
        periodo.anoLetivo = ano;
        periodo.nome = "Modulo 1 - 2026";
        periodo.ordem = 1;
        periodo.tipo = TipoPeriodoLetivo.MODULO;
        periodo.status = StatusPeriodoLetivo.ABERTO;
        periodo.persist();

        OfertaDisciplina ofertaHermeneutica = oferta(turma, ano, periodo, curso, modulo, hermeneutica, professor, "Segunda 19h", "Sala 1");
        oferta(turma, ano, periodo, curso, modulo, homiletica, professor, "Terca 19h", "Sala 1");
        oferta(turma, ano, periodo, curso, modulo, apologetica, professor, "Quarta 19h", "Sala 1");

        MatriculaDisciplina matricula = new MatriculaDisciplina();
        matricula.aluno = aluno;
        matricula.curso = curso;
        matricula.periodoLetivo = periodo;
        matricula.ofertaDisciplina = ofertaHermeneutica;
        matricula.status = StatusMatriculaDisciplina.ATIVA;
        matricula.persist();
    }

    private Usuario usuario(String nome, String email, Perfil perfil) {
        Usuario existente = Usuario.find("email", email).firstResult();
        if (existente != null) {
            return existente;
        }
        Usuario usuario = new Usuario();
        usuario.nome = nome;
        usuario.email = email;
        usuario.senhaHash = senhaService.criptografar("123456");
        usuario.perfil = perfil;
        usuario.ativo = true;
        usuario.persist();
        return usuario;
    }

    private Disciplina disciplina(Curso curso, Modulo modulo, Professor professor, String nome, String codigo, int cargaHoraria, int creditos) {
        Disciplina disciplina = new Disciplina();
        disciplina.curso = curso;
        disciplina.modulo = modulo;
        disciplina.professorResponsavel = professor;
        disciplina.nome = nome;
        disciplina.codigo = codigo;
        disciplina.cargaHoraria = cargaHoraria;
        disciplina.creditos = creditos;
        disciplina.ementaResumo = "Ementa oficial disponivel em PDF.";
        disciplina.ativo = true;
        disciplina.persist();
        return disciplina;
    }

    private OfertaDisciplina oferta(Turma turma, AnoLetivo ano, PeriodoLetivo periodo, Curso curso, Modulo modulo,
                                    Disciplina disciplina, Professor professor, String horario, String sala) {
        OfertaDisciplina oferta = new OfertaDisciplina();
        oferta.turma = turma;
        oferta.anoLetivo = ano;
        oferta.periodoLetivo = periodo;
        oferta.curso = curso;
        oferta.modulo = modulo;
        oferta.disciplina = disciplina;
        oferta.professor = professor;
        oferta.vagas = 30;
        oferta.horario = horario;
        oferta.sala = sala;
        oferta.cargaHorariaPrevista = disciplina.cargaHoraria;
        oferta.status = StatusOfertaDisciplina.ABERTA;
        oferta.persist();
        return oferta;
    }

    private void anexarGradePlaceholder(Curso curso) {
        try {
            Path pasta = Path.of(uploadDir, "grades-curriculares");
            Files.createDirectories(pasta);
            Path arquivo = pasta.resolve("grade-curricular-teologia-placeholder.pdf");
            if (!Files.exists(arquivo)) {
                Files.writeString(arquivo, """
                        %PDF-1.4
                        1 0 obj
                        << /Type /Catalog /Pages 2 0 R >>
                        endobj
                        2 0 obj
                        << /Type /Pages /Kids [3 0 R] /Count 1 >>
                        endobj
                        3 0 obj
                        << /Type /Page /Parent 2 0 R /MediaBox [0 0 300 144] /Contents 4 0 R >>
                        endobj
                        4 0 obj
                        << /Length 54 >>
                        stream
                        BT /F1 12 Tf 30 80 Td (Grade curricular - placeholder) Tj ET
                        endstream
                        endobj
                        trailer
                        << /Root 1 0 R >>
                        %%EOF
                        """);
            }
            curso.gradePdfCaminho = arquivo.toString();
            curso.gradePdfNome = "grade-curricular-teologia-placeholder.pdf";
            curso.gradePdfTipo = "application/pdf";
            curso.gradePdfTamanho = Files.size(arquivo);
        } catch (Exception ignored) {
            // A ausencia do placeholder nao deve impedir o sistema de iniciar.
        }
    }
}

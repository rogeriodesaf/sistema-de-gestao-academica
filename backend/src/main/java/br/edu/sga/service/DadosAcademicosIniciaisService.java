package br.edu.sga.service;

import br.edu.sga.entity.*;
import br.edu.sga.enums.*;
import br.edu.sga.exception.ApiException;
import br.edu.sga.security.SenhaService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class DadosAcademicosIniciaisService {
    @Inject
    SenhaService senhaService;

    @ConfigProperty(name = "sga.upload.dir")
    String uploadDir;

    @ConfigProperty(name = "sga.seed.homologacao.enabled", defaultValue = "false")
    boolean seedHomologacaoEnabled;

    @Inject
    AcademicoService academicoService;

    @Transactional
    void criarDadosAcademicos(@Observes StartupEvent evento) {
        limparReferenciasDeArquivosAusentes();

        Curso existente = Curso.find("lower(nome) = ?1 or lower(nome) = ?2",
                "curso de teologia", "teologia ministerial").firstResult();
        if (existente != null) {
            garantirGradeMinisterial(existente);
            garantirGradePlaceholder(existente);
            garantirProfessorDemonstracao();
            garantirProfessorHomologacao(existente);
            garantirCenarioConclusaoDemonstracao();
            return;
        }

        Curso curso = new Curso();
        curso.nome = "Teologia Ministerial";
        curso.descricao = "Curso de Teologia Ministerial do Seminário Teológico Congregacional de João Pessoa.";
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
        ano.ano = 2026;
        ano.dataInicio = LocalDate.of(2026, 1, 1);
        ano.dataFim = LocalDate.of(2026, 12, 31);
        ano.status = StatusAnoLetivo.EM_ANDAMENTO;
        ano.persist();

        PeriodoLetivo periodo = new PeriodoLetivo();
        periodo.anoLetivo = ano;
        periodo.nome = "Modulo 1 - 2026";
        periodo.ordem = 1;
        periodo.tipo = TipoPeriodoLetivo.MODULO;
        periodo.status = StatusPeriodoLetivo.ABERTO;
        periodo.persist();

        turma.anoLetivo = ano;
        turma.periodoLetivo = periodo;
        turma.quantidadeMaximaAlunos = 30;

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

        garantirProfessorDemonstracao();
        garantirProfessorHomologacao(curso);
        garantirGradeMinisterial(curso);
        garantirCenarioConclusaoDemonstracao();
    }

    private void garantirProfessorDemonstracao() {
        Usuario usuario = Usuario.find("email", "professor@sga.local").firstResult();
        if (usuario == null) {
            usuario = new Usuario();
            usuario.email = "professor@sga.local";
        }
        usuario.nome = "Professor Exemplo";
        usuario.senhaHash = senhaService.criptografar("123456");
        usuario.perfil = Perfil.PROFESSOR;
        usuario.ativo = true;
        if (!usuario.isPersistent()) usuario.persist();

        Professor professor = Professor.find("usuario = ?1", usuario).firstResult();
        if (professor == null) professor = Professor.find("nome", "Professor Exemplo").firstResult();
        if (professor == null) {
            professor = new Professor();
            professor.nome = "Professor Exemplo";
            professor.persist();
        }
        professor.email = "professor@sga.local";
        professor.formacao = professor.formacao == null ? "Teologia" : professor.formacao;
        professor.ativo = true;
        professor.usuario = usuario;
    }

    private void garantirCenarioConclusaoDemonstracao() {
        Professor professor = Professor.find("email", "professor@sga.local").firstResult();
        if (professor == null) return;

        Curso curso = Curso.find("nome", "Curso Demonstrativo de Conclusao").firstResult();
        if (curso == null) {
            curso = new Curso();
            curso.nome = "Curso Demonstrativo de Conclusao";
            curso.persist();
        }
        curso.descricao = "Cenario isolado para demonstrar fechamento de diario e conclusao de curso.";
        curso.cargaHorariaTotal = 2;
        curso.creditosTotais = 1;
        curso.ativo = true;

        Modulo modulo = Modulo.find("curso = ?1 and ordem = 1", curso).firstResult();
        if (modulo == null) {
            modulo = new Modulo();
            modulo.curso = curso;
            modulo.ordem = 1;
        }
        modulo.nome = "Modulo Unico - Demonstracao";
        modulo.descricao = "Componente final utilizado na demonstracao da conclusao.";
        modulo.status = StatusModulo.ABERTO;
        modulo.ativo = true;
        if (!modulo.isPersistent()) modulo.persist();

        Disciplina disciplina = Disciplina.find("codigo", "DEMO-CONCLUSAO").firstResult();
        if (disciplina == null) {
            disciplina = new Disciplina();
            disciplina.codigo = "DEMO-CONCLUSAO";
        }
        disciplina.nome = "Componente Final de Demonstracao";
        disciplina.curso = curso;
        disciplina.modulo = modulo;
        disciplina.moduloOriginal = modulo;
        disciplina.professorResponsavel = professor;
        disciplina.cargaHoraria = 2;
        disciplina.creditos = 1;
        disciplina.tipoComponente = TipoComponenteCurricular.OBRIGATORIA;
        disciplina.ementaResumo = "Componente preparado para demonstrar a conclusao do curso.";
        disciplina.ativo = true;
        if (!disciplina.isPersistent()) disciplina.persist();

        AnoLetivo ano = AnoLetivo.find("ano", 2026).firstResult();
        if (ano == null) {
            ano = new AnoLetivo();
            ano.ano = 2026;
            ano.dataInicio = LocalDate.of(2026, 1, 1);
            ano.dataFim = LocalDate.of(2026, 12, 31);
            ano.status = StatusAnoLetivo.EM_ANDAMENTO;
            ano.persist();
        }
        PeriodoLetivo periodo = PeriodoLetivo.find("anoLetivo = ?1 order by ordem", ano).firstResult();

        Turma turma = Turma.find("nome", "Turma Demonstracao de Conclusao").firstResult();
        if (turma == null) {
            turma = new Turma();
            turma.nome = "Turma Demonstracao de Conclusao";
        }
        turma.curso = curso;
        turma.anoLetivo = ano;
        turma.periodoLetivo = periodo;
        turma.anoPeriodo = "2026";
        turma.turno = "Noite";
        turma.quantidadeMaximaAlunos = 10;
        turma.status = StatusTurma.EM_ANDAMENTO;
        if (!turma.isPersistent()) turma.persist();

        OfertaDisciplina oferta = OfertaDisciplina.find(
                "turma = ?1 and disciplina = ?2", turma, disciplina).firstResult();
        if (oferta == null) {
            oferta = new OfertaDisciplina();
            oferta.turma = turma;
            oferta.disciplina = disciplina;
            oferta.status = StatusOfertaDisciplina.EM_ANDAMENTO;
        }
        oferta.anoLetivo = ano;
        oferta.periodoLetivo = periodo;
        oferta.curso = curso;
        oferta.modulo = modulo;
        oferta.professor = professor;
        oferta.vagas = 10;
        oferta.horario = "Demonstracao agendada";
        oferta.sala = "Sala Demonstrativa";
        oferta.cargaHorariaPrevista = 2;
        oferta.dataInicio = LocalDate.of(2026, 7, 1);
        oferta.dataFim = LocalDate.of(2026, 7, 31);
        if (!oferta.isPersistent()) oferta.persist();

        Usuario usuarioAluno = Usuario.find("email", "aluno.conclusao@sga.local").firstResult();
        if (usuarioAluno == null) {
            usuarioAluno = new Usuario();
            usuarioAluno.email = "aluno.conclusao@sga.local";
        }
        usuarioAluno.nome = "Aluno Concluinte Demonstracao";
        usuarioAluno.senhaHash = senhaService.criptografar("Aluno@123");
        usuarioAluno.perfil = Perfil.ALUNO;
        usuarioAluno.ativo = true;
        if (!usuarioAluno.isPersistent()) usuarioAluno.persist();

        Aluno aluno = Aluno.find("email", usuarioAluno.email).firstResult();
        if (aluno == null) {
            aluno = new Aluno();
            aluno.email = usuarioAluno.email;
        }
        aluno.nome = usuarioAluno.nome;
        aluno.status = StatusAluno.ATIVO;
        aluno.dataIngresso = LocalDate.of(2026, 7, 1);
        aluno.curso = curso;
        aluno.usuario = usuarioAluno;
        if (!aluno.isPersistent()) aluno.persist();

        Matricula matriculaCurso = Matricula.find(
                "aluno = ?1 and curso = ?2 and turma is null and disciplina is null", aluno, curso).firstResult();
        if (matriculaCurso == null) {
            matriculaCurso = new Matricula();
            matriculaCurso.aluno = aluno;
            matriculaCurso.curso = curso;
            matriculaCurso.dataMatricula = LocalDate.of(2026, 7, 1);
            matriculaCurso.status = StatusMatricula.EM_ANDAMENTO;
            matriculaCurso.persist();
        }

        MatriculaDisciplina matricula = MatriculaDisciplina.find(
                "aluno = ?1 and ofertaDisciplina = ?2", aluno, oferta).firstResult();
        if (matricula == null) {
            matricula = new MatriculaDisciplina();
            matricula.aluno = aluno;
            matricula.ofertaDisciplina = oferta;
            matricula.curso = curso;
            matricula.periodoLetivo = periodo;
            matricula.dataMatricula = LocalDate.of(2026, 7, 1);
            matricula.status = StatusMatriculaDisciplina.ATIVA;
            matricula.persist();
        }

        AulaMinistrada aula = AulaMinistrada.find(
                "ofertaDisciplina = ?1 and dataAula = ?2", oferta, LocalDate.of(2026, 7, 1)).firstResult();
        if (aula == null) {
            aula = new AulaMinistrada();
            aula.ofertaDisciplina = oferta;
            aula.dataAula = LocalDate.of(2026, 7, 1);
            aula.persist();
        }
        aula.disciplina = disciplina;
        aula.turma = turma;
        aula.professor = professor;
        aula.conteudoMinistrado = "Sintese final e encerramento do componente demonstrativo.";
        aula.cargaHorariaAula = 2;

        Frequencia frequencia = Frequencia.find(
                "aula = ?1 and matriculaDisciplina = ?2", aula, matricula).firstResult();
        if (frequencia == null) {
            frequencia = new Frequencia();
            frequencia.aula = aula;
            frequencia.matriculaDisciplina = matricula;
            frequencia.persist();
        }
        frequencia.aluno = aluno;
        frequencia.status = StatusFrequencia.PRESENTE;
        frequencia.presente = true;

        Avaliacao avaliacao = Avaliacao.find(
                "ofertaDisciplina = ?1 and nome = ?2", oferta, "Avaliacao Final Demonstrativa").firstResult();
        if (avaliacao == null) {
            avaliacao = new Avaliacao();
            avaliacao.ofertaDisciplina = oferta;
            avaliacao.nome = "Avaliacao Final Demonstrativa";
        }
        avaliacao.professor = professor;
        avaliacao.ordem = 1;
        avaliacao.data = LocalDate.of(2026, 7, 1);
        avaliacao.notaMaxima = BigDecimal.TEN;
        avaliacao.peso = BigDecimal.ONE;
        if (!avaliacao.isPersistent()) avaliacao.persist();

        NotaAvaliacao nota = NotaAvaliacao.find(
                "avaliacao = ?1 and matriculaDisciplina = ?2", avaliacao, matricula).firstResult();
        if (nota == null) {
            nota = new NotaAvaliacao();
            nota.avaliacao = avaliacao;
            nota.matriculaDisciplina = matricula;
        }
        nota.aluno = aluno;
        nota.nota = new BigDecimal("9.0");
        nota.observacao = "Nota preparada para demonstracao da conclusao.";
        if (!nota.isPersistent()) nota.persist();
    }

    private void garantirGradeMinisterial(Curso curso) {
        curso.nome = "Teologia Ministerial";
        curso.descricao = "Curso de Teologia Ministerial do Seminário Teológico Congregacional de João Pessoa.";
        curso.cargaHorariaTotal = 3240;
        curso.creditosTotais = 180;
        curso.ativo = true;

        List<ComponenteGrade> componentes = List.of(
                componente(1, "Metodologia da Pesquisa e do Trabalho Científico", 4),
                componente(1, "Português Instrumental I", 2),
                componente(1, "Introdução a Teologia", 2),
                componente(1, "Introdução Filosofia", 2),
                componente(1, "História da Igreja I: Idade Antiga", 2),
                componente(1, "Ética Cristã", 2),
                componente(1, "Estágio I", 1),

                componente(2, "Português Instrumental II", 2),
                componente(2, "Teologia Sistemática I: Bibliologia e Teontologia", 4),
                componente(2, "Hermenêutica I", 2),
                componente(2, "História da Igreja II: Idade Medieval", 2),
                componente(2, "Introdução à Psicologia", 2),
                componente(2, "Introdução à Sociologia", 2),
                componente(2, "Evangelismo e Missões", 2),
                componente(2, "Estágio II", 1),

                componente(3, "Teologia Sistemática II: Antropologia e Angeologia", 4),
                componente(3, "Hermenêutica II", 2),
                componente(3, "Antigo Testamento: Pentateuco", 4),
                componente(3, "Cosmovisão Cristã", 2),
                componente(3, "História da Igreja III: Idade Moderna", 2),
                componente(3, "Missiologia Kalleyana", 2),
                componente(3, "Estágio III", 1),

                componente(4, "Teologia Sistemática II: Hamartiologia e Soteriologia", 4),
                componente(4, "Antigo Testamento: Históricos", 2),
                componente(4, "História da Igreja IV: Idade Contemporânea", 2),
                componente(4, "Homilética I", 2),
                optativa(4, "Música Cristã - Optativa", 2),
                componente(4, "Apologética I", 2),
                componente(4, "Estágio IV", 1),

                componente(5, "Teologia Sistemática IV: Cristologia e Pneumatologia", 4),
                componente(5, "Homilética II", 2),
                componente(5, "Apologética II", 2),
                componente(5, "Teologia Patrística", 2),
                componente(5, "Evangelhos Sinóticos", 2),
                componente(5, "Antigo Testamento: Sapienciais", 4),
                componente(5, "Estágio V", 1),
                complementar(5, "Práticas Ministeriais I", 1),

                componente(6, "Teologia Sistemática V: Eclesiologia", 4),
                componente(6, "Teologia Escolástica", 4),
                componente(6, "Teologia Pastoral", 2),
                componente(6, "Igreja e Sociedade", 2),
                componente(6, "Análise no Evangelho de João", 2),
                componente(6, "Antigo Testamento: Profetas", 2),
                componente(6, "Estágio VI", 1),
                complementar(6, "Práticas Ministeriais II", 1),

                componente(7, "Teologia Sistemática VI: Escatologia", 4),
                componente(7, "Atos dos Apóstolos", 2),
                componente(7, "Teologia da Reforma", 2),
                componente(7, "Teologia Bíblica do Antigo Testamento", 2),
                componente(7, "Aconselhamento Bíblico", 2),
                optativa(7, "Religiões Comparadas - Optativa", 2),
                componente(7, "Estágio VII", 1),
                complementar(7, "Práticas Ministeriais III", 1),

                componente(8, "Teologia Contemporânea", 2),
                componente(8, "Cartas Paulinas", 2),
                componente(8, "Hebraico Instrumental I", 2),
                componente(8, "Cartas Gerais", 2),
                optativa(8, "Discipulado - Optativa", 2),
                optativa(8, "Bem-estar e Saúde - Optativa", 2),
                componente(8, "Estágio VIII", 1),
                complementar(8, "Práticas Ministeriais IV", 1),

                componente(9, "Hebraico Instrumental II", 2),
                componente(9, "História do Protestantismo Brasileiro", 2),
                componente(9, "Análise em Apocalipse", 4),
                componente(9, "Análise em Romanos", 2),
                componente(9, "Plantação e revitalização de Igrejas", 2),
                optativa(9, "Tecnologia, Mídias Digitais e Ministério - Optativa", 2),
                componente(9, "Estágio IX", 1),

                componente(10, "Grego Instrumental I", 2),
                componente(10, "Análise em Hebreus", 2),
                componente(10, "Análise em Efésios", 2),
                componente(10, "Gestão Eclesiástica", 2),
                componente(10, "História do Congregacionalismo", 2),
                componente(10, "Culto e Liturgia", 2),
                componente(10, "Estágio X", 1),

                componente(11, "Grego Instrumental II", 2),
                componente(11, "Teologia Bíblica Novo Testamento", 2),
                componente(11, "Monografia I", 4),
                componente(11, "Eclesiologia Congregacional", 2),
                optativa(11, "Cristianismo e Política - Optativa", 2),
                componente(11, "Fundamentos Pedagógicos", 2),
                componente(11, "Estágio XI", 1),

                componente(12, "Manuscritologia Bíblica", 2),
                componente(12, "Monografia II", 4),
                optativa(12, "Missões Urbanas - Optativa", 2),
                componente(12, "Liderança Cristã", 2),
                optativa(12, "Cristianismo e Arte - Optativa", 2),
                componente(12, "Direitos Humanos", 2),
                componente(12, "Estágio XII", 1)
        );

        Modulo[] modulos = new Modulo[13];
        for (int ordem = 1; ordem <= 12; ordem++) {
            modulos[ordem] = garantirModuloMatriz(curso, ordem);
        }
        int[] ordemNoModulo = new int[13];
        for (ComponenteGrade componente : componentes) {
            int ordem = ++ordemNoModulo[componente.modulo()];
            garantirDisciplinaMatriz(curso, modulos[componente.modulo()], componente, ordem);
        }
    }

    private Modulo garantirModuloMatriz(Curso curso, int ordem) {
        Modulo modulo = Modulo.find("curso = ?1 and ordem = ?2 and anoLetivo is null", curso, ordem).firstResult();
        if (modulo == null) {
            modulo = new Modulo();
            modulo.curso = curso;
            modulo.ordem = ordem;
        }
        modulo.nome = "Módulo " + ordem;
        modulo.descricao = "Matriz curricular do curso de Teologia Ministerial.";
        modulo.status = StatusModulo.ABERTO;
        modulo.ativo = true;
        if (!modulo.isPersistent()) modulo.persist();
        return modulo;
    }

    private void garantirDisciplinaMatriz(Curso curso, Modulo modulo, ComponenteGrade componente, int ordem) {
        String codigo = codigoPreservado(componente.nome());
        if (codigo == null) codigo = "TM-%02d-%02d".formatted(componente.modulo(), ordem);
        Disciplina disciplina = Disciplina.find("codigo", codigo).firstResult();
        if (disciplina == null) {
            disciplina = Disciplina.find("curso = ?1 and lower(nome) = lower(?2)", curso, componente.nome()).firstResult();
        }
        if (disciplina == null) {
            disciplina = new Disciplina();
            disciplina.codigo = codigo;
        }
        disciplina.curso = curso;
        disciplina.nome = componente.nome();
        disciplina.moduloOriginal = modulo;
        if (disciplina.modulo == null) disciplina.modulo = modulo;
        disciplina.cargaHoraria = componente.creditos() * 18;
        disciplina.creditos = componente.creditos();
        disciplina.tipoComponente = componente.tipo();
        disciplina.ementaResumo = disciplina.ementaResumo == null
                ? "Ementa a ser cadastrada conforme o plano de ensino." : disciplina.ementaResumo;
        disciplina.ativo = true;
        if (!disciplina.isPersistent()) disciplina.persist();
    }

    private String codigoPreservado(String nome) {
        return switch (nome) {
            case "Hermenêutica I" -> "HER-101";
            case "Homilética I" -> "HOM-101";
            case "Apologética I" -> "APO-101";
            default -> null;
        };
    }

    private ComponenteGrade componente(int modulo, String nome, int creditos) {
        return new ComponenteGrade(modulo, nome, creditos, TipoComponenteCurricular.OBRIGATORIA);
    }

    private ComponenteGrade optativa(int modulo, String nome, int creditos) {
        return new ComponenteGrade(modulo, nome, creditos, TipoComponenteCurricular.OPTATIVA);
    }

    private ComponenteGrade complementar(int modulo, String nome, int creditos) {
        return new ComponenteGrade(modulo, nome, creditos, TipoComponenteCurricular.COMPLEMENTAR);
    }

    private record ComponenteGrade(int modulo, String nome, int creditos, TipoComponenteCurricular tipo) {}

    private void garantirProfessorHomologacao(Curso curso) {
        if (!seedHomologacaoEnabled) {
            return;
        }

        Professor professor = professorHomologacao();
        professor.email = "professor@seminario.local";
        professor.formacao = professor.formacao == null ? "Teologia" : professor.formacao;
        professor.ativo = true;

        Modulo modulo = moduloHomologacao(curso);
        Disciplina disciplina = disciplinaHomologacao(curso, modulo, professor);
        Turma turma = turmaHomologacao(curso, disciplina, professor);
        AnoLetivo ano = anoLetivoHomologacao(turma);
        PeriodoLetivo periodo = periodoLetivoHomologacao(ano);
        turma.anoLetivo = ano;
        turma.periodoLetivo = periodo;
        OfertaDisciplina oferta = ofertaHomologacao(turma, ano, periodo, curso, modulo, disciplina, professor);

        turma.anoLetivo = ano;
        turma.periodoLetivo = periodo;

        for (Aluno aluno : alunosHomologacao(curso)) {
            matricularAlunoHomologacao(aluno, curso, periodo, oferta);
        }
    }

    private Professor professorHomologacao() {
        Usuario usuario = usuarioHomologacao();
        Professor professor = Professor.find("usuario = ?1", usuario).firstResult();
        if (professor == null) {
            professor = Professor.find("nome", "Professor Exemplo").firstResult();
        }
        if (professor == null) {
            professor = new Professor();
            professor.nome = "Professor Exemplo";
            professor.persist();
        }
        professor.usuario = usuario;
        return professor;
    }

    private Usuario usuarioHomologacao() {
        Usuario usuario = Usuario.find("email", "professor@seminario.local").firstResult();
        if (usuario == null) {
            usuario = new Usuario();
            usuario.email = "professor@seminario.local";
        }
        usuario.nome = "Professor Exemplo";
        usuario.senhaHash = senhaService.criptografar("Professor@123");
        usuario.perfil = Perfil.PROFESSOR;
        usuario.ativo = true;
        if (!usuario.isPersistent()) {
            usuario.persist();
        }
        return usuario;
    }

    private Modulo moduloHomologacao(Curso curso) {
        Modulo modulo = Modulo.find("curso = ?1 and ordem = ?2", curso, 1).firstResult();
        if (modulo == null) {
            modulo = new Modulo();
            modulo.curso = curso;
            modulo.ordem = 1;
        }
        modulo.nome = "Modulo 1";
        modulo.descricao = "Hermenêutica, Homilética e Apologética.";
        modulo.status = StatusModulo.ABERTO;
        modulo.ativo = true;
        if (!modulo.isPersistent()) {
            modulo.persist();
        }
        return modulo;
    }

    private Disciplina disciplinaHomologacao(Curso curso, Modulo modulo, Professor professor) {
        Disciplina disciplina = Disciplina.find("codigo", "HER-101").firstResult();
        if (disciplina == null) {
            disciplina = Disciplina.find("nome in ?1", List.of("Hermenêutica I", "Hermenêutica")).firstResult();
        }
        if (disciplina == null) {
            disciplina = new Disciplina();
            disciplina.codigo = "HER-101";
        }
        disciplina.curso = curso;
        disciplina.modulo = modulo;
        disciplina.professorResponsavel = professor;
        disciplina.nome = "Hermenêutica I";
        disciplina.cargaHoraria = disciplina.cargaHoraria == null ? 40 : disciplina.cargaHoraria;
        disciplina.creditos = disciplina.creditos == null ? 2 : disciplina.creditos;
        disciplina.ementaResumo = disciplina.ementaResumo == null ? "Introdução aos princípios de interpretação bíblica." : disciplina.ementaResumo;
        disciplina.ativo = true;
        if (!disciplina.isPersistent()) {
            disciplina.persist();
        }
        return disciplina;
    }

    private Turma turmaHomologacao(Curso curso, Disciplina disciplina, Professor professor) {
        Turma turma = Turma.find("nome", "Hermenêutica I - Noite - 2026").firstResult();
        if (turma == null) {
            turma = new Turma();
            turma.nome = "Hermenêutica I - Noite - 2026";
        }
        turma.curso = curso;
        turma.anoPeriodo = "2026";
        turma.turno = "Noite";
        turma.quantidadeMaximaAlunos = turma.quantidadeMaximaAlunos == null ? 30 : turma.quantidadeMaximaAlunos;
        turma.status = StatusTurma.EM_ANDAMENTO;
        if (!turma.isPersistent()) {
            turma.persist();
        }
        return turma;
    }

    private AnoLetivo anoLetivoHomologacao(Turma turma) {
        AnoLetivo ano = AnoLetivo.find("ano = ?1 and legado = false", 2026).firstResult();
        if (ano == null) {
            ano = new AnoLetivo();
            ano.ano = 2026;
        }
        ano.status = StatusAnoLetivo.EM_ANDAMENTO;
        ano.dataInicio = ano.dataInicio == null ? LocalDate.of(2026, 1, 1) : ano.dataInicio;
        ano.dataFim = ano.dataFim == null ? LocalDate.of(2026, 12, 31) : ano.dataFim;
        if (!ano.isPersistent()) {
            ano.persist();
        }
        return ano;
    }

    private PeriodoLetivo periodoLetivoHomologacao(AnoLetivo ano) {
        PeriodoLetivo periodo = PeriodoLetivo.find("anoLetivo = ?1 and nome = ?2", ano, "Modulo 1 - 2026").firstResult();
        if (periodo == null) {
            periodo = new PeriodoLetivo();
            periodo.anoLetivo = ano;
            periodo.nome = "Modulo 1 - 2026";
        }
        periodo.ordem = 1;
        periodo.tipo = TipoPeriodoLetivo.MODULO;
        periodo.status = StatusPeriodoLetivo.ABERTO;
        periodo.dataInicio = periodo.dataInicio == null ? LocalDate.of(2026, 1, 5) : periodo.dataInicio;
        periodo.dataFim = periodo.dataFim == null ? LocalDate.of(2026, 4, 30) : periodo.dataFim;
        if (!periodo.isPersistent()) {
            periodo.persist();
        }
        return periodo;
    }

    private OfertaDisciplina ofertaHomologacao(Turma turma, AnoLetivo ano, PeriodoLetivo periodo, Curso curso,
                                               Modulo modulo, Disciplina disciplina, Professor professor) {
        OfertaDisciplina oferta = OfertaDisciplina.find("turma = ?1 and disciplina = ?2 and periodoLetivo = ?3", turma, disciplina, periodo).firstResult();
        boolean novaOferta = oferta == null;
        if (oferta == null) {
            oferta = new OfertaDisciplina();
            oferta.turma = turma;
            oferta.disciplina = disciplina;
            oferta.periodoLetivo = periodo;
        }
        oferta.anoLetivo = ano;
        oferta.curso = curso;
        oferta.modulo = modulo;
        oferta.professor = professor;
        oferta.vagas = oferta.vagas == null ? 30 : oferta.vagas;
        oferta.horario = "Segunda-feira, 19h";
        oferta.sala = "Sala 1";
        oferta.cargaHorariaPrevista = disciplina.cargaHoraria;
        oferta.cargaHorariaMinistrada = oferta.cargaHorariaMinistrada == null ? 0 : oferta.cargaHorariaMinistrada;
        oferta.dataInicio = oferta.dataInicio == null ? LocalDate.of(2026, 1, 5) : oferta.dataInicio;
        oferta.dataFim = oferta.dataFim == null ? LocalDate.of(2026, 4, 30) : oferta.dataFim;
        if (novaOferta || oferta.status == null) {
            oferta.status = StatusOfertaDisciplina.EM_ANDAMENTO;
        }
        if (!oferta.isPersistent()) {
            oferta.persist();
        }
        return oferta;
    }

    private List<Aluno> alunosHomologacao(Curso curso) {
        return List.of(
                alunoHomologacao(curso, "Aluno Hermenêutica 01", "aluno.hermeneutica01@seminario.local"),
                alunoHomologacao(curso, "Aluno Hermenêutica 02", "aluno.hermeneutica02@seminario.local"),
                alunoHomologacao(curso, "Aluno Hermenêutica 03", "aluno.hermeneutica03@seminario.local"),
                alunoHomologacao(curso, "Aluno Hermenêutica 04", "aluno.hermeneutica04@seminario.local"),
                alunoHomologacao(curso, "Aluno Hermenêutica 05", "aluno.hermeneutica05@seminario.local"),
                alunoHomologacao(curso, "Aluno Hermenêutica 06", "aluno.hermeneutica06@seminario.local"),
                alunoHomologacao(curso, "Aluno Hermenêutica 07", "aluno.hermeneutica07@seminario.local"),
                alunoHomologacao(curso, "Aluno Hermenêutica 08", "aluno.hermeneutica08@seminario.local"),
                alunoHomologacao(curso, "Aluno Hermenêutica 09", "aluno.hermeneutica09@seminario.local"),
                alunoHomologacao(curso, "Aluno Hermenêutica 10", "aluno.hermeneutica10@seminario.local")
        );
    }

    private Aluno alunoHomologacao(Curso curso, String nome, String email) {
        Aluno aluno = Aluno.find("email", email).firstResult();
        if (aluno == null) {
            aluno = new Aluno();
            aluno.email = email;
        }
        aluno.nome = nome;
        aluno.curso = curso;
        aluno.status = StatusAluno.ATIVO;
        aluno.dataIngresso = aluno.dataIngresso == null ? LocalDate.of(2026, 1, 5) : aluno.dataIngresso;
        aluno.observacoes = aluno.observacoes == null ? "Aluno de homologação para testes da area do professor." : aluno.observacoes;
        if (!aluno.isPersistent()) {
            aluno.persist();
        }
        return aluno;
    }

    private void matricularAlunoHomologacao(Aluno aluno, Curso curso, PeriodoLetivo periodo, OfertaDisciplina oferta) {
        MatriculaDisciplina matricula = MatriculaDisciplina.find("aluno = ?1 and ofertaDisciplina = ?2", aluno, oferta).firstResult();
        if (matricula == null) {
            matricula = MatriculaDisciplina.find(
                    "aluno = ?1 and ofertaDisciplina.disciplina = ?2 and status not in ?3",
                    aluno, oferta.disciplina,
                    List.of(StatusMatriculaDisciplina.CANCELADO, StatusMatriculaDisciplina.TRANCADO)).firstResult();
        }
        if (matricula == null) {
            matricula = new MatriculaDisciplina();
            matricula.aluno = aluno;
            matricula.ofertaDisciplina = oferta;
            matricula.curso = curso;
            matricula.periodoLetivo = periodo;
            matricula.dataMatricula = LocalDate.of(2026, 1, 5);
            matricula.status = StatusMatriculaDisciplina.MATRICULADO;
            try {
                academicoService.matricularEmDisciplina(matricula);
            } catch (ApiException erro) {
                if (erro.status != Response.Status.CONFLICT) throw erro;
            }
            return;
        }
        matricula.curso = curso;
        matricula.periodoLetivo = periodo;
        if (matricula.status == StatusMatriculaDisciplina.CANCELADO || matricula.status == StatusMatriculaDisciplina.TRANCADO) {
            matricula.status = StatusMatriculaDisciplina.MATRICULADO;
        }
    }

    private void garantirGradePlaceholder(Curso curso) {
        boolean semArquivo = curso.gradePdfCaminho == null || !Files.exists(Path.of(curso.gradePdfCaminho));
        boolean placeholder = curso.gradePdfNome == null || curso.gradePdfNome.equals("grade-curricular-teologia-placeholder.pdf");
        if (semArquivo && placeholder) {
            anexarGradePlaceholder(curso);
        }
    }

    private void limparReferenciasDeArquivosAusentes() {
        Curso.<Curso>listAll().forEach(curso -> {
            if (curso.gradePdfCaminho != null && !Files.exists(Path.of(curso.gradePdfCaminho))) {
                curso.gradePdfCaminho = null;
                curso.gradePdfNome = null;
                curso.gradePdfTipo = null;
                curso.gradePdfTamanho = null;
            }
        });
        Disciplina.<Disciplina>listAll().forEach(disciplina -> {
            if (disciplina.ementaPdfCaminho != null && !Files.exists(Path.of(disciplina.ementaPdfCaminho))) {
                disciplina.ementaPdfCaminho = null;
                disciplina.ementaPdfNome = null;
                disciplina.ementaPdfTipo = null;
                disciplina.ementaPdfTamanho = null;
            }
        });
        PlanoEnsino.<PlanoEnsino>listAll().forEach(plano -> {
            if (plano.planoPdfCaminho != null && !Files.exists(Path.of(plano.planoPdfCaminho))) {
                plano.planoPdfCaminho = null;
                plano.planoPdfNome = null;
                plano.planoPdfTipo = null;
                plano.planoPdfTamanho = null;
            }
        });
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
        oferta.status = StatusOfertaDisciplina.EM_ANDAMENTO;
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

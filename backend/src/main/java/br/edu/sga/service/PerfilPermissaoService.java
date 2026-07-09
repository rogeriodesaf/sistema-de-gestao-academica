package br.edu.sga.service;

import br.edu.sga.dto.AtualizarPerfilPermissoesDTO;
import br.edu.sga.dto.PerfilPermissaoDTO;
import br.edu.sga.dto.PerfilPermissoesDTO;
import br.edu.sga.dto.PerfilResumoDTO;
import br.edu.sga.entity.PerfilPermissao;
import br.edu.sga.enums.Perfil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PerfilPermissaoService {
    private static final List<PerfilResumoDTO> PERFIS = List.of(
            new PerfilResumoDTO(Perfil.ADMINISTRADOR, "Administrador", "Acesso total ao sistema"),
            new PerfilResumoDTO(Perfil.COORDENADOR, "Coordenador", "Gestao academica"),
            new PerfilResumoDTO(Perfil.SECRETARIA, "Secretaria", "Operacoes academicas"),
            new PerfilResumoDTO(Perfil.PROFESSOR, "Professor", "Area docente"),
            new PerfilResumoDTO(Perfil.ALUNO, "Aluno", "Portal do aluno")
    );

    private static final List<RecursoPermissao> RECURSOS = List.of(
            new RecursoPermissao("ACADEMICO", "Cursos"),
            new RecursoPermissao("ACADEMICO", "Matriz Curricular"),
            new RecursoPermissao("ACADEMICO", "Disciplinas"),
            new RecursoPermissao("ACADEMICO", "Modulos"),
            new RecursoPermissao("ACADEMICO", "Turmas"),
            new RecursoPermissao("ACADEMICO", "Ofertas de Disciplinas"),
            new RecursoPermissao("ACADEMICO", "Montagem do Periodo"),
            new RecursoPermissao("PESSOAS", "Alunos"),
            new RecursoPermissao("PESSOAS", "Professores"),
            new RecursoPermissao("AVALIACAO", "Matriculas"),
            new RecursoPermissao("AVALIACAO", "Frequencia"),
            new RecursoPermissao("AVALIACAO", "Notas"),
            new RecursoPermissao("CONSULTAS", "Historico Escolar"),
            new RecursoPermissao("CONSULTAS", "Relatorios"),
            new RecursoPermissao("ADMINISTRACAO", "Usuarios"),
            new RecursoPermissao("ADMINISTRACAO", "Perfis"),
            new RecursoPermissao("ADMINISTRACAO", "Configuracoes")
    );

    public List<PerfilResumoDTO> listarPerfis() {
        return PERFIS;
    }

    @Transactional
    public PerfilPermissoesDTO permissoes(Perfil perfil) {
        garantirPermissoes(perfil);
        return new PerfilPermissoesDTO(perfil, listarPermissoes(perfil));
    }

    @Transactional
    public PerfilPermissoesDTO atualizar(Perfil perfil, AtualizarPerfilPermissoesDTO dto) {
        garantirPermissoes(perfil);
        Map<String, PerfilPermissaoDTO> entrada = dto.permissoes().stream()
                .collect(java.util.stream.Collectors.toMap(PerfilPermissaoDTO::recurso, item -> item));

        List<PerfilPermissao> permissoes = PerfilPermissao.list("perfil", perfil);
        for (PerfilPermissao permissao : permissoes) {
            PerfilPermissaoDTO nova = entrada.get(permissao.recurso);
            if (nova == null) continue;
            if (perfil == Perfil.ADMINISTRADOR) {
                marcarTudo(permissao);
            } else {
                permissao.visualizar = nova.visualizar();
                permissao.criar = nova.criar();
                permissao.editar = nova.editar();
                permissao.excluir = nova.excluir();
            }
        }
        return new PerfilPermissoesDTO(perfil, listarPermissoes(perfil));
    }

    private List<PerfilPermissaoDTO> listarPermissoes(Perfil perfil) {
        return PerfilPermissao.<PerfilPermissao>list("perfil", perfil).stream()
                .sorted(Comparator.comparing((PerfilPermissao p) -> ordemArea(p.area)).thenComparing(p -> p.recurso))
                .map(PerfilPermissaoDTO::de)
                .toList();
    }

    private void garantirPermissoes(Perfil perfil) {
        for (RecursoPermissao recurso : RECURSOS) {
            if (PerfilPermissao.count("perfil = ?1 and recurso = ?2", perfil, recurso.nome()) > 0) continue;
            PerfilPermissao permissao = new PerfilPermissao();
            permissao.perfil = perfil;
            permissao.area = recurso.area();
            permissao.recurso = recurso.nome();
            aplicarPadrao(permissao);
            permissao.persist();
        }
    }

    private void aplicarPadrao(PerfilPermissao permissao) {
        switch (permissao.perfil) {
            case ADMINISTRADOR -> marcarTudo(permissao);
            case COORDENADOR -> padraoCoordenador(permissao);
            case SECRETARIA -> padraoSecretaria(permissao);
            case PROFESSOR -> padraoProfessor(permissao);
            case ALUNO -> padraoAluno(permissao);
        }
    }

    private void marcarTudo(PerfilPermissao permissao) {
        permissao.visualizar = true;
        permissao.criar = true;
        permissao.editar = true;
        permissao.excluir = true;
    }

    private void padraoCoordenador(PerfilPermissao permissao) {
        if ("ACADEMICO".equals(permissao.area)) {
            marcarTudo(permissao);
        } else if ("PESSOAS".equals(permissao.area) || "AVALIACAO".equals(permissao.area) || "CONSULTAS".equals(permissao.area)) {
            permissao.visualizar = true;
        }
    }

    private void padraoSecretaria(PerfilPermissao permissao) {
        if ("ACADEMICO".equals(permissao.area) || "CONSULTAS".equals(permissao.area)) {
            permissao.visualizar = true;
        }
        if (List.of("Alunos", "Professores", "Matriculas").contains(permissao.recurso)) {
            marcarTudo(permissao);
        }
    }

    private void padraoProfessor(PerfilPermissao permissao) {
        if (List.of("Disciplinas", "Turmas", "Alunos", "Frequencia", "Notas").contains(permissao.recurso)) {
            permissao.visualizar = true;
        }
        if (List.of("Frequencia", "Notas").contains(permissao.recurso)) {
            permissao.criar = true;
            permissao.editar = true;
        }
    }

    private void padraoAluno(PerfilPermissao permissao) {
        if (List.of("Disciplinas", "Frequencia", "Notas", "Historico Escolar").contains(permissao.recurso)) {
            permissao.visualizar = true;
        }
    }

    private int ordemArea(String area) {
        return switch (area) {
            case "ACADEMICO" -> 1;
            case "PESSOAS" -> 2;
            case "AVALIACAO" -> 3;
            case "CONSULTAS" -> 4;
            case "ADMINISTRACAO" -> 5;
            default -> 99;
        };
    }

    private record RecursoPermissao(String area, String nome) {
    }
}

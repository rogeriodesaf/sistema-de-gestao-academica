package br.edu.sga.resource;

import br.edu.sga.entity.AnoLetivo;
import br.edu.sga.entity.Disciplina;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.PeriodoLetivo;
import br.edu.sga.entity.Professor;
import br.edu.sga.entity.Turma;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OfertaDisciplinaTurmaTest {
    @Test
    void deveListarTurmaPeloNomeEPermitirNovaOfertaNaMesmaTurma() {
        String token = given()
                .contentType("application/json")
                .body(Map.of("email", "admin@sga.local", "senha", "admin123"))
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .extract().path("token");

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/api/turmas/opcoes?tamanho=500")
                .then().statusCode(200)
                .body("find { it.nome == 'Turma Teologia 2026' }.nome", equalTo("Turma Teologia 2026"))
                .body("find { it.nome == 'Turma Teologia 2026' }", not(hasKey("disciplina")))
                .body("find { it.nome == 'Turma Teologia 2026' }", not(hasKey("horario")));

        Turma turma = Turma.find("nome", "Turma Teologia 2026").firstResult();
        Disciplina disciplina = Disciplina.find("nome", "Introdução Filosofia").firstResult();
        AnoLetivo ano = AnoLetivo.find("ano", 2026).firstResult();
        PeriodoLetivo periodo = PeriodoLetivo.find("anoLetivo", ano).firstResult();
        Professor professor = Professor.find("email", "professor@sga.local").firstResult();
        List<OfertaDisciplina> ofertasAnteriores = OfertaDisciplina.list("turma", turma);
        Set<Long> idsAnteriores = ofertasAnteriores.stream().map(oferta -> oferta.id).collect(Collectors.toSet());

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(Map.ofEntries(
                        Map.entry("turma", Map.of("id", turma.id)),
                        Map.entry("anoLetivo", Map.of("id", ano.id)),
                        Map.entry("periodoLetivo", Map.of("id", periodo.id)),
                        Map.entry("curso", Map.of("id", disciplina.curso.id)),
                        Map.entry("modulo", Map.of("id", disciplina.modulo.id)),
                        Map.entry("disciplina", Map.of("id", disciplina.id)),
                        Map.entry("professor", Map.of("id", professor.id)),
                        Map.entry("vagas", 30),
                        Map.entry("horario", "Quinta 19h"),
                        Map.entry("sala", "Sala 2"),
                        Map.entry("status", "ABERTA")))
                .when().post("/api/ofertas-disciplinas")
                .then().statusCode(200)
                .body("turma.id", equalTo(turma.id.intValue()))
                .body("turma.nome", equalTo("Turma Teologia 2026"))
                .body("disciplina.nome", equalTo("Introdução Filosofia"));

        List<OfertaDisciplina> ofertasAtuais = OfertaDisciplina.list("turma", turma);
        Set<Long> idsAtuais = ofertasAtuais.stream().map(oferta -> oferta.id).collect(Collectors.toSet());
        assertEquals(ofertasAnteriores.size() + 1, ofertasAtuais.size());
        assertTrue(idsAtuais.containsAll(idsAnteriores));
    }

    @Test
    void deveCadastrarTurmaSomenteComDadosAdministrativosEFiltrarOpcoes() {
        String token = token("admin@sga.local", "admin123");
        AnoLetivo ano = AnoLetivo.find("ano", 2026).firstResult();
        PeriodoLetivo periodo = PeriodoLetivo.find("anoLetivo", ano).firstResult();
        Disciplina disciplina = Disciplina.find("curso is not null").firstResult();
        String nome = "Turma Administrativa " + UUID.randomUUID();

        Number turmaIdExtraido = given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(Map.of(
                        "nome", nome,
                        "curso", Map.of("id", disciplina.curso.id),
                        "anoLetivo", Map.of("id", ano.id),
                        "periodoLetivo", Map.of("id", periodo.id),
                        "turno", "Noite",
                        "quantidadeMaximaAlunos", 40,
                        "status", "PLANEJADA"))
                .when().post("/api/turmas")
                .then().statusCode(200)
                .body("$", not(hasKey("disciplina")))
                .body("$", not(hasKey("professor")))
                .body("$", not(hasKey("horario")))
                .body("$", not(hasKey("sala")))
                .extract().path("id");
        Long turmaId = turmaIdExtraido.longValue();

        Turma turma = Turma.findById(turmaId);
        assertEquals(null, turma.disciplina);
        assertEquals(null, turma.professor);
        assertEquals(null, turma.horario);
        assertEquals(null, turma.sala);

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("anoLetivoId", ano.id)
                .queryParam("periodoLetivoId", periodo.id)
                .queryParam("cursoId", disciplina.curso.id)
                .when().get("/api/turmas/opcoes")
                .then().statusCode(200)
                .body("find { it.id == " + turmaId + " }.nome", equalTo(nome));
    }

    @Test
    void portalDeveListarSomenteOfertasDoProfessorAutenticado() {
        String token = token("professor@sga.local", "123456");
        List<Map<String, Object>> ofertas = given()
                .header("Authorization", "Bearer " + token)
                .when().get("/api/professor/ofertas")
                .then().statusCode(200)
                .extract().jsonPath().getList("$");

        assertTrue(!ofertas.isEmpty());
        assertTrue(ofertas.stream().allMatch(oferta -> {
            Map<?, ?> professor = (Map<?, ?>) oferta.get("professor");
            return professor != null && "professor@sga.local".equals(professor.get("email"));
        }));
    }

    private String token(String email, String senha) {
        return given()
                .contentType("application/json")
                .body(Map.of("email", email, "senha", senha))
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .extract().path("token");
    }
}

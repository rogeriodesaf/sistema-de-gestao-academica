package br.edu.sga.resource;

import br.edu.sga.entity.AnoLetivo;
import br.edu.sga.entity.Disciplina;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.Professor;
import br.edu.sga.entity.Turma;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        Professor professor = Professor.find("email", "professor@sga.local").firstResult();
        List<OfertaDisciplina> ofertasAnteriores = OfertaDisciplina.list("turma", turma);
        Set<Long> idsAnteriores = ofertasAnteriores.stream().map(oferta -> oferta.id).collect(Collectors.toSet());

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(Map.of(
                        "turma", Map.of("id", turma.id),
                        "anoLetivo", Map.of("id", ano.id),
                        "curso", Map.of("id", disciplina.curso.id),
                        "modulo", Map.of("id", disciplina.modulo.id),
                        "disciplina", Map.of("id", disciplina.id),
                        "professor", Map.of("id", professor.id),
                        "vagas", 30,
                        "horario", "Quinta 19h",
                        "sala", "Sala 2",
                        "status", "ABERTA"))
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
}

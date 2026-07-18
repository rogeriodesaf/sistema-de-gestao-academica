package br.edu.sga.resource;

import br.edu.sga.entity.Aluno;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AreaAlunoResourceTest {
    @BeforeEach
    @Transactional
    void simularCadastroSemVinculo() {
        Aluno aluno = Aluno.find("email", "aluno@sga.local").firstResult();
        aluno.usuario = null;
    }

    @Test
    void deveVincularUsuarioEncontrarAlunoECarregarDisciplinas() {
        String token = given()
                .contentType("application/json")
                .body("{\"email\":\"aluno@sga.local\",\"senha\":\"123456\"}")
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .body("perfil", is("ALUNO"))
                .extract().path("token");

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/api/aluno/me")
                .then().statusCode(200)
                .body("email", is("aluno@sga.local"));

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/api/aluno/disciplinas")
                .then().statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    void deveVincularUsuarioAlunoDuranteCadastro() {
        String token = given()
                .contentType("application/json")
                .body("{\"email\":\"admin@sga.local\",\"senha\":\"admin123\"}")
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .extract().path("token");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"nome\":\"Aluno Vinculado\",\"email\":\"aluno.vinculado@sga.local\","
                        + "\"senha\":\"Aluno@123\",\"confirmarSenha\":\"Aluno@123\","
                        + "\"perfil\":\"ALUNO\",\"ativo\":true}")
                .when().post("/api/usuarios")
                .then().statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"nome\":\"Aluno Vinculado\",\"email\":\"aluno.vinculado@sga.local\","
                        + "\"status\":\"ATIVO\"}")
                .when().post("/api/alunos")
                .then().statusCode(200)
                .body("usuario.id", notNullValue())
                .body("usuario.email", is("aluno.vinculado@sga.local"));
    }
}

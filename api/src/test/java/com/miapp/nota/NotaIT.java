package com.miapp.nota;

import java.net.URI;
import java.util.List;

import com.miapp.nota.dto.CrearNota;
import com.miapp.nota.dto.NotaResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class NotaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17"))
        .withReuse(true);

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void elDatoEscritoPorLaApiSeLeeDeVuelta() {
        ResponseEntity<NotaResponse> creada = restTemplate.postForEntity(
            "/api/notas", new CrearNota("hola"), NotaResponse.class);

        List<NotaResponse> notas = restTemplate.exchange(
                "/api/notas", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<NotaResponse>>() { })
            .getBody();

        assertThat(notas).extracting(NotaResponse::texto).contains("hola");
        assertThat(notas).extracting(NotaResponse::id).contains(creada.getBody().id());
    }

    @Test
    void laCreacionDevuelve201ConLaCabeceraLocation() {
        ResponseEntity<NotaResponse> respuesta = restTemplate.postForEntity(
            "/api/notas", new CrearNota("otra nota"), NotaResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        URI location = respuesta.getHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.toString()).isEqualTo("/api/notas/" + respuesta.getBody().id());
    }

    @Test
    void unTextoVacioDevuelve400ConProblemDetail() {
        ResponseEntity<ProblemDetail> respuesta = restTemplate.postForEntity(
            "/api/notas", new CrearNota(""), ProblemDetail.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ProblemDetail cuerpo = respuesta.getBody();
        assertThat(cuerpo).isNotNull();
        assertThat(cuerpo.getType()).isNotNull();
        assertThat(cuerpo.getTitle()).isNotNull();
        assertThat(cuerpo.getStatus()).isEqualTo(400);
        assertThat(cuerpo.getDetail()).isNotNull();
        assertThat(cuerpo.getInstance()).isNotNull();
    }

}

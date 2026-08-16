package com.miapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EsquemaOpenApiTests {

    @Autowired
    MockMvc mockMvc;

    private JsonNode esquema() throws Exception {
        String cuerpo = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return new ObjectMapper().readTree(cuerpo);
    }

    @Test
    void elEsquemaDeclaraLasRutasDeNotas() throws Exception {
        JsonNode rutaNotas = esquema().path("paths").path("/api/notas");
        assertThat(rutaNotas.has("get")).isTrue();
        assertThat(rutaNotas.has("post")).isTrue();
    }

    @Test
    void crearNotaExponeSoloElCampoTextoConSusRestricciones() throws Exception {
        JsonNode crearNota = esquema().path("components").path("schemas").path("CrearNota");
        JsonNode propiedades = crearNota.path("properties");

        assertThat(propiedades.properties()).hasSize(1);
        assertThat(propiedades.has("texto")).isTrue();
        assertThat(propiedades.path("texto").path("maxLength").asInt()).isEqualTo(500);

        List<String> requeridos = new ArrayList<>();
        crearNota.path("required").forEach(nodo -> requeridos.add(nodo.asText()));
        assertThat(requeridos).containsExactly("texto");
    }

    @Test
    void notaResponseExponeIdTextoYCreadaEn() throws Exception {
        JsonNode propiedades = esquema().path("components").path("schemas").path("NotaResponse").path("properties");

        assertThat(propiedades.properties()).hasSize(3);
        assertThat(propiedades.has("id")).isTrue();
        assertThat(propiedades.has("texto")).isTrue();
        assertThat(propiedades.has("creadaEn")).isTrue();
    }

}

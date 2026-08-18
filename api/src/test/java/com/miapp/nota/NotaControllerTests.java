package com.miapp.nota;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.miapp.nota.dto.NotaResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotaController.class)
class NotaControllerTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NotaService notaService;

    @Test
    void creaUnaNotaYDevuelve201ConLocationYCuerpo() throws Exception {
        UUID id = UUID.randomUUID();
        Instant creadaEn = Instant.parse("2026-01-01T00:00:00Z");
        given(notaService.crear(any())).willReturn(new NotaResponse(id, "hola", creadaEn));

        mockMvc.perform(post("/api/notas")
                .contentType("application/json")
                .content("{\"texto\":\"hola\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/notas/" + id))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.texto").value("hola"))
            .andExpect(jsonPath("$.creadaEn").value("2026-01-01T00:00:00Z"));
    }

    @Test
    void ignoraElCampoCreadaEnEnviadoPorElCliente() throws Exception {
        given(notaService.crear(any())).willReturn(new NotaResponse(UUID.randomUUID(), "hola", Instant.now()));

        mockMvc.perform(post("/api/notas")
                .contentType("application/json")
                .content("{\"texto\":\"hola\",\"creadaEn\":\"1999-01-01T00:00:00Z\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void listaLasNotasCreadas() throws Exception {
        NotaResponse nota = new NotaResponse(UUID.randomUUID(), "hola", Instant.now());
        given(notaService.listar()).willReturn(List.of(nota));

        mockMvc.perform(get("/api/notas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].texto").value("hola"));
    }

    @Test
    void listaVariasNotasCreadasEnElMismoOrden() throws Exception {
        NotaResponse primera = new NotaResponse(UUID.randomUUID(), "primera", Instant.now());
        NotaResponse segunda = new NotaResponse(UUID.randomUUID(), "segunda", Instant.now());
        given(notaService.listar()).willReturn(List.of(primera, segunda));

        mockMvc.perform(get("/api/notas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].texto").value("primera"))
            .andExpect(jsonPath("$[1].texto").value("segunda"));
    }

    @Test
    void listaVaciaDevuelveArregloVacio() throws Exception {
        given(notaService.listar()).willReturn(List.of());

        mockMvc.perform(get("/api/notas"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void rechazaTextoEnBlanco() throws Exception {
        mockMvc.perform(post("/api/notas")
                .contentType("application/json")
                .content("{\"texto\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaTextoDeMasDe500Caracteres() throws Exception {
        String textoLargo = "a".repeat(501);

        mockMvc.perform(post("/api/notas")
                .contentType("application/json")
                .content("{\"texto\":\"" + textoLargo + "\"}"))
            .andExpect(status().isBadRequest());
    }

}

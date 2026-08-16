package com.miapp.nota;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotaController.class)
class NotaControllerManejoErroresTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NotaService notaService;

    @Test
    void unFalloDeValidacionDevuelveProblemDetailConElCampoInvalido() throws Exception {
        mockMvc.perform(post("/api/notas")
                .contentType("application/json")
                .content("{\"texto\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail", containsString("texto")))
            .andExpect(jsonPath("$.instance").value("/api/notas"));
    }

    @Test
    void unJsonMalformadoDevuelveLaMismaFormaDeError() throws Exception {
        mockMvc.perform(post("/api/notas")
                .contentType("application/json")
                .content("{no-es-json"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.title").exists())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").exists())
            .andExpect(jsonPath("$.instance").value("/api/notas"));
    }

    @Test
    void unErrorInesperadoDevuelve500SinFiltrarDetallesInternos() throws Exception {
        given(notaService.listar()).willThrow(new RuntimeException("detalle interno sensible"));

        mockMvc.perform(get("/api/notas"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.instance").value("/api/notas"))
            .andExpect(jsonPath("$.detail", not(containsString("detalle interno sensible"))))
            .andExpect(jsonPath("$.detail", not(containsString("RuntimeException"))));
    }

}

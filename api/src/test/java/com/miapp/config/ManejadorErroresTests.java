package com.miapp.config;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ManejadorErroresTests {

    private final ManejadorErrores manejadorErrores = new ManejadorErrores();

    @Test
    void unFalloDeValidacionDevuelveProblemDetailConElCampoInvalido() throws Exception {
        Method metodoFalso = getClass().getDeclaredMethod("metodoDePrueba", String.class);
        MethodParameter parametro = new MethodParameter(metodoFalso, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "objeto");
        bindingResult.addError(new FieldError("objeto", "texto", "no debe estar vacío"));
        MethodArgumentNotValidException excepcion = new MethodArgumentNotValidException(parametro, bindingResult);
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/health"));

        ResponseEntity<Object> respuesta = manejadorErrores.handleMethodArgumentNotValid(
            excepcion, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        ProblemDetail cuerpo = (ProblemDetail) respuesta.getBody();
        assertThat(cuerpo).isNotNull();
        assertThat(cuerpo.getStatus()).isEqualTo(400);
        assertThat(cuerpo.getDetail()).contains("texto");
        assertThat(cuerpo.getInstance()).isEqualTo(java.net.URI.create("/api/health"));
    }

    @Test
    void unJsonMalformadoDevuelveLaMismaFormaDeError() {
        HttpMessageNotReadableException excepcion = new HttpMessageNotReadableException(
            "JSON inválido", mock(HttpInputMessage.class));
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/health"));

        ResponseEntity<Object> respuesta = manejadorErrores.handleHttpMessageNotReadable(
            excepcion, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        ProblemDetail cuerpo = (ProblemDetail) respuesta.getBody();
        assertThat(cuerpo).isNotNull();
        assertThat(cuerpo.getStatus()).isEqualTo(400);
        assertThat(cuerpo.getDetail()).isEqualTo("El cuerpo de la petición no es JSON válido");
    }

    @Test
    void unErrorInesperadoDevuelve500SinFiltrarDetallesInternos() {
        RuntimeException excepcion = new RuntimeException("detalle interno sensible");
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/health"));

        ResponseEntity<Object> respuesta = manejadorErrores.manejarErrorInesperado(excepcion, request);

        ProblemDetail cuerpo = (ProblemDetail) respuesta.getBody();
        assertThat(cuerpo).isNotNull();
        assertThat(cuerpo.getStatus()).isEqualTo(500);
        assertThat(cuerpo.getDetail()).doesNotContain("detalle interno sensible");
    }

    @SuppressWarnings("unused")
    private void metodoDePrueba(String valor) {
    }

}

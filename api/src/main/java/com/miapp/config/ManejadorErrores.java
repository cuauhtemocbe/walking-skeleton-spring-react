package com.miapp.config;

import java.net.URI;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);
    private static final URI TIPO_GENERICO = URI.create("about:blank");

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        ProblemDetail problemDetail = problemDetail(status, detalle, request);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problemDetail = problemDetail(status, "El cuerpo de la petición no es JSON válido", request);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> manejarErrorInesperado(Exception ex, WebRequest request) {
        log.error("Error no controlado atendiendo {}", request.getDescription(false), ex);
        ProblemDetail problemDetail = problemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", request);
        return handleExceptionInternal(ex, problemDetail, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private static ProblemDetail problemDetail(HttpStatusCode status, String detalle, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detalle);
        problemDetail.setType(TIPO_GENERICO);
        problemDetail.setInstance(instancia(request));
        return problemDetail;
    }

    private static URI instancia(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return URI.create(servletWebRequest.getRequest().getRequestURI());
        }
        return null;
    }

}

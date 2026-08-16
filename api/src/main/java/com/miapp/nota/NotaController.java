package com.miapp.nota;

import java.net.URI;
import java.util.List;

import com.miapp.nota.dto.CrearNota;
import com.miapp.nota.dto.NotaResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class NotaController {

    private final NotaService notaService;

    NotaController(NotaService notaService) {
        this.notaService = notaService;
    }

    @PostMapping("/api/notas")
    ResponseEntity<NotaResponse> crear(@Valid @RequestBody CrearNota crearNota) {
        NotaResponse creada = notaService.crear(crearNota);
        return ResponseEntity.created(URI.create("/api/notas/" + creada.id())).body(creada);
    }

    @GetMapping("/api/notas")
    List<NotaResponse> listar() {
        return notaService.listar();
    }

}

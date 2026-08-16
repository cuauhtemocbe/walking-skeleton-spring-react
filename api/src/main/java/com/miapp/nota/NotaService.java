package com.miapp.nota;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.miapp.nota.dto.CrearNota;
import com.miapp.nota.dto.NotaResponse;

import org.springframework.stereotype.Service;

@Service
class NotaService {

    private final NotaRepository notaRepository;

    NotaService(NotaRepository notaRepository) {
        this.notaRepository = notaRepository;
    }

    NotaResponse crear(CrearNota crearNota) {
        Nota nota = new Nota(UUID.randomUUID(), crearNota.texto(), Instant.now());
        Nota guardada = notaRepository.save(nota);
        return aRespuesta(guardada);
    }

    List<NotaResponse> listar() {
        return notaRepository.findAll().stream()
            .map(NotaService::aRespuesta)
            .toList();
    }

    private static NotaResponse aRespuesta(Nota nota) {
        return new NotaResponse(nota.getId(), nota.getTexto(), nota.getCreadaEn());
    }

}

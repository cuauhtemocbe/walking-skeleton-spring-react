package com.miapp.nota;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.miapp.nota.dto.CrearNota;
import com.miapp.nota.dto.NotaResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotaServiceTests {

    @Mock
    NotaRepository notaRepository;

    NotaService notaService;

    @Test
    void alCrearAsignaIdYFechaDeCreacionEnElServidor() {
        notaService = new NotaService(notaRepository);
        given(notaRepository.save(any())).willAnswer(invocacion -> invocacion.getArgument(0));

        NotaResponse respuesta = notaService.crear(new CrearNota("hola"));

        ArgumentCaptor<Nota> notaGuardada = ArgumentCaptor.forClass(Nota.class);
        verify(notaRepository).save(notaGuardada.capture());
        assertThat(respuesta.id()).isEqualTo(notaGuardada.getValue().getId());
        assertThat(respuesta.texto()).isEqualTo("hola");
        assertThat(respuesta.creadaEn()).isNotNull();
    }

    @Test
    void alListarMapeaLasEntidadesGuardadasARespuestas() {
        notaService = new NotaService(notaRepository);
        Nota nota = new Nota(UUID.randomUUID(), "hola", Instant.now());
        given(notaRepository.findAll()).willReturn(List.of(nota));

        List<NotaResponse> respuesta = notaService.listar();

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.get(0).texto()).isEqualTo("hola");
    }

}

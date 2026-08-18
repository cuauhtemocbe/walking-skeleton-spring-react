package com.miapp.cliente;

import java.util.List;
import java.util.Optional;

import com.miapp.cliente.dto.ActualizarCliente;
import com.miapp.cliente.dto.ClienteResponse;
import com.miapp.cliente.dto.CrearCliente;
import com.miapp.cliente.excepciones.ClienteNoEncontradoException;
import com.miapp.cliente.excepciones.RfcDuplicadoException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTests {

    @Mock
    ClienteRepository clienteRepository;

    ClienteService clienteService;

    @Test
    void alCrearGuardaElClienteConLosDatosRecibidos() {
        clienteService = new ClienteService(clienteRepository);
        given(clienteRepository.findByRfc("AAA010101AAA")).willReturn(Optional.empty());
        given(clienteRepository.save(any())).willAnswer(invocacion -> invocacion.getArgument(0));

        ClienteResponse respuesta = clienteService.crear(
            new CrearCliente("Ferretería Truper", "AAA010101AAA", "Truper S.A. de C.V."));

        ArgumentCaptor<Cliente> clienteGuardado = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(clienteGuardado.capture());
        assertThat(respuesta.nombre()).isEqualTo("Ferretería Truper");
        assertThat(respuesta.rfc()).isEqualTo(clienteGuardado.getValue().getRfc());
        assertThat(respuesta.razonSocial()).isEqualTo("Truper S.A. de C.V.");
    }

    @Test
    void alCrearConRfcYaRegistradoLanzaExcepcion() {
        clienteService = new ClienteService(clienteRepository);
        Cliente existente = new Cliente("Otro", "AAA010101AAA", "Otro S.A.");
        ReflectionTestUtils.setField(existente, "id", 5L);
        given(clienteRepository.findByRfc("AAA010101AAA")).willReturn(Optional.of(existente));

        assertThatThrownBy(() -> clienteService.crear(
            new CrearCliente("Nuevo", "AAA010101AAA", "Nuevo S.A.")))
            .isInstanceOf(RfcDuplicadoException.class);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void alListarMapeaLasEntidadesGuardadasARespuestas() {
        clienteService = new ClienteService(clienteRepository);
        Cliente cliente = new Cliente("Ferretería Truper", "AAA010101AAA", "Truper S.A. de C.V.");
        given(clienteRepository.findAll()).willReturn(List.of(cliente));

        List<ClienteResponse> respuesta = clienteService.listar();

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.get(0).nombre()).isEqualTo("Ferretería Truper");
    }

    @Test
    void alListarMapeaVariosClientesEnElMismoOrden() {
        clienteService = new ClienteService(clienteRepository);
        Cliente primero = new Cliente("Primero", "AAA010101AAA", "Primero S.A.");
        Cliente segundo = new Cliente("Segundo", "BBB020202BBB", "Segundo S.A.");
        Cliente tercero = new Cliente("Tercero", "CCC030303CCC", "Tercero S.A.");
        given(clienteRepository.findAll()).willReturn(List.of(primero, segundo, tercero));

        List<ClienteResponse> respuesta = clienteService.listar();

        assertThat(respuesta).extracting(ClienteResponse::nombre)
            .containsExactly("Primero", "Segundo", "Tercero");
    }

    @Test
    void alBuscarPorRfcExistenteDevuelveElCliente() {
        clienteService = new ClienteService(clienteRepository);
        Cliente cliente = new Cliente("Ferretería Truper", "AAA010101AAA", "Truper S.A. de C.V.");
        given(clienteRepository.findByRfc("AAA010101AAA")).willReturn(Optional.of(cliente));

        Optional<ClienteResponse> respuesta = clienteService.buscarPorRfc("AAA010101AAA");

        assertThat(respuesta).isPresent();
        assertThat(respuesta.get().rfc()).isEqualTo("AAA010101AAA");
    }

    @Test
    void alBuscarPorRfcInexistenteDevuelveVacio() {
        clienteService = new ClienteService(clienteRepository);
        given(clienteRepository.findByRfc("ZZZ999999ZZZ")).willReturn(Optional.empty());

        Optional<ClienteResponse> respuesta = clienteService.buscarPorRfc("ZZZ999999ZZZ");

        assertThat(respuesta).isEmpty();
    }

    @Test
    void alActualizarCambiaLosDatosDelCliente() {
        clienteService = new ClienteService(clienteRepository);
        Cliente cliente = new Cliente("Viejo nombre", "AAA010101AAA", "Vieja razón social");
        ReflectionTestUtils.setField(cliente, "id", 1L);
        given(clienteRepository.findById(1L)).willReturn(Optional.of(cliente));
        given(clienteRepository.findByRfc("AAA010101AAA")).willReturn(Optional.of(cliente));

        ClienteResponse respuesta = clienteService.actualizar(1L,
            new ActualizarCliente("Nuevo nombre", "AAA010101AAA", "Nueva razón social"));

        assertThat(respuesta.nombre()).isEqualTo("Nuevo nombre");
        assertThat(respuesta.razonSocial()).isEqualTo("Nueva razón social");
    }

    @Test
    void alActualizarUnClienteInexistenteLanzaExcepcion() {
        clienteService = new ClienteService(clienteRepository);
        given(clienteRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.actualizar(1L,
            new ActualizarCliente("Nombre", "AAA010101AAA", "Razón social")))
            .isInstanceOf(ClienteNoEncontradoException.class);
    }

    @Test
    void alActualizarConRfcDeOtroClienteLanzaExcepcion() {
        clienteService = new ClienteService(clienteRepository);
        Cliente cliente = new Cliente("Nombre", "AAA010101AAA", "Razón social");
        ReflectionTestUtils.setField(cliente, "id", 1L);
        Cliente otro = new Cliente("Otro", "BBB020202BBB", "Otro S.A.");
        ReflectionTestUtils.setField(otro, "id", 2L);
        given(clienteRepository.findById(1L)).willReturn(Optional.of(cliente));
        given(clienteRepository.findByRfc("BBB020202BBB")).willReturn(Optional.of(otro));

        assertThatThrownBy(() -> clienteService.actualizar(1L,
            new ActualizarCliente("Nombre", "BBB020202BBB", "Razón social")))
            .isInstanceOf(RfcDuplicadoException.class);
    }

    @Test
    void alEliminarUnClienteExistenteLoBorraDelRepositorio() {
        clienteService = new ClienteService(clienteRepository);
        given(clienteRepository.existsById(1L)).willReturn(true);

        clienteService.eliminar(1L);

        verify(clienteRepository).deleteById(1L);
    }

    @Test
    void alEliminarUnClienteInexistenteLanzaExcepcion() {
        clienteService = new ClienteService(clienteRepository);
        given(clienteRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> clienteService.eliminar(1L))
            .isInstanceOf(ClienteNoEncontradoException.class);
        verify(clienteRepository, never()).deleteById(any());
    }

}

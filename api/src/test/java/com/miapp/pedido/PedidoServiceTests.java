package com.miapp.pedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.miapp.cliente.ClienteService;
import com.miapp.cliente.excepciones.ClienteNoEncontradoException;
import com.miapp.pedido.dto.CrearPedido;
import com.miapp.pedido.dto.LineaPedidoInput;
import com.miapp.pedido.dto.PedidoResponse;
import com.miapp.pedido.excepciones.CantidadInvalidaException;
import com.miapp.pedido.excepciones.PedidoNoEncontradoException;
import com.miapp.producto.ProductoService;
import com.miapp.producto.dto.ProductoResponse;
import com.miapp.producto.excepciones.ProductoNoEncontradoException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class PedidoServiceTests {

    @Mock
    PedidoRepository pedidoRepository;

    @Mock
    ClienteService clienteService;

    @Mock
    ProductoService productoService;

    PedidoService pedidoService;

    @Test
    void alCrearConUnaLineaCalculaElTotalYGuardaElPedido() {
        pedidoService = new PedidoService(pedidoRepository, clienteService, productoService);
        given(clienteService.existePorId(1L)).willReturn(true);
        given(productoService.obtenerPorId(10L))
            .willReturn(new ProductoResponse(10L, "TRP-001", "Martillo", new BigDecimal("100.00")));
        given(pedidoRepository.save(any())).willAnswer(invocacion -> {
            Pedido guardado = invocacion.getArgument(0);
            ReflectionTestUtils.setField(guardado, "id", 1L);
            return guardado;
        });

        PedidoResponse respuesta = pedidoService.crear(
            new CrearPedido(1L, List.of(new LineaPedidoInput(10L, 3))));

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.clienteId()).isEqualTo(1L);
        assertThat(respuesta.fecha()).isEqualTo(LocalDate.now());
        assertThat(respuesta.lineas()).hasSize(1);
        assertThat(respuesta.total()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void alCrearConVariasLineasSumaElTotalDeCadaUna() {
        pedidoService = new PedidoService(pedidoRepository, clienteService, productoService);
        given(clienteService.existePorId(1L)).willReturn(true);
        given(productoService.obtenerPorId(10L))
            .willReturn(new ProductoResponse(10L, "TRP-001", "Martillo", new BigDecimal("100.00")));
        given(productoService.obtenerPorId(20L))
            .willReturn(new ProductoResponse(20L, "TRP-002", "Serrucho", new BigDecimal("50.00")));
        given(pedidoRepository.save(any())).willAnswer(invocacion -> {
            Pedido guardado = invocacion.getArgument(0);
            ReflectionTestUtils.setField(guardado, "id", 2L);
            return guardado;
        });

        PedidoResponse respuesta = pedidoService.crear(new CrearPedido(1L, List.of(
            new LineaPedidoInput(10L, 3),
            new LineaPedidoInput(20L, 4))));

        assertThat(respuesta.lineas()).hasSize(2);
        assertThat(respuesta.total()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void alCrearConClienteInexistenteLanzaExcepcionYNoGuardaNada() {
        pedidoService = new PedidoService(pedidoRepository, clienteService, productoService);
        given(clienteService.existePorId(99L)).willReturn(false);

        assertThatThrownBy(() -> pedidoService.crear(
            new CrearPedido(99L, List.of(new LineaPedidoInput(10L, 1)))))
            .isInstanceOf(ClienteNoEncontradoException.class);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void alCrearConProductoInexistenteLanzaExcepcionYNoGuardaNada() {
        pedidoService = new PedidoService(pedidoRepository, clienteService, productoService);
        given(clienteService.existePorId(1L)).willReturn(true);
        given(productoService.obtenerPorId(99L)).willThrow(new ProductoNoEncontradoException(99L));

        assertThatThrownBy(() -> pedidoService.crear(
            new CrearPedido(1L, List.of(new LineaPedidoInput(99L, 1)))))
            .isInstanceOf(ProductoNoEncontradoException.class);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void alCrearConCantidadCeroLanzaExcepcionYNoGuardaNada() {
        pedidoService = new PedidoService(pedidoRepository, clienteService, productoService);
        given(clienteService.existePorId(1L)).willReturn(true);

        assertThatThrownBy(() -> pedidoService.crear(
            new CrearPedido(1L, List.of(new LineaPedidoInput(10L, 0)))))
            .isInstanceOf(CantidadInvalidaException.class);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void alCrearConCantidadNegativaLanzaExcepcionYNoGuardaNada() {
        pedidoService = new PedidoService(pedidoRepository, clienteService, productoService);
        given(clienteService.existePorId(1L)).willReturn(true);

        assertThatThrownBy(() -> pedidoService.crear(
            new CrearPedido(1L, List.of(new LineaPedidoInput(10L, -1)))))
            .isInstanceOf(CantidadInvalidaException.class);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void alListarMapeaVariosPedidosEnElMismoOrden() {
        pedidoService = new PedidoService(pedidoRepository, clienteService, productoService);
        Pedido primero = new Pedido(1L, LocalDate.now());
        ReflectionTestUtils.setField(primero, "id", 1L);
        primero.agregarDetalle(new PedidoDetalle(10L, 2));
        Pedido segundo = new Pedido(2L, LocalDate.now());
        ReflectionTestUtils.setField(segundo, "id", 2L);
        segundo.agregarDetalle(new PedidoDetalle(10L, 1));
        given(pedidoRepository.findAll()).willReturn(List.of(primero, segundo));
        given(productoService.obtenerPorId(10L))
            .willReturn(new ProductoResponse(10L, "TRP-001", "Martillo", new BigDecimal("100.00")));

        List<PedidoResponse> respuesta = pedidoService.listar();

        assertThat(respuesta).extracting(PedidoResponse::id).containsExactly(1L, 2L);
    }

    @Test
    void alObtenerPorIdExistenteDevuelveElPedidoConSuTotal() {
        pedidoService = new PedidoService(pedidoRepository, clienteService, productoService);
        Pedido pedido = new Pedido(1L, LocalDate.now());
        ReflectionTestUtils.setField(pedido, "id", 5L);
        pedido.agregarDetalle(new PedidoDetalle(10L, 2));
        given(pedidoRepository.findById(5L)).willReturn(Optional.of(pedido));
        given(productoService.obtenerPorId(10L))
            .willReturn(new ProductoResponse(10L, "TRP-001", "Martillo", new BigDecimal("100.00")));

        Optional<PedidoResponse> respuesta = pedidoService.obtenerPorId(5L);

        assertThat(respuesta).isPresent();
        assertThat(respuesta.get().total()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void alObtenerPorIdInexistenteDevuelveVacio() {
        pedidoService = new PedidoService(pedidoRepository, clienteService, productoService);
        given(pedidoRepository.findById(99L)).willReturn(Optional.empty());

        Optional<PedidoResponse> respuesta = pedidoService.obtenerPorId(99L);

        assertThat(respuesta).isEmpty();
    }

    @Test
    void alEliminarUnPedidoExistenteLoBorraDelRepositorio() {
        pedidoService = new PedidoService(pedidoRepository, clienteService, productoService);
        given(pedidoRepository.existsById(1L)).willReturn(true);

        pedidoService.eliminar(1L);

        verify(pedidoRepository).deleteById(1L);
    }

    @Test
    void alEliminarUnPedidoInexistenteLanzaExcepcion() {
        pedidoService = new PedidoService(pedidoRepository, clienteService, productoService);
        given(pedidoRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> pedidoService.eliminar(1L))
            .isInstanceOf(PedidoNoEncontradoException.class);
        verify(pedidoRepository, never()).deleteById(any());
    }

}

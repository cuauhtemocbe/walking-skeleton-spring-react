package com.miapp.producto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.miapp.producto.dto.ActualizarProducto;
import com.miapp.producto.dto.CrearProducto;
import com.miapp.producto.dto.ProductoResponse;
import com.miapp.producto.excepciones.CodigoProductoDuplicadoException;
import com.miapp.producto.excepciones.ProductoNoEncontradoException;

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
class ProductoServiceTests {

    @Mock
    ProductoRepository productoRepository;

    ProductoService productoService;

    @Test
    void alCrearGuardaElProductoConLosDatosRecibidos() {
        productoService = new ProductoService(productoRepository);
        given(productoRepository.findByCodigo("TRP-001")).willReturn(Optional.empty());
        given(productoRepository.save(any())).willAnswer(invocacion -> invocacion.getArgument(0));

        ProductoResponse respuesta = productoService.crear(
            new CrearProducto("TRP-001", "Martillo Truper", new BigDecimal("199.90")));

        ArgumentCaptor<Producto> productoGuardado = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(productoGuardado.capture());
        assertThat(respuesta.nombre()).isEqualTo("Martillo Truper");
        assertThat(respuesta.codigo()).isEqualTo(productoGuardado.getValue().getCodigo());
        assertThat(respuesta.precioUnitario()).isEqualTo(new BigDecimal("199.90"));
    }

    @Test
    void alCrearConCodigoYaRegistradoLanzaExcepcion() {
        productoService = new ProductoService(productoRepository);
        Producto existente = new Producto("TRP-001", "Otro", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(existente, "id", 5L);
        given(productoRepository.findByCodigo("TRP-001")).willReturn(Optional.of(existente));

        assertThatThrownBy(() -> productoService.crear(
            new CrearProducto("TRP-001", "Nuevo", new BigDecimal("10.00"))))
            .isInstanceOf(CodigoProductoDuplicadoException.class);
        verify(productoRepository, never()).save(any());
    }

    @Test
    void alListarMapeaLasEntidadesGuardadasARespuestas() {
        productoService = new ProductoService(productoRepository);
        Producto producto = new Producto("TRP-001", "Martillo Truper", new BigDecimal("199.90"));
        given(productoRepository.findAll()).willReturn(List.of(producto));

        List<ProductoResponse> respuesta = productoService.listar();

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.get(0).nombre()).isEqualTo("Martillo Truper");
    }

    @Test
    void alListarMapeaVariosProductosEnElMismoOrden() {
        productoService = new ProductoService(productoRepository);
        Producto primero = new Producto("TRP-001", "Primero", new BigDecimal("10.00"));
        Producto segundo = new Producto("TRP-002", "Segundo", new BigDecimal("20.00"));
        Producto tercero = new Producto("TRP-003", "Tercero", new BigDecimal("30.00"));
        given(productoRepository.findAll()).willReturn(List.of(primero, segundo, tercero));

        List<ProductoResponse> respuesta = productoService.listar();

        assertThat(respuesta).extracting(ProductoResponse::nombre)
            .containsExactly("Primero", "Segundo", "Tercero");
    }

    @Test
    void alBuscarPorCodigoExistenteDevuelveElProducto() {
        productoService = new ProductoService(productoRepository);
        Producto producto = new Producto("TRP-001", "Martillo Truper", new BigDecimal("199.90"));
        given(productoRepository.findByCodigo("TRP-001")).willReturn(Optional.of(producto));

        Optional<ProductoResponse> respuesta = productoService.buscarPorCodigo("TRP-001");

        assertThat(respuesta).isPresent();
        assertThat(respuesta.get().codigo()).isEqualTo("TRP-001");
    }

    @Test
    void alBuscarPorCodigoInexistenteDevuelveVacio() {
        productoService = new ProductoService(productoRepository);
        given(productoRepository.findByCodigo("ZZZ-999")).willReturn(Optional.empty());

        Optional<ProductoResponse> respuesta = productoService.buscarPorCodigo("ZZZ-999");

        assertThat(respuesta).isEmpty();
    }

    @Test
    void alActualizarCambiaLosDatosDelProducto() {
        productoService = new ProductoService(productoRepository);
        Producto producto = new Producto("TRP-001", "Viejo nombre", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(producto, "id", 1L);
        given(productoRepository.findById(1L)).willReturn(Optional.of(producto));
        given(productoRepository.findByCodigo("TRP-001")).willReturn(Optional.of(producto));

        ProductoResponse respuesta = productoService.actualizar(1L,
            new ActualizarProducto("TRP-001", "Nuevo nombre", new BigDecimal("150.00")));

        assertThat(respuesta.nombre()).isEqualTo("Nuevo nombre");
        assertThat(respuesta.precioUnitario()).isEqualTo(new BigDecimal("150.00"));
    }

    @Test
    void alActualizarUnProductoInexistenteLanzaExcepcion() {
        productoService = new ProductoService(productoRepository);
        given(productoRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizar(1L,
            new ActualizarProducto("TRP-001", "Nombre", new BigDecimal("100.00"))))
            .isInstanceOf(ProductoNoEncontradoException.class);
    }

    @Test
    void alActualizarConCodigoDeOtroProductoLanzaExcepcion() {
        productoService = new ProductoService(productoRepository);
        Producto producto = new Producto("TRP-001", "Nombre", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(producto, "id", 1L);
        Producto otro = new Producto("TRP-002", "Otro", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(otro, "id", 2L);
        given(productoRepository.findById(1L)).willReturn(Optional.of(producto));
        given(productoRepository.findByCodigo("TRP-002")).willReturn(Optional.of(otro));

        assertThatThrownBy(() -> productoService.actualizar(1L,
            new ActualizarProducto("TRP-002", "Nombre", new BigDecimal("100.00"))))
            .isInstanceOf(CodigoProductoDuplicadoException.class);
    }

    @Test
    void alEliminarUnProductoExistenteLoBorraDelRepositorio() {
        productoService = new ProductoService(productoRepository);
        given(productoRepository.existsById(1L)).willReturn(true);

        productoService.eliminar(1L);

        verify(productoRepository).deleteById(1L);
    }

    @Test
    void alEliminarUnProductoInexistenteLanzaExcepcion() {
        productoService = new ProductoService(productoRepository);
        given(productoRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> productoService.eliminar(1L))
            .isInstanceOf(ProductoNoEncontradoException.class);
        verify(productoRepository, never()).deleteById(any());
    }

}

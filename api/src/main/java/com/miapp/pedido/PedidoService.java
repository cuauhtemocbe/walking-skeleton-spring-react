package com.miapp.pedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.miapp.cliente.ClienteService;
import com.miapp.cliente.excepciones.ClienteNoEncontradoException;
import com.miapp.pedido.dto.CrearPedido;
import com.miapp.pedido.dto.LineaPedidoInput;
import com.miapp.pedido.dto.PedidoResponse;
import com.miapp.pedido.dto.PedidoResponse.LineaPedidoResponse;
import com.miapp.pedido.excepciones.CantidadInvalidaException;
import com.miapp.pedido.excepciones.PedidoNoEncontradoException;
import com.miapp.producto.ProductoService;
import com.miapp.producto.dto.ProductoResponse;

import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final ProductoService productoService;

    PedidoService(PedidoRepository pedidoRepository, ClienteService clienteService, ProductoService productoService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteService = clienteService;
        this.productoService = productoService;
    }

    public PedidoResponse crear(CrearPedido crearPedido) {
        if (!clienteService.existePorId(crearPedido.clienteId())) {
            throw new ClienteNoEncontradoException(crearPedido.clienteId());
        }

        crearPedido.lineas().forEach(PedidoService::validarCantidad);

        Map<Long, ProductoResponse> productosPorId = crearPedido.lineas().stream()
            .map(LineaPedidoInput::productoId)
            .distinct()
            .collect(Collectors.toMap(id -> id, productoService::obtenerPorId));

        Pedido pedido = new Pedido(crearPedido.clienteId(), LocalDate.now());
        crearPedido.lineas().forEach(linea ->
            pedido.agregarDetalle(new PedidoDetalle(linea.productoId(), linea.cantidad())));

        Pedido guardado = pedidoRepository.save(pedido);
        return aRespuesta(guardado, productosPorId);
    }

    public List<PedidoResponse> listar() {
        return pedidoRepository.findAll().stream()
            .map(pedido -> aRespuesta(pedido, productosPorId(pedido)))
            .toList();
    }

    public Optional<PedidoResponse> obtenerPorId(Long id) {
        return pedidoRepository.findById(id)
            .map(pedido -> aRespuesta(pedido, productosPorId(pedido)));
    }

    public void eliminar(Long id) {
        if (!pedidoRepository.existsById(id)) {
            throw new PedidoNoEncontradoException(id);
        }
        pedidoRepository.deleteById(id);
    }

    private static void validarCantidad(LineaPedidoInput linea) {
        if (linea.cantidad() == null || linea.cantidad() <= 0) {
            throw new CantidadInvalidaException(linea.cantidad());
        }
    }

    private Map<Long, ProductoResponse> productosPorId(Pedido pedido) {
        return pedido.getDetalles().stream()
            .map(PedidoDetalle::getProductoId)
            .distinct()
            .collect(Collectors.toMap(id -> id, productoService::obtenerPorId));
    }

    private static PedidoResponse aRespuesta(Pedido pedido, Map<Long, ProductoResponse> productosPorId) {
        List<LineaPedidoResponse> lineas = pedido.getDetalles().stream()
            .map(detalle -> aLineaRespuesta(detalle, productosPorId.get(detalle.getProductoId())))
            .toList();
        BigDecimal total = lineas.stream()
            .map(LineaPedidoResponse::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PedidoResponse(pedido.getId(), pedido.getClienteId(), pedido.getFecha(), lineas, total);
    }

    private static LineaPedidoResponse aLineaRespuesta(PedidoDetalle detalle, ProductoResponse producto) {
        BigDecimal subtotal = producto.precioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()));
        return new LineaPedidoResponse(detalle.getProductoId(), detalle.getCantidad(), producto.precioUnitario(), subtotal);
    }

}

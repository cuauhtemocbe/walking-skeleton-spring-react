package com.miapp.producto;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.miapp.producto.dto.ActualizarProducto;
import com.miapp.producto.dto.CrearProducto;
import com.miapp.producto.dto.ProductoResponse;
import com.miapp.producto.excepciones.CodigoProductoDuplicadoException;
import com.miapp.producto.excepciones.ProductoNoEncontradoException;

import org.springframework.stereotype.Service;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public ProductoResponse crear(CrearProducto crearProducto) {
        validarCodigoDisponible(crearProducto.codigo(), null);
        Producto producto = new Producto(crearProducto.codigo(), crearProducto.nombre(), crearProducto.precioUnitario());
        Producto guardado = productoRepository.save(producto);
        return aRespuesta(guardado);
    }

    public List<ProductoResponse> listar() {
        return productoRepository.findAll().stream()
            .map(ProductoService::aRespuesta)
            .toList();
    }

    public Optional<ProductoResponse> buscarPorCodigo(String codigo) {
        return productoRepository.findByCodigo(codigo).map(ProductoService::aRespuesta);
    }

    public ProductoResponse actualizar(Long id, ActualizarProducto actualizarProducto) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ProductoNoEncontradoException(id));
        validarCodigoDisponible(actualizarProducto.codigo(), id);
        producto.actualizar(actualizarProducto.codigo(), actualizarProducto.nombre(), actualizarProducto.precioUnitario());
        return aRespuesta(producto);
    }

    public void eliminar(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new ProductoNoEncontradoException(id);
        }
        productoRepository.deleteById(id);
    }

    private void validarCodigoDisponible(String codigo, Long idAExcluir) {
        productoRepository.findByCodigo(codigo)
            .filter(existente -> !Objects.equals(existente.getId(), idAExcluir))
            .ifPresent(existente -> {
                throw new CodigoProductoDuplicadoException(codigo);
            });
    }

    private static ProductoResponse aRespuesta(Producto producto) {
        return new ProductoResponse(producto.getId(), producto.getCodigo(), producto.getNombre(), producto.getPrecioUnitario());
    }

}

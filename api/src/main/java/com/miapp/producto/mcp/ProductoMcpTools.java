package com.miapp.producto.mcp;

import java.math.BigDecimal;
import java.util.List;

import com.miapp.producto.ProductoService;
import com.miapp.producto.dto.ActualizarProducto;
import com.miapp.producto.dto.CrearProducto;
import com.miapp.producto.dto.ProductoResponse;
import com.miapp.producto.excepciones.CodigoProductoDuplicadoException;
import com.miapp.producto.excepciones.ProductoNoEncontradoException;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
class ProductoMcpTools {

    private final ProductoService productoService;

    ProductoMcpTools(ProductoService productoService) {
        this.productoService = productoService;
    }

    @McpTool(name = "crearProducto", description = "Da de alta un producto nuevo con su código único y precio")
    ProductoResponse crearProducto(
        @McpToolParam(description = "Código único del producto", required = true) String codigo,
        @McpToolParam(description = "Nombre del producto", required = true) String nombre,
        @McpToolParam(description = "Precio unitario del producto", required = true) BigDecimal precioUnitario) {
        try {
            return productoService.crear(new CrearProducto(codigo, nombre, precioUnitario));
        } catch (CodigoProductoDuplicadoException e) {
            throw errorParametroInvalido(e.getMessage());
        }
    }

    @McpTool(name = "listarProductos", description = "Lista todos los productos registrados")
    List<ProductoResponse> listarProductos() {
        return productoService.listar();
    }

    @McpTool(name = "buscarProducto", description = "Busca un producto por su código; devuelve null si no existe")
    ProductoResponse buscarProducto(
        @McpToolParam(description = "Código del producto a buscar", required = true) String codigo) {
        return productoService.buscarPorCodigo(codigo).orElse(null);
    }

    @McpTool(name = "actualizarProducto", description = "Actualiza los datos de un producto existente")
    ProductoResponse actualizarProducto(
        @McpToolParam(description = "Id del producto a actualizar", required = true) Long id,
        @McpToolParam(description = "Código único del producto", required = true) String codigo,
        @McpToolParam(description = "Nombre del producto", required = true) String nombre,
        @McpToolParam(description = "Precio unitario del producto", required = true) BigDecimal precioUnitario) {
        try {
            return productoService.actualizar(id, new ActualizarProducto(codigo, nombre, precioUnitario));
        } catch (ProductoNoEncontradoException e) {
            throw errorRecursoNoEncontrado(e.getMessage());
        } catch (CodigoProductoDuplicadoException e) {
            throw errorParametroInvalido(e.getMessage());
        }
    }

    @McpTool(name = "eliminarProducto", description = "Elimina un producto por su id")
    String eliminarProducto(
        @McpToolParam(description = "Id del producto a eliminar", required = true) Long id) {
        try {
            productoService.eliminar(id);
            return "Producto " + id + " eliminado";
        } catch (ProductoNoEncontradoException e) {
            throw errorRecursoNoEncontrado(e.getMessage());
        }
    }

    private static McpError errorParametroInvalido(String mensaje) {
        return McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message(mensaje).build();
    }

    private static McpError errorRecursoNoEncontrado(String mensaje) {
        return McpError.builder(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND).message(mensaje).build();
    }

}

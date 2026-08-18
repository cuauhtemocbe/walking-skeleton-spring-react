package com.miapp.pedido.mcp;

import java.util.List;

import com.miapp.cliente.excepciones.ClienteNoEncontradoException;
import com.miapp.pedido.PedidoService;
import com.miapp.pedido.dto.CrearPedido;
import com.miapp.pedido.dto.LineaPedidoInput;
import com.miapp.pedido.dto.PedidoResponse;
import com.miapp.pedido.excepciones.CantidadInvalidaException;
import com.miapp.pedido.excepciones.PedidoNoEncontradoException;
import com.miapp.producto.excepciones.ProductoNoEncontradoException;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
class PedidoMcpTools {

    private final PedidoService pedidoService;

    PedidoMcpTools(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @McpTool(name = "crearPedido", description = "Crea un pedido para un cliente existente con una o más líneas de producto y devuelve el total calculado")
    PedidoResponse crearPedido(
        @McpToolParam(description = "Id del cliente para el que se crea el pedido", required = true) Long clienteId,
        @McpToolParam(description = "Líneas del pedido: cada una con el id del producto y la cantidad", required = true) List<LineaPedidoInput> lineas) {
        try {
            return pedidoService.crear(new CrearPedido(clienteId, lineas));
        } catch (ClienteNoEncontradoException | ProductoNoEncontradoException e) {
            throw errorRecursoNoEncontrado(e.getMessage());
        } catch (CantidadInvalidaException e) {
            throw errorParametroInvalido(e.getMessage());
        }
    }

    @McpTool(name = "listarPedidos", description = "Lista todos los pedidos registrados, incluyendo su total calculado")
    List<PedidoResponse> listarPedidos() {
        return pedidoService.listar();
    }

    @McpTool(name = "obtenerPedido", description = "Obtiene un pedido por su id, incluyendo su total calculado; devuelve null si no existe")
    PedidoResponse obtenerPedido(
        @McpToolParam(description = "Id del pedido a obtener", required = true) Long id) {
        return pedidoService.obtenerPorId(id).orElse(null);
    }

    @McpTool(name = "eliminarPedido", description = "Elimina un pedido por su id")
    String eliminarPedido(
        @McpToolParam(description = "Id del pedido a eliminar", required = true) Long id) {
        try {
            pedidoService.eliminar(id);
            return "Pedido " + id + " eliminado";
        } catch (PedidoNoEncontradoException e) {
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

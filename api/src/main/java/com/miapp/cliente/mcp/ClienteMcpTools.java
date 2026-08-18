package com.miapp.cliente.mcp;

import java.util.List;

import com.miapp.cliente.ClienteService;
import com.miapp.cliente.dto.ActualizarCliente;
import com.miapp.cliente.dto.ClienteResponse;
import com.miapp.cliente.dto.CrearCliente;
import com.miapp.cliente.excepciones.ClienteNoEncontradoException;
import com.miapp.cliente.excepciones.RfcDuplicadoException;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
class ClienteMcpTools {

    private final ClienteService clienteService;

    ClienteMcpTools(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @McpTool(name = "crearCliente", description = "Da de alta un cliente nuevo con su RFC y razón social")
    ClienteResponse crearCliente(
        @McpToolParam(description = "Nombre del cliente", required = true) String nombre,
        @McpToolParam(description = "RFC único del cliente", required = true) String rfc,
        @McpToolParam(description = "Razón social del cliente", required = true) String razonSocial) {
        try {
            return clienteService.crear(new CrearCliente(nombre, rfc, razonSocial));
        } catch (RfcDuplicadoException e) {
            throw errorParametroInvalido(e.getMessage());
        }
    }

    @McpTool(name = "listarClientes", description = "Lista todos los clientes registrados")
    List<ClienteResponse> listarClientes() {
        return clienteService.listar();
    }

    @McpTool(name = "buscarCliente", description = "Busca un cliente por su RFC; devuelve null si no existe")
    ClienteResponse buscarCliente(
        @McpToolParam(description = "RFC del cliente a buscar", required = true) String rfc) {
        return clienteService.buscarPorRfc(rfc).orElse(null);
    }

    @McpTool(name = "actualizarCliente", description = "Actualiza los datos de un cliente existente")
    ClienteResponse actualizarCliente(
        @McpToolParam(description = "Id del cliente a actualizar", required = true) Long id,
        @McpToolParam(description = "Nombre del cliente", required = true) String nombre,
        @McpToolParam(description = "RFC único del cliente", required = true) String rfc,
        @McpToolParam(description = "Razón social del cliente", required = true) String razonSocial) {
        try {
            return clienteService.actualizar(id, new ActualizarCliente(nombre, rfc, razonSocial));
        } catch (ClienteNoEncontradoException e) {
            throw errorRecursoNoEncontrado(e.getMessage());
        } catch (RfcDuplicadoException e) {
            throw errorParametroInvalido(e.getMessage());
        }
    }

    @McpTool(name = "eliminarCliente", description = "Elimina un cliente por su id")
    String eliminarCliente(
        @McpToolParam(description = "Id del cliente a eliminar", required = true) Long id) {
        try {
            clienteService.eliminar(id);
            return "Cliente " + id + " eliminado";
        } catch (ClienteNoEncontradoException e) {
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

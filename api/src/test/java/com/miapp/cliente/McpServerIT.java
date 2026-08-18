package com.miapp.cliente;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class McpServerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17"))
        .withReuse(true);

    @LocalServerPort
    int puerto;

    private final ObjectMapper json = new ObjectMapper();

    McpSyncClient cliente;

    @BeforeEach
    void conectar() {
        var transporte = HttpClientStreamableHttpTransport.builder("http://localhost:" + puerto).build();
        cliente = McpClient.sync(transporte).build();
        cliente.initialize();
    }

    @AfterEach
    void cerrar() {
        cliente.closeGracefully();
    }

    @Test
    void listaExactamenteLasCincoToolsDeCliente() {
        ListToolsResult tools = cliente.listTools();

        assertThat(tools.tools()).hasSize(5);
        assertThat(tools.tools()).extracting(Tool::name).containsExactlyInAnyOrder(
            "crearCliente", "listarClientes", "buscarCliente", "actualizarCliente", "eliminarCliente");
    }

    @Test
    void flujoFelizCompletoCrearListarBuscarActualizarEliminar() throws Exception {
        CallToolResult creado = cliente.callTool(new CallToolRequest("crearCliente", Map.of(
            "nombre", "Ferretería Truper",
            "rfc", "AAA010101AAA",
            "razonSocial", "Truper S.A. de C.V.")));
        assertThat(creado.isError()).isNotEqualTo(Boolean.TRUE);
        JsonNode clienteCreado = textoComoJson(creado);
        long id = clienteCreado.path("id").asLong();
        assertThat(clienteCreado.path("nombre").asText()).isEqualTo("Ferretería Truper");

        CallToolResult listado = cliente.callTool(new CallToolRequest("listarClientes", Map.of()));
        JsonNode listaClientes = textoComoJson(listado);
        assertThat(listaClientes).anyMatch(nodo -> nodo.path("rfc").asText().equals("AAA010101AAA"));

        CallToolResult buscado = cliente.callTool(new CallToolRequest("buscarCliente", Map.of("rfc", "AAA010101AAA")));
        JsonNode clienteBuscado = textoComoJson(buscado);
        assertThat(clienteBuscado.path("id").asLong()).isEqualTo(id);

        CallToolResult actualizado = cliente.callTool(new CallToolRequest("actualizarCliente", Map.of(
            "id", id,
            "nombre", "Nuevo nombre",
            "rfc", "AAA010101AAA",
            "razonSocial", "Nueva razón social")));
        assertThat(actualizado.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(textoComoJson(actualizado).path("nombre").asText()).isEqualTo("Nuevo nombre");

        CallToolResult eliminado = cliente.callTool(new CallToolRequest("eliminarCliente", Map.of("id", id)));
        assertThat(eliminado.isError()).isNotEqualTo(Boolean.TRUE);

        CallToolResult buscadoTrasEliminar = cliente.callTool(new CallToolRequest("buscarCliente", Map.of("rfc", "AAA010101AAA")));
        assertThat(buscadoTrasEliminar.content()).extracting(contenido -> ((TextContent) contenido).text())
            .allMatch(texto -> texto.equals("null") || texto.isBlank());
    }

    @Test
    void crearClienteConRfcDuplicadoDevuelveErrorLegible() {
        cliente.callTool(new CallToolRequest("crearCliente", Map.of(
            "nombre", "Ferretería Truper",
            "rfc", "BBB020202BBB",
            "razonSocial", "Truper S.A. de C.V.")));

        CallToolResult resultado = cliente.callTool(new CallToolRequest("crearCliente", Map.of(
            "nombre", "Otro cliente",
            "rfc", "BBB020202BBB",
            "razonSocial", "Otro S.A.")));

        assertThat(resultado.isError()).isTrue();
        assertThat(resultado.content()).extracting(contenido -> ((TextContent) contenido).text())
            .anyMatch(texto -> texto.contains("Ya existe un cliente con RFC"));
    }

    @Test
    void actualizarClienteInexistenteDevuelveErrorLegible() {
        CallToolResult resultado = cliente.callTool(new CallToolRequest("actualizarCliente", Map.of(
            "id", 999999L,
            "nombre", "Nombre",
            "rfc", "CCC030303CCC",
            "razonSocial", "Razón social")));

        assertThat(resultado.isError()).isTrue();
        assertThat(resultado.content()).extracting(contenido -> ((TextContent) contenido).text())
            .anyMatch(texto -> texto.contains("No existe un cliente con id"));
    }

    @Test
    void actualizarClienteConRfcDeOtroClienteDevuelveErrorLegible() throws Exception {
        CallToolResult primero = cliente.callTool(new CallToolRequest("crearCliente", Map.of(
            "nombre", "Primero", "rfc", "DDD040404DDD", "razonSocial", "Primero S.A.")));
        long idPrimero = textoComoJson(primero).path("id").asLong();
        cliente.callTool(new CallToolRequest("crearCliente", Map.of(
            "nombre", "Segundo", "rfc", "EEE050505EEE", "razonSocial", "Segundo S.A.")));

        CallToolResult resultado = cliente.callTool(new CallToolRequest("actualizarCliente", Map.of(
            "id", idPrimero, "nombre", "Primero", "rfc", "EEE050505EEE", "razonSocial", "Primero S.A.")));

        assertThat(resultado.isError()).isTrue();
        assertThat(resultado.content()).extracting(contenido -> ((TextContent) contenido).text())
            .anyMatch(texto -> texto.contains("Ya existe un cliente con RFC"));
    }

    @Test
    void eliminarClienteInexistenteDevuelveErrorLegible() {
        CallToolResult resultado = cliente.callTool(new CallToolRequest("eliminarCliente", Map.of("id", 999999L)));

        assertThat(resultado.isError()).isTrue();
        assertThat(resultado.content()).extracting(contenido -> ((TextContent) contenido).text())
            .anyMatch(texto -> texto.contains("No existe un cliente con id"));
    }

    private JsonNode textoComoJson(CallToolResult resultado) throws Exception {
        String texto = ((TextContent) resultado.content().get(0)).text();
        return json.readTree(texto);
    }

}

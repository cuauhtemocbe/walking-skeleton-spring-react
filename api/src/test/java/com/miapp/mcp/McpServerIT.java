package com.miapp.mcp;

import java.math.BigDecimal;
import java.util.List;
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
    void listaExactamenteLasCatorceToolsDeClienteProductoYPedido() {
        ListToolsResult tools = cliente.listTools();

        assertThat(tools.tools()).hasSize(14);
        assertThat(tools.tools()).extracting(Tool::name).containsExactlyInAnyOrder(
            "crearCliente", "listarClientes", "buscarCliente", "actualizarCliente", "eliminarCliente",
            "crearProducto", "listarProductos", "buscarProducto", "actualizarProducto", "eliminarProducto",
            "crearPedido", "listarPedidos", "obtenerPedido", "eliminarPedido");
    }

    @Test
    void flujoFelizCompletoCrearListarBuscarActualizarEliminarCliente() throws Exception {
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

    @Test
    void flujoFelizCompletoCrearListarBuscarActualizarEliminarProducto() throws Exception {
        CallToolResult creado = cliente.callTool(new CallToolRequest("crearProducto", Map.of(
            "codigo", "TRP-100",
            "nombre", "Martillo Truper",
            "precioUnitario", 199.90)));
        assertThat(creado.isError()).isNotEqualTo(Boolean.TRUE);
        JsonNode productoCreado = textoComoJson(creado);
        long id = productoCreado.path("id").asLong();
        assertThat(productoCreado.path("nombre").asText()).isEqualTo("Martillo Truper");

        CallToolResult listado = cliente.callTool(new CallToolRequest("listarProductos", Map.of()));
        JsonNode listaProductos = textoComoJson(listado);
        assertThat(listaProductos).anyMatch(nodo -> nodo.path("codigo").asText().equals("TRP-100"));

        CallToolResult buscado = cliente.callTool(new CallToolRequest("buscarProducto", Map.of("codigo", "TRP-100")));
        JsonNode productoBuscado = textoComoJson(buscado);
        assertThat(productoBuscado.path("id").asLong()).isEqualTo(id);

        CallToolResult actualizado = cliente.callTool(new CallToolRequest("actualizarProducto", Map.of(
            "id", id,
            "codigo", "TRP-100",
            "nombre", "Martillo Truper XL",
            "precioUnitario", 249.90)));
        assertThat(actualizado.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(textoComoJson(actualizado).path("nombre").asText()).isEqualTo("Martillo Truper XL");

        CallToolResult eliminado = cliente.callTool(new CallToolRequest("eliminarProducto", Map.of("id", id)));
        assertThat(eliminado.isError()).isNotEqualTo(Boolean.TRUE);

        CallToolResult buscadoTrasEliminar = cliente.callTool(new CallToolRequest("buscarProducto", Map.of("codigo", "TRP-100")));
        assertThat(buscadoTrasEliminar.content()).extracting(contenido -> ((TextContent) contenido).text())
            .allMatch(texto -> texto.equals("null") || texto.isBlank());
    }

    @Test
    void crearProductoConCodigoDuplicadoDevuelveErrorLegible() {
        cliente.callTool(new CallToolRequest("crearProducto", Map.of(
            "codigo", "TRP-200",
            "nombre", "Desarmador Truper",
            "precioUnitario", 89.90)));

        CallToolResult resultado = cliente.callTool(new CallToolRequest("crearProducto", Map.of(
            "codigo", "TRP-200",
            "nombre", "Otro producto",
            "precioUnitario", 10.00)));

        assertThat(resultado.isError()).isTrue();
        assertThat(resultado.content()).extracting(contenido -> ((TextContent) contenido).text())
            .anyMatch(texto -> texto.contains("Ya existe un producto con código"));
    }

    @Test
    void actualizarProductoInexistenteDevuelveErrorLegible() {
        CallToolResult resultado = cliente.callTool(new CallToolRequest("actualizarProducto", Map.of(
            "id", 999999L,
            "codigo", "TRP-300",
            "nombre", "Producto",
            "precioUnitario", 10.00)));

        assertThat(resultado.isError()).isTrue();
        assertThat(resultado.content()).extracting(contenido -> ((TextContent) contenido).text())
            .anyMatch(texto -> texto.contains("No existe un producto con id"));
    }

    @Test
    void eliminarProductoInexistenteDevuelveErrorLegible() {
        CallToolResult resultado = cliente.callTool(new CallToolRequest("eliminarProducto", Map.of("id", 999999L)));

        assertThat(resultado.isError()).isTrue();
        assertThat(resultado.content()).extracting(contenido -> ((TextContent) contenido).text())
            .anyMatch(texto -> texto.contains("No existe un producto con id"));
    }

    @Test
    void flujoFelizCompletoCrearListarObtenerYEliminarPedido() throws Exception {
        CallToolResult clienteCreado = cliente.callTool(new CallToolRequest("crearCliente", Map.of(
            "nombre", "Ferretería del Pedido",
            "rfc", "FFF060606FFF",
            "razonSocial", "Del Pedido S.A. de C.V.")));
        long idCliente = textoComoJson(clienteCreado).path("id").asLong();

        CallToolResult martilloCreado = cliente.callTool(new CallToolRequest("crearProducto", Map.of(
            "codigo", "TRP-PED-1",
            "nombre", "Martillo del Pedido",
            "precioUnitario", 100.00)));
        long idMartillo = textoComoJson(martilloCreado).path("id").asLong();

        CallToolResult serruchoCreado = cliente.callTool(new CallToolRequest("crearProducto", Map.of(
            "codigo", "TRP-PED-2",
            "nombre", "Serrucho del Pedido",
            "precioUnitario", 50.00)));
        long idSerrucho = textoComoJson(serruchoCreado).path("id").asLong();

        CallToolResult pedidoCreado = cliente.callTool(new CallToolRequest("crearPedido", Map.of(
            "clienteId", idCliente,
            "lineas", List.of(
                Map.of("productoId", idMartillo, "cantidad", 3),
                Map.of("productoId", idSerrucho, "cantidad", 4)))));
        assertThat(pedidoCreado.isError()).isNotEqualTo(Boolean.TRUE);
        JsonNode pedidoJson = textoComoJson(pedidoCreado);
        long idPedido = pedidoJson.path("id").asLong();
        assertThat(pedidoJson.path("clienteId").asLong()).isEqualTo(idCliente);
        assertThat(pedidoJson.path("lineas")).hasSize(2);
        assertThat(new BigDecimal(pedidoJson.path("total").asText()))
            .isEqualByComparingTo(new BigDecimal("500.00"));

        CallToolResult listado = cliente.callTool(new CallToolRequest("listarPedidos", Map.of()));
        JsonNode listaPedidos = textoComoJson(listado);
        assertThat(listaPedidos).anyMatch(nodo -> nodo.path("id").asLong() == idPedido);

        CallToolResult obtenido = cliente.callTool(new CallToolRequest("obtenerPedido", Map.of("id", idPedido)));
        assertThat(textoComoJson(obtenido).path("id").asLong()).isEqualTo(idPedido);

        CallToolResult eliminado = cliente.callTool(new CallToolRequest("eliminarPedido", Map.of("id", idPedido)));
        assertThat(eliminado.isError()).isNotEqualTo(Boolean.TRUE);

        CallToolResult obtenidoTrasEliminar = cliente.callTool(new CallToolRequest("obtenerPedido", Map.of("id", idPedido)));
        assertThat(obtenidoTrasEliminar.content()).extracting(contenido -> ((TextContent) contenido).text())
            .allMatch(texto -> texto.equals("null") || texto.isBlank());
    }

    @Test
    void crearPedidoConClienteInexistenteDevuelveErrorLegible() throws Exception {
        CallToolResult productoCreado = cliente.callTool(new CallToolRequest("crearProducto", Map.of(
            "codigo", "TRP-PED-3",
            "nombre", "Producto para pedido inválido",
            "precioUnitario", 10.00)));
        long idProducto = textoComoJson(productoCreado).path("id").asLong();

        CallToolResult resultado = cliente.callTool(new CallToolRequest("crearPedido", Map.of(
            "clienteId", 999999L,
            "lineas", List.of(Map.of("productoId", idProducto, "cantidad", 1)))));

        assertThat(resultado.isError()).isTrue();
        assertThat(resultado.content()).extracting(contenido -> ((TextContent) contenido).text())
            .anyMatch(texto -> texto.contains("No existe un cliente con id"));
    }

    @Test
    void crearPedidoConProductoInexistenteDevuelveErrorLegible() throws Exception {
        CallToolResult clienteCreado = cliente.callTool(new CallToolRequest("crearCliente", Map.of(
            "nombre", "Cliente para pedido inválido",
            "rfc", "GGG070707GGG",
            "razonSocial", "Inválido S.A.")));
        long idCliente = textoComoJson(clienteCreado).path("id").asLong();

        CallToolResult resultado = cliente.callTool(new CallToolRequest("crearPedido", Map.of(
            "clienteId", idCliente,
            "lineas", List.of(Map.of("productoId", 999999L, "cantidad", 1)))));

        assertThat(resultado.isError()).isTrue();
        assertThat(resultado.content()).extracting(contenido -> ((TextContent) contenido).text())
            .anyMatch(texto -> texto.contains("No existe un producto con id"));
    }

    @Test
    void crearPedidoConCantidadInvalidaDevuelveErrorLegible() throws Exception {
        CallToolResult clienteCreado = cliente.callTool(new CallToolRequest("crearCliente", Map.of(
            "nombre", "Cliente para cantidad inválida",
            "rfc", "HHH080808HHH",
            "razonSocial", "Cantidad Inválida S.A.")));
        long idCliente = textoComoJson(clienteCreado).path("id").asLong();
        CallToolResult productoCreado = cliente.callTool(new CallToolRequest("crearProducto", Map.of(
            "codigo", "TRP-PED-4",
            "nombre", "Producto para cantidad inválida",
            "precioUnitario", 10.00)));
        long idProducto = textoComoJson(productoCreado).path("id").asLong();

        CallToolResult resultado = cliente.callTool(new CallToolRequest("crearPedido", Map.of(
            "clienteId", idCliente,
            "lineas", List.of(Map.of("productoId", idProducto, "cantidad", 0)))));

        assertThat(resultado.isError()).isTrue();
        assertThat(resultado.content()).extracting(contenido -> ((TextContent) contenido).text())
            .anyMatch(texto -> texto.contains("La cantidad debe ser mayor a cero"));
    }

    @Test
    void eliminarPedidoInexistenteDevuelveErrorLegible() {
        CallToolResult resultado = cliente.callTool(new CallToolRequest("eliminarPedido", Map.of("id", 999999L)));

        assertThat(resultado.isError()).isTrue();
        assertThat(resultado.content()).extracting(contenido -> ((TextContent) contenido).text())
            .anyMatch(texto -> texto.contains("No existe un pedido con id"));
    }

    private JsonNode textoComoJson(CallToolResult resultado) throws Exception {
        String texto = ((TextContent) resultado.content().get(0)).text();
        return json.readTree(texto);
    }

}

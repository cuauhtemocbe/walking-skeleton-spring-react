package com.miapp.cliente;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.miapp.cliente.dto.ActualizarCliente;
import com.miapp.cliente.dto.ClienteResponse;
import com.miapp.cliente.dto.CrearCliente;
import com.miapp.cliente.excepciones.ClienteNoEncontradoException;
import com.miapp.cliente.excepciones.RfcDuplicadoException;

import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public ClienteResponse crear(CrearCliente crearCliente) {
        validarRfcDisponible(crearCliente.rfc(), null);
        Cliente cliente = new Cliente(crearCliente.nombre(), crearCliente.rfc(), crearCliente.razonSocial());
        Cliente guardado = clienteRepository.save(cliente);
        return aRespuesta(guardado);
    }

    public List<ClienteResponse> listar() {
        return clienteRepository.findAll().stream()
            .map(ClienteService::aRespuesta)
            .toList();
    }

    public Optional<ClienteResponse> buscarPorRfc(String rfc) {
        return clienteRepository.findByRfc(rfc).map(ClienteService::aRespuesta);
    }

    public ClienteResponse actualizar(Long id, ActualizarCliente actualizarCliente) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ClienteNoEncontradoException(id));
        validarRfcDisponible(actualizarCliente.rfc(), id);
        cliente.actualizar(actualizarCliente.nombre(), actualizarCliente.rfc(), actualizarCliente.razonSocial());
        return aRespuesta(cliente);
    }

    public void eliminar(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new ClienteNoEncontradoException(id);
        }
        clienteRepository.deleteById(id);
    }

    public boolean existePorId(Long id) {
        return clienteRepository.existsById(id);
    }

    private void validarRfcDisponible(String rfc, Long idAExcluir) {
        clienteRepository.findByRfc(rfc)
            .filter(existente -> !Objects.equals(existente.getId(), idAExcluir))
            .ifPresent(existente -> {
                throw new RfcDuplicadoException(rfc);
            });
    }

    private static ClienteResponse aRespuesta(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getRfc(), cliente.getRazonSocial());
    }

}

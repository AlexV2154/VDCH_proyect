package com.vdch.cliente.service;

import com.vdch.cliente.dto.ClienteResumen;
import com.vdch.cliente.model.Cliente;
import com.vdch.cliente.repository.ClienteRepository;
import java.util.List;

public class ClienteService {
    private final ClienteRepository clienteRepository;

    public ClienteService() {
        this(new ClienteRepository());
    }

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Long guardarCliente(Cliente cliente) {
        return clienteRepository.guardar(cliente);
    }

    public List<ClienteResumen> buscarClientes(String texto) {
        return clienteRepository.buscar(texto);
    }

    public void cambiarEstadoCliente(Long idCliente, boolean estado) {
        clienteRepository.cambiarEstado(idCliente, estado);
    }
}

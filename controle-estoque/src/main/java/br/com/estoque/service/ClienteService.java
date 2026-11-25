package br.com.estoque.service;

import br.com.estoque.model.Cliente;
import br.com.estoque.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public List<Cliente> listar() {
        return repository.findAll();
    }

    public Cliente buscar(Long id) {
        if (id == null) throw new RegraNegocioException("Informe o cliente.");
        return repository.findById(id).orElseThrow();
    }

    public Cliente salvar(Cliente cliente) {
        cliente.setId(null);
        return repository.save(cliente);
    }

    public Cliente atualizar(Long id, Cliente dados) {
        buscar(id);
        dados.setId(id);
        return repository.save(dados);
    }

    public void excluir(Long id) {
        repository.delete(buscar(id));
    }
}

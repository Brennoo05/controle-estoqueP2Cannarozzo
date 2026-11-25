package br.com.estoque.service;

import br.com.estoque.model.Fornecedor;
import br.com.estoque.repository.FornecedorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FornecedorService {

    private final FornecedorRepository repository;

    public FornecedorService(FornecedorRepository repository) {
        this.repository = repository;
    }

    public List<Fornecedor> listar() {
        return repository.findAll();
    }

    public Fornecedor buscar(Long id) {
        if (id == null) throw new RegraNegocioException("Informe o fornecedor.");
        return repository.findById(id).orElseThrow();
    }

    public Fornecedor salvar(Fornecedor fornecedor) {
        fornecedor.setId(null);
        return repository.save(fornecedor);
    }

    public Fornecedor atualizar(Long id, Fornecedor dados) {
        buscar(id); // garante que existe (senao 404)
        dados.setId(id);
        return repository.save(dados);
    }

    public void excluir(Long id) {
        repository.delete(buscar(id));
    }
}

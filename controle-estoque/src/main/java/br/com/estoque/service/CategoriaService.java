package br.com.estoque.service;

import br.com.estoque.model.Categoria;
import br.com.estoque.repository.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    public List<Categoria> listar() {
        return repository.findAll();
    }

    public Categoria buscar(Long id) {
        // orElseThrow lanca NoSuchElementException, que vira HTTP 404
        return repository.findById(id).orElseThrow();
    }

    public Categoria salvar(Categoria categoria) {
        categoria.setId(null);
        return repository.save(categoria);
    }

    public Categoria atualizar(Long id, Categoria dados) {
        Categoria categoria = buscar(id);
        categoria.setNome(dados.getNome());
        return repository.save(categoria);
    }

    public void excluir(Long id) {
        repository.delete(buscar(id));
    }
}

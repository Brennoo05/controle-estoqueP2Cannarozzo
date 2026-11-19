package br.com.estoque.service;

import br.com.estoque.model.Entrada;
import br.com.estoque.model.Produto;
import br.com.estoque.repository.EntradaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class EntradaService {

    private final EntradaRepository repository;
    private final ProdutoService produtoService;

    public EntradaService(EntradaRepository repository, ProdutoService produtoService) {
        this.repository = repository;
        this.produtoService = produtoService;
    }

    public List<Entrada> listar() {
        return repository.findAll();
    }

    public Entrada buscar(Long id) {
        return repository.findById(id).orElseThrow();
    }

    // @Transactional: se algo falhar, nada e gravado (nem a entrada, nem o estoque)
    @Transactional
    public Entrada registrar(Entrada entrada) {
        Produto produto = produtoService.buscar(entrada.getProduto().getId());
        produtoService.adicionarEstoque(produto, entrada.getQuantidade()); // estoque SOBE

        entrada.setId(null);
        entrada.setProduto(produto);
        if (entrada.getData() == null) entrada.setData(LocalDate.now());
        return repository.save(entrada);
    }

    @Transactional
    public Entrada atualizar(Long id, Entrada dados) {
        Entrada entrada = buscar(id);
        // desfaz a entrada antiga e aplica a nova
        produtoService.removerEstoque(entrada.getProduto(), entrada.getQuantidade());
        Produto novoProduto = produtoService.buscar(dados.getProduto().getId());
        produtoService.adicionarEstoque(novoProduto, dados.getQuantidade());

        entrada.setProduto(novoProduto);
        entrada.setQuantidade(dados.getQuantidade());
        entrada.setData(dados.getData() != null ? dados.getData() : entrada.getData());
        return repository.save(entrada);
    }

    @Transactional
    public void excluir(Long id) {
        Entrada entrada = buscar(id);
        // excluir uma entrada retira do estoque o que ela tinha colocado
        produtoService.removerEstoque(entrada.getProduto(), entrada.getQuantidade());
        repository.delete(entrada);
    }
}

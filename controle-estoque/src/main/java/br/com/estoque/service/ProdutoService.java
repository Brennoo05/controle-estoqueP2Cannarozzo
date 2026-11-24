package br.com.estoque.service;

import br.com.estoque.model.Produto;
import br.com.estoque.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;
    private final CategoriaService categoriaService;
    private final FornecedorService fornecedorService;

    public ProdutoService(ProdutoRepository repository,
                          CategoriaService categoriaService,
                          FornecedorService fornecedorService) {
        this.repository = repository;
        this.categoriaService = categoriaService;
        this.fornecedorService = fornecedorService;
    }

    public List<Produto> listar() {
        return repository.findAll();
    }

    public Produto buscar(Long id) {
        if (id == null) throw new RegraNegocioException("Informe o produto.");
        return repository.findById(id).orElseThrow();
    }

    public Produto salvar(Produto produto) {
        produto.setId(null);
        produto.setQuantidadeEstoque(0); // estoque so muda por Entrada/Saida
        carregarRelacionamentos(produto);
        return repository.save(produto);
    }

    public Produto atualizar(Long id, Produto dados) {
        Produto produto = buscar(id);
        produto.setNome(dados.getNome());
        produto.setPreco(dados.getPreco());
        produto.setCategoria(dados.getCategoria());
        produto.setFornecedor(dados.getFornecedor());
        carregarRelacionamentos(produto);
        return repository.save(produto); // quantidadeEstoque nao e alterada aqui
    }

    public void excluir(Long id) {
        repository.delete(buscar(id));
    }

    // ===== REGRA DE NEGOCIO DO ESTOQUE =====

    public void adicionarEstoque(Produto produto, int quantidade) {
        produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() + quantidade);
        repository.save(produto);
    }

    public void removerEstoque(Produto produto, int quantidade) {
        if (quantidade > produto.getQuantidadeEstoque()) {
            throw new RegraNegocioException("Estoque insuficiente para " + produto.getNome()
                    + ". Disponivel: " + produto.getQuantidadeEstoque());
        }
        produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - quantidade);
        repository.save(produto);
    }

    // O JSON traz so o id da categoria/fornecedor; aqui buscamos o objeto completo no banco
    private void carregarRelacionamentos(Produto produto) {
        produto.setCategoria(categoriaService.buscar(produto.getCategoria().getId()));
        produto.setFornecedor(fornecedorService.buscar(produto.getFornecedor().getId()));
    }
}

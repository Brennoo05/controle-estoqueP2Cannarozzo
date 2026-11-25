package br.com.estoque.service;

import br.com.estoque.model.Produto;
import br.com.estoque.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // Lista so os produtos disponiveis (os excluidos logicamente ficam de fora)
    public List<Produto> listar() {
        return repository.findByAtivoTrue();
    }

    // Busca qualquer produto, ativo ou excluido (o historico ainda aponta para ele)
    public Produto buscar(Long id) {
        if (id == null) throw new RegraNegocioException("Informe o produto.");
        return repository.findById(id).orElseThrow();
    }

    // Busca um produto que ainda pode ser editado ou receber novas movimentacoes
    public Produto buscarAtivo(Long id) {
        Produto produto = buscar(id);
        exigirAtivo(produto);
        return produto;
    }

    public void exigirAtivo(Produto produto) {
        if (!produto.isAtivo()) {
            throw new RegraNegocioException("O produto " + produto.getNome()
                    + " foi excluído e não pode ser alterado nem receber movimentações.");
        }
    }

    public Produto salvar(Produto produto) {
        produto.setId(null);
        produto.setQuantidadeEstoque(0); // estoque so muda por Entrada/Saida
        produto.setAtivo(true);          // todo produto novo nasce ativo
        carregarRelacionamentos(produto);
        return repository.save(produto);
    }

    public Produto atualizar(Long id, Produto dados) {
        Produto produto = buscarAtivo(id);
        produto.setNome(dados.getNome());
        produto.setPreco(dados.getPreco());
        produto.setCategoria(dados.getCategoria());
        produto.setFornecedor(dados.getFornecedor());
        carregarRelacionamentos(produto);
        return repository.save(produto); // quantidadeEstoque e ativo nao sao alterados aqui
    }

    // Exclusao LOGICA: o produto so deixa de estar disponivel. Nao apaga a linha do banco,
    // nao mexe no estoque e nao toca nas Entradas/Saidas, entao o historico fica intacto.
    @Transactional
    public void excluir(Long id) {
        Produto produto = buscarAtivo(id);
        produto.setAtivo(false);
        repository.save(produto);
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

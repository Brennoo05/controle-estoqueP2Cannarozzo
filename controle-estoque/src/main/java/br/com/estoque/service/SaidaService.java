package br.com.estoque.service;

import br.com.estoque.model.Produto;
import br.com.estoque.model.Saida;
import br.com.estoque.repository.SaidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SaidaService {

    private final SaidaRepository repository;
    private final ProdutoService produtoService;
    private final ClienteService clienteService;

    public SaidaService(SaidaRepository repository, ProdutoService produtoService, ClienteService clienteService) {
        this.repository = repository;
        this.produtoService = produtoService;
        this.clienteService = clienteService;
    }

    public List<Saida> listar() {
        return repository.findAll();
    }

    public Saida buscar(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public Saida registrar(Saida saida) {
        Produto produto = produtoService.buscar(saida.getProduto().getId());
        produtoService.removerEstoque(produto, saida.getQuantidade()); // estoque DESCE (valida saldo)

        saida.setId(null);
        saida.setProduto(produto);
        saida.setCliente(clienteService.buscar(saida.getCliente().getId()));
        if (saida.getData() == null) saida.setData(LocalDate.now());
        return repository.save(saida);
    }

    @Transactional
    public Saida atualizar(Long id, Saida dados) {
        Saida saida = buscar(id);
        // devolve a quantidade antiga e retira a nova
        produtoService.adicionarEstoque(saida.getProduto(), saida.getQuantidade());
        Produto novoProduto = produtoService.buscar(dados.getProduto().getId());
        produtoService.removerEstoque(novoProduto, dados.getQuantidade());

        saida.setProduto(novoProduto);
        saida.setCliente(clienteService.buscar(dados.getCliente().getId()));
        saida.setQuantidade(dados.getQuantidade());
        saida.setData(dados.getData() != null ? dados.getData() : saida.getData());
        return repository.save(saida);
    }

    @Transactional
    public void excluir(Long id) {
        Saida saida = buscar(id);
        // cancelar uma venda devolve os itens ao estoque
        produtoService.adicionarEstoque(saida.getProduto(), saida.getQuantidade());
        repository.delete(saida);
    }
}

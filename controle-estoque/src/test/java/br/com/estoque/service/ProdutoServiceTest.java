package br.com.estoque.service;

import br.com.estoque.model.Produto;
import br.com.estoque.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// TESTE UNITARIO: testa so a regra de estoque, sem banco (repositorios sao "mocks")
@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository repository;
    @Mock
    private CategoriaService categoriaService;
    @Mock
    private FornecedorService fornecedorService;

    @InjectMocks
    private ProdutoService service;

    private Produto produto;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setNome("Agua");
        produto.setQuantidadeEstoque(10);
    }

    @Test
    void adicionarEstoqueDeveSomarQuantidade() {
        service.adicionarEstoque(produto, 5);

        assertEquals(15, produto.getQuantidadeEstoque());
        verify(repository).save(produto);
    }

    @Test
    void removerEstoqueDeveSubtrairQuantidade() {
        service.removerEstoque(produto, 4);

        assertEquals(6, produto.getQuantidadeEstoque());
        verify(repository).save(produto);
    }

    @Test
    void naoDevePermitirVenderMaisDoQueOEstoque() {
        assertThrows(RegraNegocioException.class, () -> service.removerEstoque(produto, 11));

        assertEquals(10, produto.getQuantidadeEstoque()); // estoque nao mudou
        verify(repository, never()).save(any());
    }

    // ===== Exclusao logica =====

    @Test
    void excluirApenasMarcaOProdutoComoInativoSemApagarNemMexerNoEstoque() {
        when(repository.findById(1L)).thenReturn(Optional.of(produto));

        service.excluir(1L);

        assertFalse(produto.isAtivo());
        assertEquals(10, produto.getQuantidadeEstoque()); // saldo preservado
        verify(repository).save(produto);
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void produtoInativoNaoPodeReceberMovimentacao() {
        produto.setAtivo(false);
        when(repository.findById(1L)).thenReturn(Optional.of(produto));

        assertThrows(RegraNegocioException.class, () -> service.buscarAtivo(1L));
    }

    @Test
    void listarTrazSomenteProdutosAtivos() {
        service.listar();

        verify(repository).findByAtivoTrue();
        verify(repository, never()).findAll();
    }
}

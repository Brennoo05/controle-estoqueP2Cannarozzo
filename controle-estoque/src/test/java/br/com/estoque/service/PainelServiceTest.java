package br.com.estoque.service;

import br.com.estoque.dto.EstoquePorCategoria;
import br.com.estoque.dto.Painel;
import br.com.estoque.dto.ProdutoVendido;
import br.com.estoque.model.*;
import br.com.estoque.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// TESTE DAS CONSULTAS DO PAINEL: roda o JPQL de verdade num H2 em memoria, zerado a cada teste
@DataJpaTest
@Import(PainelService.class)
class PainelServiceTest {

    @Autowired private PainelService service;
    @Autowired private CategoriaRepository categorias;
    @Autowired private FornecedorRepository fornecedores;
    @Autowired private ClienteRepository clientes;
    @Autowired private ProdutoRepository produtos;
    @Autowired private EntradaRepository entradas;
    @Autowired private SaidaRepository saidas;

    private final LocalDate hoje = LocalDate.now();
    private Categoria bebidas;
    private Categoria limpeza;
    private Fornecedor fornecedor;
    private Cliente cliente;

    @BeforeEach
    void cadastrosDeApoio() {
        bebidas = categorias.save(new Categoria("Bebidas"));
        limpeza = categorias.save(new Categoria("Limpeza"));

        fornecedor = new Fornecedor();
        fornecedor.setNome("Distribuidora");
        fornecedor.setCnpj("00.000.000/0001-00");
        fornecedores.save(fornecedor);

        cliente = new Cliente();
        cliente.setNome("Maria");
        cliente.setCpf("000.000.000-00");
        clientes.save(cliente);
    }

    @Test
    void semProdutosDevolveZerosEmVezDeNulo() {
        Painel painel = service.resumo();

        assertThat(painel.totalProdutos()).isZero();
        assertThat(painel.unidadesEmEstoque()).isZero();
        assertThat(painel.valorEmEstoque()).isEqualByComparingTo("0");
        assertThat(painel.unidadesEntradas()).isZero();
        assertThat(painel.unidadesSaidas()).isZero();
        assertThat(painel.maisVendidos()).isEmpty();
        assertThat(painel.paraRepor()).isEmpty();
        assertThat(painel.porCategoria()).isEmpty();
    }

    @Test
    void somaUnidadesEValorDeTodosOsProdutos() {
        produto("Agua", "2.50", 10, bebidas);   // 25,00
        produto("Suco", "4.00", 3, bebidas);    // 12,00
        produto("Sabao", "5.00", 0, limpeza);   //  0,00

        Painel painel = service.resumo();

        assertThat(painel.totalProdutos()).isEqualTo(3);
        assertThat(painel.unidadesEmEstoque()).isEqualTo(13);
        assertThat(painel.valorEmEstoque()).isEqualByComparingTo("37.00");
    }

    @Test
    void contaSeparadoQuemEstaZeradoEQuemEstaAbaixoDoMinimo() {
        produto("Zerado", "1.00", 0, bebidas);
        produto("Com um", "1.00", 1, bebidas);
        produto("Com quatro", "1.00", 4, bebidas);
        produto("No minimo", "1.00", 5, bebidas);   // 5 e o minimo: ja nao e estoque baixo

        Painel painel = service.resumo();

        assertThat(painel.estoqueMinimo()).isEqualTo(5);
        assertThat(painel.produtosSemEstoque()).isEqualTo(1);
        assertThat(painel.produtosEstoqueBaixo()).isEqualTo(2);
    }

    @Test
    void listaParaReporVaiDoMenorEstoqueParaOMaiorEDeixaDeForaQuemTemOMinimo() {
        produto("Com quatro", "1.00", 4, bebidas);
        produto("Zerado", "1.00", 0, bebidas);
        produto("No minimo", "1.00", 5, bebidas);
        produto("Com um", "1.00", 1, bebidas);

        Painel painel = service.resumo();

        assertThat(painel.paraRepor()).extracting(Produto::getNome)
                .containsExactly("Zerado", "Com um", "Com quatro");
    }

    @Test
    void movimentacaoContaSoOsUltimosTrintaDiasIncluindoHoje() {
        Produto agua = produto("Agua", "2.50", 10, bebidas);
        entrada(agua, 20, hoje);
        entrada(agua, 5, hoje.minusDays(29));    // primeiro dia do periodo
        entrada(agua, 50, hoje.minusDays(30));   // um dia antes: fora
        saida(agua, 4, hoje);
        saida(agua, 2, hoje.minusDays(29));
        saida(agua, 70, hoje.minusDays(30));

        Painel painel = service.resumo();

        assertThat(painel.diasDoPeriodo()).isEqualTo(30);
        assertThat(painel.unidadesEntradas()).isEqualTo(25);
        assertThat(painel.unidadesSaidas()).isEqualTo(6);
    }

    @Test
    void maisVendidosSomaAsSaidasDeCadaProdutoNoPeriodo() {
        Produto agua = produto("Agua", "2.50", 10, bebidas);
        Produto suco = produto("Suco", "4.00", 3, bebidas);
        Produto sabao = produto("Sabao", "5.00", 0, limpeza);
        saida(suco, 2, hoje);
        saida(agua, 4, hoje);
        saida(agua, 6, hoje.minusDays(5));
        saida(sabao, 90, hoje.minusDays(30));    // fora do periodo: nao entra na lista

        Painel painel = service.resumo();

        assertThat(painel.maisVendidos()).containsExactly(
                new ProdutoVendido("Agua", 10L),
                new ProdutoVendido("Suco", 2L));
    }

    @Test
    void maisVendidosMostraNoMaximoCinco() {
        for (int i = 1; i <= 6; i++) {
            saida(produto("Produto " + i, "1.00", 10, bebidas), i, hoje);
        }

        Painel painel = service.resumo();

        assertThat(painel.maisVendidos()).extracting(ProdutoVendido::produto)
                .containsExactly("Produto 6", "Produto 5", "Produto 4", "Produto 3", "Produto 2");
    }

    @Test
    void agrupaProdutosUnidadesEValorPorCategoria() {
        produto("Agua", "2.50", 10, bebidas);
        produto("Suco", "4.00", 3, bebidas);
        produto("Sabao", "5.00", 2, limpeza);

        Painel painel = service.resumo();

        assertThat(painel.porCategoria()).extracting(EstoquePorCategoria::categoria)
                .containsExactly("Bebidas", "Limpeza");
        EstoquePorCategoria linhaBebidas = painel.porCategoria().get(0);
        assertThat(linhaBebidas.produtos()).isEqualTo(2);
        assertThat(linhaBebidas.unidades()).isEqualTo(13);
        assertThat(linhaBebidas.valor()).isEqualByComparingTo("37.00");
        EstoquePorCategoria linhaLimpeza = painel.porCategoria().get(1);
        assertThat(linhaLimpeza.produtos()).isEqualTo(1);
        assertThat(linhaLimpeza.unidades()).isEqualTo(2);
        assertThat(linhaLimpeza.valor()).isEqualByComparingTo("10.00");
    }

    // ---------- Apoio ----------

    private Produto produto(String nome, String preco, int estoque, Categoria categoria) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setPreco(new BigDecimal(preco));
        produto.setQuantidadeEstoque(estoque);
        produto.setCategoria(categoria);
        produto.setFornecedor(fornecedor);
        return produtos.save(produto);
    }

    private void entrada(Produto produto, int quantidade, LocalDate data) {
        Entrada entrada = new Entrada();
        entrada.setProduto(produto);
        entrada.setQuantidade(quantidade);
        entrada.setData(data);
        entradas.save(entrada);
    }

    private void saida(Produto produto, int quantidade, LocalDate data) {
        Saida saida = new Saida();
        saida.setProduto(produto);
        saida.setCliente(cliente);
        saida.setQuantidade(quantidade);
        saida.setData(data);
        saidas.save(saida);
    }
}

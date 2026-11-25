package br.com.estoque.dto;

import br.com.estoque.model.Produto;

import java.math.BigDecimal;
import java.util.List;

// Tudo o que a tela Painel mostra, devolvido de uma vez por GET /api/painel
public record Painel(
        long totalProdutos,
        long unidadesEmEstoque,
        BigDecimal valorEmEstoque,          // soma de preco x quantidade de cada produto
        long produtosSemEstoque,            // quantidade igual a zero
        long produtosEstoqueBaixo,          // acima de zero e abaixo do minimo
        int estoqueMinimo,
        int diasDoPeriodo,                  // janela usada nas entradas, saidas e mais vendidos
        long unidadesEntradas,
        long unidadesSaidas,
        List<ProdutoVendido> maisVendidos,
        List<Produto> paraRepor,            // abaixo do minimo, do menor estoque para o maior
        List<EstoquePorCategoria> porCategoria) {
}

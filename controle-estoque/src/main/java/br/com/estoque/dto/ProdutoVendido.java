package br.com.estoque.dto;

// Linha da lista "mais vendidos": quantas unidades de um produto sairam no periodo
public record ProdutoVendido(String produto, Long quantidade) {
}

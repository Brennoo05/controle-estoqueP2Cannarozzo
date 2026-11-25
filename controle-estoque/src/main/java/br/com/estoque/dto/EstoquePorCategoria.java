package br.com.estoque.dto;

import java.math.BigDecimal;

// Linha do resumo por categoria: quantos produtos, quantas unidades e quanto vale o estoque
public record EstoquePorCategoria(String categoria, Long produtos, Long unidades, BigDecimal valor) {
}

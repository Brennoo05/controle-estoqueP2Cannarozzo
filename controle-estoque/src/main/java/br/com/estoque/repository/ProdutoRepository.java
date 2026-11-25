package br.com.estoque.repository;

import br.com.estoque.dto.EstoquePorCategoria;
import br.com.estoque.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // ===== Exclusao logica =====
    // Produtos excluidos (ativo = false) ficam no banco so por causa do historico

    // ... WHERE ativo = true
    List<Produto> findByAtivoTrue();

    // ===== Consultas do Painel (somente produtos ativos) =====
    // sum() devolve null quando nao ha nenhum produto; quem chama trata como zero

    long countByAtivoTrue();

    @Query("select sum(p.quantidadeEstoque) from Produto p where p.ativo = true")
    Long somarUnidades();

    @Query("select sum(p.preco * p.quantidadeEstoque) from Produto p where p.ativo = true")
    BigDecimal somarValorEmEstoque();

    // Spring gera a consulta pelo nome: SELECT count(*) ... WHERE quantidade_estoque = ? AND ativo = true
    long countByQuantidadeEstoqueAndAtivoTrue(Integer quantidade);

    @Query("select count(p) from Produto p where p.ativo = true and p.quantidadeEstoque > 0 and p.quantidadeEstoque < :minimo")
    long contarEstoqueBaixo(@Param("minimo") int minimo);

    // ... WHERE quantidade_estoque < ? AND ativo = true ORDER BY quantidade_estoque, nome
    List<Produto> findByQuantidadeEstoqueLessThanAndAtivoTrueOrderByQuantidadeEstoqueAscNomeAsc(Integer minimo);

    // "select new" monta um EstoquePorCategoria para cada grupo (GROUP BY categoria)
    @Query("""
            select new br.com.estoque.dto.EstoquePorCategoria(
                c.nome, count(p), sum(p.quantidadeEstoque), sum(p.preco * p.quantidadeEstoque))
            from Produto p join p.categoria c
            where p.ativo = true
            group by c.id, c.nome
            order by c.nome
            """)
    List<EstoquePorCategoria> estoquePorCategoria();
}

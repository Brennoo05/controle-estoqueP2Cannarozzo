package br.com.estoque.repository;

import br.com.estoque.dto.EstoquePorCategoria;
import br.com.estoque.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // ===== Consultas do Painel =====
    // sum() devolve null quando nao ha nenhum produto; quem chama trata como zero

    @Query("select sum(p.quantidadeEstoque) from Produto p")
    Long somarUnidades();

    @Query("select sum(p.preco * p.quantidadeEstoque) from Produto p")
    BigDecimal somarValorEmEstoque();

    // Spring gera a consulta pelo nome: SELECT count(*) ... WHERE quantidade_estoque = ?
    long countByQuantidadeEstoque(Integer quantidade);

    @Query("select count(p) from Produto p where p.quantidadeEstoque > 0 and p.quantidadeEstoque < :minimo")
    long contarEstoqueBaixo(@Param("minimo") int minimo);

    // ... WHERE quantidade_estoque < ? ORDER BY quantidade_estoque, nome
    List<Produto> findByQuantidadeEstoqueLessThanOrderByQuantidadeEstoqueAscNomeAsc(Integer minimo);

    // "select new" monta um EstoquePorCategoria para cada grupo (GROUP BY categoria)
    @Query("""
            select new br.com.estoque.dto.EstoquePorCategoria(
                c.nome, count(p), sum(p.quantidadeEstoque), sum(p.preco * p.quantidadeEstoque))
            from Produto p join p.categoria c
            group by c.id, c.nome
            order by c.nome
            """)
    List<EstoquePorCategoria> estoquePorCategoria();
}

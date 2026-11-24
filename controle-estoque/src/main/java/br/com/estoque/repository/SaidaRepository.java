package br.com.estoque.repository;

import br.com.estoque.dto.ProdutoVendido;
import br.com.estoque.model.Saida;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SaidaRepository extends JpaRepository<Saida, Long> {

    // Unidades vendidas de uma data em diante (null se nao houve nenhuma saida)
    @Query("select sum(s.quantidade) from Saida s where s.data >= :inicio")
    Long somarQuantidadeDesde(@Param("inicio") LocalDate inicio);

    // Soma as saidas de cada produto e ordena da maior para a menor.
    // O Pageable limita quantas linhas voltam (ex.: so as 5 primeiras).
    @Query("""
            select new br.com.estoque.dto.ProdutoVendido(p.nome, sum(s.quantidade))
            from Saida s join s.produto p
            where s.data >= :inicio
            group by p.id, p.nome
            order by sum(s.quantidade) desc, p.nome
            """)
    List<ProdutoVendido> maisVendidosDesde(@Param("inicio") LocalDate inicio, Pageable limite);
}

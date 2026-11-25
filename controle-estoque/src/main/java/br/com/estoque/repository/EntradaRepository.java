package br.com.estoque.repository;

import br.com.estoque.model.Entrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {

    // Unidades compradas de uma data em diante (null se nao houve nenhuma entrada)
    @Query("select sum(e.quantidade) from Entrada e where e.data >= :inicio")
    Long somarQuantidadeDesde(@Param("inicio") LocalDate inicio);
}

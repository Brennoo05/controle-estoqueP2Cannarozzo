package br.com.estoque.service;

import br.com.estoque.dto.Painel;
import br.com.estoque.repository.EntradaRepository;
import br.com.estoque.repository.ProdutoRepository;
import br.com.estoque.repository.SaidaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

// Monta o resumo do estoque mostrado na tela Painel
@Service
public class PainelService {

    private static final int DIAS_DO_PERIODO = 30;
    private static final int QUANTOS_MAIS_VENDIDOS = 5;

    private final ProdutoRepository produtoRepository;
    private final EntradaRepository entradaRepository;
    private final SaidaRepository saidaRepository;
    private final int estoqueMinimo;

    // estoque.minimo pode ser definido no application.properties; sem isso vale 5,
    // o mesmo limite que a tela de Produtos usa para marcar "Estoque baixo"
    public PainelService(ProdutoRepository produtoRepository, EntradaRepository entradaRepository,
                         SaidaRepository saidaRepository, @Value("${estoque.minimo:5}") int estoqueMinimo) {
        this.produtoRepository = produtoRepository;
        this.entradaRepository = entradaRepository;
        this.saidaRepository = saidaRepository;
        this.estoqueMinimo = estoqueMinimo;
    }

    // readOnly: so consulta, e todas as consultas enxergam o banco no mesmo instante
    @Transactional(readOnly = true)
    public Painel resumo() {
        // 30 dias contando hoje: de 29 dias atras ate hoje
        LocalDate inicio = LocalDate.now().minusDays(DIAS_DO_PERIODO - 1);
        BigDecimal valor = produtoRepository.somarValorEmEstoque();

        return new Painel(
                produtoRepository.count(),
                zeroSeNulo(produtoRepository.somarUnidades()),
                valor == null ? BigDecimal.ZERO : valor,
                produtoRepository.countByQuantidadeEstoque(0),
                produtoRepository.contarEstoqueBaixo(estoqueMinimo),
                estoqueMinimo,
                DIAS_DO_PERIODO,
                zeroSeNulo(entradaRepository.somarQuantidadeDesde(inicio)),
                zeroSeNulo(saidaRepository.somarQuantidadeDesde(inicio)),
                saidaRepository.maisVendidosDesde(inicio, PageRequest.of(0, QUANTOS_MAIS_VENDIDOS)),
                produtoRepository.findByQuantidadeEstoqueLessThanOrderByQuantidadeEstoqueAscNomeAsc(estoqueMinimo),
                produtoRepository.estoquePorCategoria());
    }

    private long zeroSeNulo(Long valor) {
        return valor == null ? 0 : valor;
    }
}

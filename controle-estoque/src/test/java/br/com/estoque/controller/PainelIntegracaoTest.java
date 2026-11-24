package br.com.estoque.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TESTE DE INTEGRACAO do painel: da rota HTTP ate o banco, passando pelo login
@SpringBootTest
@AutoConfigureMockMvc
class PainelIntegracaoTest {

    @Autowired
    private MockMvc mvc;

    private final RequestPostProcessor admin = user("admin").roles("ADMIN");
    private final RequestPostProcessor comum = user("usuario").roles("USER");

    // O banco e compartilhado com os outros testes de integracao, entao o teste
    // compara o painel de antes com o de depois em vez de esperar valores fixos.
    @Test
    void usuarioComumVeNoPainelOEfeitoDeUmaCompraEDeUmaVenda() throws Exception {
        String antes = painel();

        long categoria = criar("/api/categorias", "{\"nome\":\"Categoria do painel\"}");
        long fornecedor = criar("/api/fornecedores", "{\"nome\":\"Fornecedor do painel\",\"cnpj\":\"CNPJ-PAINEL\"}");
        long cliente = criar("/api/clientes", "{\"nome\":\"Cliente do painel\",\"cpf\":\"CPF-PAINEL\"}");
        long produto = criar("/api/produtos", "{\"nome\":\"Produto do painel\",\"preco\":2.5,"
                + "\"categoria\":{\"id\":" + categoria + "},\"fornecedor\":{\"id\":" + fornecedor + "}}");
        criar("/api/entradas", "{\"produto\":{\"id\":" + produto + "},\"quantidade\":10}");
        criar("/api/saidas", "{\"produto\":{\"id\":" + produto + "},\"cliente\":{\"id\":" + cliente + "},\"quantidade\":4}");

        String depois = painel();

        assertEquals(1, diferenca(antes, depois, "$.totalProdutos"));
        assertEquals(6, diferenca(antes, depois, "$.unidadesEmEstoque"));      // 10 - 4
        assertEquals(15.0, diferenca(antes, depois, "$.valorEmEstoque"));      // 6 x 2,50
        assertEquals(10, diferenca(antes, depois, "$.unidadesEntradas"));
        assertEquals(4, diferenca(antes, depois, "$.unidadesSaidas"));
    }

    private String painel() throws Exception {
        return mvc.perform(get("/api/painel").with(comum))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private double diferenca(String antes, String depois, String campo) {
        Number valorAntes = JsonPath.read(antes, campo);
        Number valorDepois = JsonPath.read(depois, campo);
        return valorDepois.doubleValue() - valorAntes.doubleValue();
    }

    // Faz um POST como admin, espera 201 e devolve o id criado
    private long criar(String url, String json) throws Exception {
        String resposta = mvc.perform(post(url).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }
}

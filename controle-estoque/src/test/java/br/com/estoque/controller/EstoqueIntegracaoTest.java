package br.com.estoque.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// TESTE DE INTEGRACAO: sobe a aplicacao inteira (Controller -> Service -> Repository -> H2)
@SpringBootTest
@AutoConfigureMockMvc
class EstoqueIntegracaoTest {

    @Autowired
    private MockMvc mvc;

    private final RequestPostProcessor admin = user("admin").roles("ADMIN");
    private final RequestPostProcessor comum = user("usuario").roles("USER");

    @Test
    void semLoginDeveRetornar401() throws Exception {
        mvc.perform(get("/api/produtos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioComumNaoPodeExcluir() throws Exception {
        mvc.perform(delete("/api/categorias/1").with(comum))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioComumNaoAcessaCadastroDeUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").with(comum))
                .andExpect(status().isForbidden());
    }

    @Test
    void crudDeCategoria() throws Exception {
        long id = criar("/api/categorias", "{\"nome\":\"Higiene\"}");

        mvc.perform(get("/api/categorias/" + id).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Higiene"));

        mvc.perform(put("/api/categorias/" + id).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Higiene Pessoal\"}"))
                .andExpect(jsonPath("$.nome").value("Higiene Pessoal"));

        mvc.perform(delete("/api/categorias/" + id).with(admin))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/categorias/" + id).with(admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void entradaAumentaESaidaDiminuiOEstoque() throws Exception {
        long categoria = criar("/api/categorias", "{\"nome\":\"Teste\"}");
        long fornecedor = criar("/api/fornecedores", "{\"nome\":\"Forn Teste\",\"cnpj\":\"99.999.999/0001-99\"}");
        long cliente = criar("/api/clientes", "{\"nome\":\"Joao\",\"cpf\":\"999.999.999-99\"}");
        long produto = criar("/api/produtos", "{\"nome\":\"Sabao\",\"preco\":5.0,"
                + "\"categoria\":{\"id\":" + categoria + "},\"fornecedor\":{\"id\":" + fornecedor + "}}");

        // Entrada de 10 -> estoque 10
        criar("/api/entradas", "{\"produto\":{\"id\":" + produto + "},\"quantidade\":10}");
        mvc.perform(get("/api/produtos/" + produto).with(admin))
                .andExpect(jsonPath("$.quantidadeEstoque").value(10));

        // Saida de 3 -> estoque 7
        criar("/api/saidas", "{\"produto\":{\"id\":" + produto + "},\"cliente\":{\"id\":" + cliente + "},\"quantidade\":3}");
        mvc.perform(get("/api/produtos/" + produto).with(admin))
                .andExpect(jsonPath("$.quantidadeEstoque").value(7));

        // Saida maior que o estoque -> 400 e estoque continua 7
        mvc.perform(post("/api/saidas").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produto\":{\"id\":" + produto + "},\"cliente\":{\"id\":" + cliente + "},\"quantidade\":100}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/produtos/" + produto).with(admin))
                .andExpect(jsonPath("$.quantidadeEstoque").value(7));
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

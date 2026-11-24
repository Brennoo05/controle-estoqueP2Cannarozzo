package br.com.estoque.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TESTE DE INTEGRACAO das regras de negocio: correcao de entrada, dados incompletos e protecao do administrador
@SpringBootTest
@AutoConfigureMockMvc
class RegrasDeNegocioIntegracaoTest {

    // CNPJ, CPF e login sao unicos no banco: cada teste pega um numero novo
    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired
    private MockMvc mvc;

    private final RequestPostProcessor admin = user("admin").roles("ADMIN");

    // ---------- Correcao de uma entrada ja lancada ----------

    @Test
    void aumentarEntradaFuncionaMesmoComParteDoEstoqueVendida() throws Exception {
        long produto = novoProduto();
        long entrada = criar("/api/entradas", entrada(produto, "10"));
        criar("/api/saidas", saida(produto, novoCliente(), 3));

        mvc.perform(put("/api/entradas/" + entrada).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(entrada(produto, "15")))
                .andExpect(status().isOk());

        assertEquals(12, estoqueDe(produto)); // 15 comprados - 3 vendidos
    }

    @Test
    void diminuirEntradaFuncionaEnquantoOEstoqueCobreADiferenca() throws Exception {
        long produto = novoProduto();
        long entrada = criar("/api/entradas", entrada(produto, "10"));
        criar("/api/saidas", saida(produto, novoCliente(), 3));

        mvc.perform(put("/api/entradas/" + entrada).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(entrada(produto, "5")))
                .andExpect(status().isOk());

        assertEquals(2, estoqueDe(produto)); // 5 comprados - 3 vendidos
    }

    @Test
    void diminuirEntradaAbaixoDoQueJaFoiVendidoERecusado() throws Exception {
        long produto = novoProduto();
        long entrada = criar("/api/entradas", entrada(produto, "10"));
        criar("/api/saidas", saida(produto, novoCliente(), 3));

        // 2 comprados - 3 vendidos deixaria o estoque negativo
        mvc.perform(put("/api/entradas/" + entrada).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(entrada(produto, "2")))
                .andExpect(status().isBadRequest());

        assertEquals(7, estoqueDe(produto)); // nada mudou
    }

    // ---------- Dados incompletos ou em formato errado viram 400 com {"erro": ...} ----------

    @Test
    void entradaSemIdDoProdutoRetorna400() throws Exception {
        mvc.perform(post("/api/entradas").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produto\":{},\"quantidade\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void saidaSemIdDoClienteRetorna400ENaoMexeNoEstoque() throws Exception {
        long produto = novoProduto();
        criar("/api/entradas", entrada(produto, "10"));

        mvc.perform(post("/api/saidas").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produto\":{\"id\":" + produto + "},\"cliente\":{},\"quantidade\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());

        assertEquals(10, estoqueDe(produto));
    }

    @Test
    void produtoSemIdDaCategoriaRetorna400() throws Exception {
        long fornecedor = novoFornecedor();

        mvc.perform(post("/api/produtos").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Sem categoria\",\"preco\":1.0,"
                                + "\"categoria\":{},\"fornecedor\":{\"id\":" + fornecedor + "}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void produtoSemIdDoFornecedorRetorna400() throws Exception {
        long categoria = criar("/api/categorias", "{\"nome\":\"Categoria de teste\"}");

        mvc.perform(post("/api/produtos").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Sem fornecedor\",\"preco\":1.0,"
                                + "\"categoria\":{\"id\":" + categoria + "},\"fornecedor\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void quantidadeDecimalERecusadaEmVezDeArredondada() throws Exception {
        long produto = novoProduto();

        mvc.perform(post("/api/entradas").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(entrada(produto, "2.7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());

        assertEquals(0, estoqueDe(produto));
    }

    // ---------- Apoio ----------

    private String entrada(long produto, String quantidade) {
        return "{\"produto\":{\"id\":" + produto + "},\"quantidade\":" + quantidade + "}";
    }

    private String saida(long produto, long cliente, int quantidade) {
        return "{\"produto\":{\"id\":" + produto + "},\"cliente\":{\"id\":" + cliente + "},\"quantidade\":" + quantidade + "}";
    }

    private long novoFornecedor() throws Exception {
        int n = SEQUENCIA.incrementAndGet();
        return criar("/api/fornecedores", "{\"nome\":\"Fornecedor " + n + "\",\"cnpj\":\"CNPJ-REGRAS-" + n + "\"}");
    }

    private long novoCliente() throws Exception {
        int n = SEQUENCIA.incrementAndGet();
        return criar("/api/clientes", "{\"nome\":\"Cliente " + n + "\",\"cpf\":\"CPF-REGRAS-" + n + "\"}");
    }

    private long novoProduto() throws Exception {
        long categoria = criar("/api/categorias", "{\"nome\":\"Categoria de teste\"}");
        long fornecedor = novoFornecedor();
        return criar("/api/produtos", "{\"nome\":\"Produto de teste\",\"preco\":5.0,"
                + "\"categoria\":{\"id\":" + categoria + "},\"fornecedor\":{\"id\":" + fornecedor + "}}");
    }

    private long novoUsuario(String perfil) throws Exception {
        String login = "usuario-teste-" + SEQUENCIA.incrementAndGet();
        return criar("/api/usuarios",
                "{\"login\":\"" + login + "\",\"senha\":\"segredo123\",\"perfil\":\"" + perfil + "\"}");
    }

    private int estoqueDe(long produto) throws Exception {
        String resposta = mvc.perform(get("/api/produtos/" + produto).with(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.quantidadeEstoque");
    }

    private long idDoUsuarioLogado() throws Exception {
        String resposta = mvc.perform(get("/api/usuarios/me").with(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
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

package br.com.estoque.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TESTE DE INTEGRACAO da exclusao logica de produtos: o produto some da lista,
// o historico de entradas e saidas continua e nenhuma nova movimentacao e aceita
@SpringBootTest
@AutoConfigureMockMvc
class ExclusaoLogicaProdutoIntegracaoTest {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired
    private MockMvc mvc;

    private final RequestPostProcessor admin = user("admin").roles("ADMIN");

    @Test
    void excluirProdutoComMovimentacoesPreservaOHistorico() throws Exception {
        long produto = novoProduto("Arroz 5 kg");
        long entrada = criar("/api/entradas", entrada(produto, 20));
        long saida = criar("/api/saidas", saida(produto, novoCliente(), 5));

        mvc.perform(delete("/api/produtos/" + produto).with(admin))
                .andExpect(status().isNoContent());

        // sumiu da lista de produtos disponiveis
        assertFalse(idsDaLista("/api/produtos").contains(produto));

        // a entrada e a saida continuam no historico, identificando o produto
        assertTrue(idsDaLista("/api/entradas").contains(entrada));
        assertTrue(idsDaLista("/api/saidas").contains(saida));
        mvc.perform(get("/api/entradas/" + entrada).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(20))
                .andExpect(jsonPath("$.produto.nome").value("Arroz 5 kg"))
                .andExpect(jsonPath("$.produto.ativo").value(false));
        mvc.perform(get("/api/saidas/" + saida).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(5))
                .andExpect(jsonPath("$.produto.nome").value("Arroz 5 kg"));

        // o saldo nao foi alterado pela exclusao: 20 - 5
        mvc.perform(get("/api/produtos/" + produto).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEstoque").value(15))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void produtoSemMovimentacaoTambemEExcluidoLogicamente() throws Exception {
        long produto = novoProduto("Produto sem historico");

        mvc.perform(delete("/api/produtos/" + produto).with(admin))
                .andExpect(status().isNoContent());

        assertFalse(idsDaLista("/api/produtos").contains(produto));
        mvc.perform(get("/api/produtos/" + produto).with(admin))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void produtoExcluidoNaoRecebeNovaEntradaNemSaidaPelaApi() throws Exception {
        long produto = novoProduto("Feijao 1 kg");
        criar("/api/entradas", entrada(produto, 10));
        long cliente = novoCliente();
        mvc.perform(delete("/api/produtos/" + produto).with(admin)).andExpect(status().isNoContent());

        mvc.perform(post("/api/entradas").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(entrada(produto, 5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("foi excluído")));
        mvc.perform(post("/api/saidas").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(saida(produto, cliente, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("foi excluído")));

        assertEquals(10, estoqueDe(produto)); // nada mudou
    }

    @Test
    void movimentacaoNaoPodeSerTrocadaParaProdutoExcluido() throws Exception {
        long ativo = novoProduto("Produto ativo");
        long excluido = novoProduto("Produto excluido");
        long entrada = criar("/api/entradas", entrada(ativo, 10));
        long saida = criar("/api/saidas", saida(ativo, novoCliente(), 2));
        mvc.perform(delete("/api/produtos/" + excluido).with(admin)).andExpect(status().isNoContent());

        mvc.perform(put("/api/entradas/" + entrada).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(entrada(excluido, 10)))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/saidas/" + saida).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(saida(excluido, novoCliente(), 2)))
                .andExpect(status().isBadRequest());

        assertEquals(8, estoqueDe(ativo));
        assertEquals(0, estoqueDe(excluido));
    }

    @Test
    void historicoDeProdutoExcluidoNaoPodeSerAlteradoNemApagado() throws Exception {
        long produto = novoProduto("Macarrao 500 g");
        long entrada = criar("/api/entradas", entrada(produto, 20));
        long saida = criar("/api/saidas", saida(produto, novoCliente(), 5));
        mvc.perform(delete("/api/produtos/" + produto).with(admin)).andExpect(status().isNoContent());

        mvc.perform(put("/api/entradas/" + entrada).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(entrada(produto, 30)))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/entradas/" + entrada).with(admin))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/saidas/" + saida).with(admin))
                .andExpect(status().isBadRequest());

        assertTrue(idsDaLista("/api/entradas").contains(entrada));
        assertTrue(idsDaLista("/api/saidas").contains(saida));
        assertEquals(15, estoqueDe(produto));
    }

    @Test
    void produtoExcluidoNaoPodeSerEditadoNemExcluidoDeNovo() throws Exception {
        long produto = novoProduto("Oleo 900 ml");
        mvc.perform(delete("/api/produtos/" + produto).with(admin)).andExpect(status().isNoContent());

        mvc.perform(put("/api/produtos/" + produto).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonProduto("Oleo editado")))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/produtos/" + produto).with(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void produtoNovoNasceAtivoMesmoQueOJsonDigaOContrario() throws Exception {
        String json = jsonProduto("Produto novo").replaceFirst("\\{", "{\"ativo\":false,");
        long produto = criar("/api/produtos", json);

        assertTrue(idsDaLista("/api/produtos").contains(produto));
    }

    // ---------- Apoio ----------

    private String entrada(long produto, int quantidade) {
        return "{\"produto\":{\"id\":" + produto + "},\"quantidade\":" + quantidade + "}";
    }

    private String saida(long produto, long cliente, int quantidade) {
        return "{\"produto\":{\"id\":" + produto + "},\"cliente\":{\"id\":" + cliente + "},\"quantidade\":" + quantidade + "}";
    }

    private long novoCliente() throws Exception {
        int n = SEQUENCIA.incrementAndGet();
        return criar("/api/clientes", "{\"nome\":\"Cliente " + n + "\",\"cpf\":\"CPF-EXCLUSAO-" + n + "\"}");
    }

    private String jsonProduto(String nome) throws Exception {
        int n = SEQUENCIA.incrementAndGet();
        long categoria = criar("/api/categorias", "{\"nome\":\"Categoria exclusao " + n + "\"}");
        long fornecedor = criar("/api/fornecedores",
                "{\"nome\":\"Fornecedor " + n + "\",\"cnpj\":\"CNPJ-EXCLUSAO-" + n + "\"}");
        return "{\"nome\":\"" + nome + "\",\"preco\":5.0,"
                + "\"categoria\":{\"id\":" + categoria + "},\"fornecedor\":{\"id\":" + fornecedor + "}}";
    }

    private long novoProduto(String nome) throws Exception {
        return criar("/api/produtos", jsonProduto(nome));
    }

    private List<Long> idsDaLista(String url) throws Exception {
        String resposta = mvc.perform(get(url).with(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(resposta, "$[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    private int estoqueDe(long produto) throws Exception {
        String resposta = mvc.perform(get("/api/produtos/" + produto).with(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.quantidadeEstoque");
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

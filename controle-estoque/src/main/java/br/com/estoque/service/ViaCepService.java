package br.com.estoque.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Integracao com API externa: https://viacep.com.br
@Service
public class ViaCepService {

    // Formato que o ViaCEP devolve (so os campos que usamos)
    public record ViaCepResposta(String cep, String logradouro, String bairro,
                                 String localidade, String uf, String erro) {}

    // Formato que devolvemos ao frontend (mesmos nomes dos campos de Fornecedor/Cliente)
    public record Endereco(String cep, String endereco, String cidade, String uf) {}

    private final RestClient restClient = RestClient.create("https://viacep.com.br/ws");

    public Endereco buscar(String cep) {
        String apenasNumeros = cep == null ? "" : cep.replaceAll("\\D", "");
        if (apenasNumeros.length() != 8) {
            throw new RegraNegocioException("CEP deve ter 8 digitos.");
        }

        ViaCepResposta resposta;
        try {
            resposta = restClient.get()
                    .uri("/{cep}/json/", apenasNumeros)
                    .retrieve()
                    .body(ViaCepResposta.class);
        } catch (RestClientException e) {
            throw new RegraNegocioException("Nao foi possivel consultar o ViaCEP.");
        }

        if (resposta == null || resposta.erro() != null) {
            throw new RegraNegocioException("CEP nao encontrado.");
        }
        return new Endereco(resposta.cep(),
                resposta.logradouro() + " - " + resposta.bairro(),
                resposta.localidade(),
                resposta.uf());
    }
}

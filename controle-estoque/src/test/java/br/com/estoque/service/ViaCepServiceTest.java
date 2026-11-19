package br.com.estoque.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

// TESTE UNITARIO: valida o CEP antes de chamar a API externa
class ViaCepServiceTest {

    private final ViaCepService service = new ViaCepService();

    @Test
    void cepComMenosDeOitoDigitosDeveSerRecusado() {
        assertThrows(RegraNegocioException.class, () -> service.buscar("1234"));
    }

    @Test
    void cepVazioDeveSerRecusado() {
        assertThrows(RegraNegocioException.class, () -> service.buscar(""));
    }
}

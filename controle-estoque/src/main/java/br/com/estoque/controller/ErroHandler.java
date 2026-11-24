package br.com.estoque.controller;

import br.com.estoque.service.RegraNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

// Transforma excecoes em respostas HTTP com JSON {"erro": "..."}
@RestControllerAdvice
public class ErroHandler {

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> naoEncontrado() {
        return Map.of("erro", "Registro nao encontrado.");
    }

    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> regraNegocio(RegraNegocioException e) {
        return Map.of("erro", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> validacao(MethodArgumentNotValidException e) {
        var campo = e.getBindingResult().getFieldErrors().get(0);
        return Map.of("erro", "Campo '" + campo.getField() + "' " + campo.getDefaultMessage());
    }

    // JSON que nao da para ler: texto onde vai numero, quantidade quebrada, perfil que nao existe
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> formatoInvalido() {
        return Map.of("erro", "Dados em formato inválido. Confira os campos: a quantidade deve ser um número inteiro.");
    }

    // Ex.: excluir categoria que ainda tem produtos, ou CPF/CNPJ/login repetido
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> integridade() {
        return Map.of("erro", "Operacao viola integridade dos dados (registro em uso ou duplicado).");
    }
}

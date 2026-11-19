package br.com.estoque.controller;

import br.com.estoque.service.ViaCepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cep")
@Tag(name = "CEP", description = "Consulta de endereco na API externa ViaCEP")
public class CepController {

    private final ViaCepService viaCepService;

    public CepController(ViaCepService viaCepService) {
        this.viaCepService = viaCepService;
    }

    @GetMapping("/{cep}")
    @Operation(summary = "Busca endereco pelo CEP")
    public ViaCepService.Endereco buscar(@PathVariable String cep) {
        return viaCepService.buscar(cep);
    }
}

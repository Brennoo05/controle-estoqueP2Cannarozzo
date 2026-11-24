package br.com.estoque.controller;

import br.com.estoque.dto.Painel;
import br.com.estoque.service.PainelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel")
@Tag(name = "Painel", description = "Resumo do estoque: totais, movimentacao recente e reposicao")
public class PainelController {

    private final PainelService service;

    public PainelController(PainelService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Resumo do estoque (liberado para qualquer perfil)")
    public Painel resumo() {
        return service.resumo();
    }
}

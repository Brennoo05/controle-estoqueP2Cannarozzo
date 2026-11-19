package br.com.estoque.controller;

import br.com.estoque.model.Saida;
import br.com.estoque.service.SaidaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/saidas")
@Tag(name = "Saida", description = "Saidas de estoque (vendas) - diminuem o estoque")
public class SaidaController {

    private final SaidaService service;

    public SaidaController(SaidaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista todos")
    public List<Saida> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca por id")
    public Saida buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra")
    public Saida criar(@RequestBody @Valid Saida saida) {
        return service.registrar(saida);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza")
    public Saida atualizar(@PathVariable Long id, @RequestBody @Valid Saida saida) {
        return service.atualizar(id, saida);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui (somente ADMIN)")
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}

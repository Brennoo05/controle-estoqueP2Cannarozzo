package br.com.estoque.controller;

import br.com.estoque.model.Entrada;
import br.com.estoque.service.EntradaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entradas")
@Tag(name = "Entrada", description = "Entradas de estoque (compras) - aumentam o estoque")
public class EntradaController {

    private final EntradaService service;

    public EntradaController(EntradaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista todos")
    public List<Entrada> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca por id")
    public Entrada buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra")
    public Entrada criar(@RequestBody @Valid Entrada entrada) {
        return service.registrar(entrada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza")
    public Entrada atualizar(@PathVariable Long id, @RequestBody @Valid Entrada entrada) {
        return service.atualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui (somente ADMIN)")
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}

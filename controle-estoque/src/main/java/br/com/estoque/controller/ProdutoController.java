package br.com.estoque.controller;

import br.com.estoque.model.Produto;
import br.com.estoque.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produtos")
@Tag(name = "Produto", description = "Produtos (estoque muda apenas por entradas e saidas)")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista todos")
    public List<Produto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca por id")
    public Produto buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra")
    public Produto criar(@RequestBody @Valid Produto produto) {
        return service.salvar(produto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza")
    public Produto atualizar(@PathVariable Long id, @RequestBody @Valid Produto produto) {
        return service.atualizar(id, produto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui (somente ADMIN)")
    public ResponseEntity<Map<String, String>> excluir(@PathVariable Long id) {
        try {
            service.excluir(id);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro",
                    "Não é possível excluir este produto, pois ele possui um histórico de movimentações de estoque associado. "
                            + "A exclusão comprometeria a integridade desse histórico."));
        }
        return ResponseEntity.noContent().build();
    }
}

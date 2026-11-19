package br.com.estoque.controller;

import br.com.estoque.model.Usuario;
import br.com.estoque.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuario", description = "Usuarios do sistema (somente ADMIN)")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista todos")
    public List<Usuario> listar() {
        return service.listar();
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o usuario logado (liberado para qualquer perfil)")
    public Usuario usuarioLogado(Principal principal) {
        return service.buscarPorLogin(principal.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca por id")
    public Usuario buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra")
    public Usuario criar(@RequestBody @Valid Usuario usuario) {
        return service.salvar(usuario);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza")
    public Usuario atualizar(@PathVariable Long id, @RequestBody @Valid Usuario usuario) {
        return service.atualizar(id, usuario);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui (somente ADMIN)")
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}

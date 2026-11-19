package br.com.estoque.service;

import br.com.estoque.model.Usuario;
import br.com.estoque.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listar() {
        return repository.findAll();
    }

    public Usuario buscar(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Usuario buscarPorLogin(String login) {
        return repository.findByLogin(login).orElseThrow();
    }

    public Usuario salvar(Usuario usuario) {
        if (usuario.getSenha() == null || usuario.getSenha().isBlank()) {
            throw new RegraNegocioException("A senha e obrigatoria.");
        }
        usuario.setId(null);
        // Nunca salvamos a senha pura: guardamos o hash BCrypt
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return repository.save(usuario);
    }

    public Usuario atualizar(Long id, Usuario dados) {
        Usuario usuario = buscar(id);
        usuario.setLogin(dados.getLogin());
        usuario.setPerfil(dados.getPerfil());
        // Senha em branco = mantem a senha atual
        if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dados.getSenha()));
        }
        return repository.save(usuario);
    }

    public void excluir(Long id) {
        repository.delete(buscar(id));
    }
}

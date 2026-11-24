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
        // Sem nenhum ADMIN ninguem mais consegue cadastrar usuarios nem excluir registros
        boolean deixaDeSerAdmin = usuario.getPerfil() == Usuario.Perfil.ADMIN
                && dados.getPerfil() != Usuario.Perfil.ADMIN;
        if (deixaDeSerAdmin && repository.countByPerfil(Usuario.Perfil.ADMIN) == 1) {
            throw new RegraNegocioException("O sistema precisa de pelo menos um administrador.");
        }
        usuario.setLogin(dados.getLogin());
        usuario.setPerfil(dados.getPerfil());
        // Senha em branco = mantem a senha atual
        if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dados.getSenha()));
        }
        return repository.save(usuario);
    }

    // loginLogado = quem esta fazendo a exclusao
    public void excluir(Long id, String loginLogado) {
        Usuario usuario = buscar(id);
        // Como so ADMIN exclui, impedir a auto-exclusao tambem garante que sempre sobra um ADMIN
        if (usuario.getLogin().equals(loginLogado)) {
            throw new RegraNegocioException("Você não pode excluir o próprio usuário.");
        }
        repository.delete(usuario);
    }
}

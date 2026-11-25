package br.com.estoque.repository;

import br.com.estoque.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Spring gera a consulta pelo nome do metodo: SELECT ... WHERE login = ?
    Optional<Usuario> findByLogin(String login);

    // SELECT count(*) ... WHERE perfil = ?
    long countByPerfil(Usuario.Perfil perfil);
}

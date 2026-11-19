package br.com.estoque.config;

import br.com.estoque.model.*;
import br.com.estoque.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Roda ao iniciar a aplicacao: cria usuarios e alguns dados de exemplo se o banco estiver vazio
@Component
public class DadosIniciais implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public DadosIniciais(UsuarioRepository usuarioRepository, CategoriaRepository categoriaRepository,
                         FornecedorRepository fornecedorRepository, ProdutoRepository produtoRepository,
                         ClienteRepository clienteRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) return;

        usuarioRepository.save(new Usuario("admin", passwordEncoder.encode("admin123"), Usuario.Perfil.ADMIN));
        usuarioRepository.save(new Usuario("usuario", passwordEncoder.encode("user123"), Usuario.Perfil.USER));

        Categoria bebidas = categoriaRepository.save(new Categoria("Bebidas"));
        categoriaRepository.save(new Categoria("Limpeza"));

        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setNome("Distribuidora Santos");
        fornecedor.setCnpj("12.345.678/0001-90");
        fornecedor.setCep("11010-000");
        fornecedor.setCidade("Santos");
        fornecedor.setUf("SP");
        fornecedorRepository.save(fornecedor);

        Produto produto = new Produto();
        produto.setNome("Agua mineral 500ml");
        produto.setPreco(new BigDecimal("2.50"));
        produto.setCategoria(bebidas);
        produto.setFornecedor(fornecedor);
        produtoRepository.save(produto);

        Cliente cliente = new Cliente();
        cliente.setNome("Maria Silva");
        cliente.setCpf("123.456.789-00");
        cliente.setTelefone("(13) 99999-0000");
        clienteRepository.save(cliente);
    }
}

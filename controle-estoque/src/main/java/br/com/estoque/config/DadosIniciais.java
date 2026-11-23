package br.com.estoque.config;

import br.com.estoque.model.*;
import br.com.estoque.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Cadastra as categorias padrao e cria dados de exemplo na primeira inicializacao.
@Component
public class DadosIniciais implements CommandLineRunner {

    private static final String BEBIDAS = "Bebidas";
    private static final List<String> CATEGORIAS_PADRAO = List.of(
            BEBIDAS, "Alimentação", "Mercearia", "Hortifruti", "Açougue", "Peixaria",
            "Padaria e Confeitaria", "Frios e Laticínios", "Congelados", "Limpeza",
            "Higiene Pessoal", "Utilidades Domésticas", "Produtos para Pets"
    );

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
        Categoria bebidas = cadastrarCategoriasPadrao();
        if (usuarioRepository.count() > 0) return;

        usuarioRepository.save(new Usuario("admin", passwordEncoder.encode("admin123"), Usuario.Perfil.ADMIN));
        usuarioRepository.save(new Usuario("usuario", passwordEncoder.encode("user123"), Usuario.Perfil.USER));

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

    private Categoria cadastrarCategoriasPadrao() {
        Map<String, Categoria> categoriasPorNome = new HashMap<>();
        for (Categoria categoria : categoriaRepository.findAll()) {
            categoriasPorNome.putIfAbsent(chaveCategoria(categoria.getNome()), categoria);
        }
        for (String nome : CATEGORIAS_PADRAO) {
            categoriasPorNome.computeIfAbsent(chaveCategoria(nome),
                    chave -> categoriaRepository.save(new Categoria(nome)));
        }
        return categoriasPorNome.get(chaveCategoria(BEBIDAS));
    }

    private String chaveCategoria(String nome) {
        return nome.strip().toLowerCase(Locale.ROOT);
    }
}

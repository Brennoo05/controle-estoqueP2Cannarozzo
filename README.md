# Controle de Estoque

Sistema web para controle de estoque de um pequeno comércio: cadastro de produtos, categorias, fornecedores e clientes, com registro de **entradas** (compras) e **saídas** (vendas) que atualizam o estoque automaticamente. Uma saída nunca pode ser maior que a quantidade disponível.

Projeto Final — Desenvolvimento de Software II (DS-II).

## Integrantes

| Nome | GitHub |
|------|--------|
| Lucas Viana dos Santos | @iluccs |
| Brenno Rodrigues | @Brennoo05 |
| Murilo Novaes | @kiillerzin |

## Funcionalidades

- **Painel** (tela inicial): total de produtos, unidades e valor em estoque, produtos zerados ou abaixo do mínimo, unidades que entraram e saíram nos últimos 30 dias, os cinco mais vendidos, a lista para repor e o estoque por categoria. Os dados vêm de `GET /api/painel`.
- **Cadastros** de produtos, categorias, fornecedores, clientes e usuários, com busca, filtro por categoria e exportação em CSV.
- **Entradas e saídas** que atualizam o estoque na mesma transação.
- **Endereço pelo CEP** nos cadastros de fornecedor e cliente (ViaCEP).

### Regras de negócio

- Uma saída nunca pode ser maior que o estoque disponível.
- Corrigir uma entrada aplica só a diferença entre a quantidade nova e a antiga, então dá para ajustar um lançamento mesmo depois de parte do estoque ter sido vendida. A correção é recusada se deixaria o estoque negativo.
- Quantidade é sempre um número inteiro a partir de 1; valor quebrado é recusado, não arredondado.
- Excluir uma entrada ou uma saída desfaz o efeito dela no estoque.
- Excluir um produto é uma exclusão lógica: ele sai da lista de produtos disponíveis e não aceita novas entradas ou saídas, mas o histórico de movimentações e o saldo são preservados.
- Ninguém exclui o próprio usuário, e o único administrador não pode virar usuário comum: o sistema sempre fica com pelo menos um ADMIN.
- Dados inválidos voltam como erro 400 com a mensagem em `{"erro": "..."}`.

## Tecnologias

- Java 17 e Spring Boot 3.5 (Web, Data JPA, Security, Validation)
- Hibernate / JPA com H2 (local) ou PostgreSQL (deploy); consultas JPQL com agregação no Painel
- Swagger / OpenAPI (springdoc)
- Frontend em HTML, CSS e JavaScript (consumindo a API REST)
- API externa: [ViaCEP](https://viacep.com.br)
- Testes: JUnit 5, Mockito, MockMvc e `@DataJpaTest` (unitários, de consulta e de integração)
- Docker (deploy)

## Como executar

Pré-requisitos: Java 17+ e Maven.

```bash
cd controle-estoque
mvn spring-boot:run
```

Acesse http://localhost:8080 (ou a versão publicada: https://controle-estoquep2cannarozzo.onrender.com) e entre com:

| Login | Senha | Perfil |
|-------|-------|--------|
| admin | admin123 | ADMIN (acesso total) |
| usuario | user123 | USER (não exclui registros e não gerencia usuários) |

Rodar os testes:

```bash
mvn test
```

Por padrão o banco é H2 em arquivo (`./data`). Para usar PostgreSQL, defina as variáveis de ambiente:

```
DB_URL=jdbc:postgresql://host:5432/estoque
DB_USER=usuario
DB_PASSWORD=senha
```

## Links

- Deploy: https://controle-estoquep2cannarozzo.onrender.com
- Swagger: https://controle-estoquep2cannarozzo.onrender.com/swagger-ui.html (requer login)

# Controle de Estoque

Sistema web para controle de estoque de um pequeno comércio: cadastro de produtos, categorias, fornecedores e clientes, com registro de **entradas** (compras) e **saídas** (vendas) que atualizam o estoque automaticamente. Uma saída nunca pode ser maior que a quantidade disponível.

Projeto Final — Desenvolvimento de Software II (DS-II).

## Integrantes

| Nome | GitHub |
|------|--------|
| Lucas Viana dos Santos | @usuario-github |
| Nome do integrante 2 | @usuario-github |
| Nome do integrante 3 | @usuario-github |
| Nome do integrante 4 | @usuario-github |

## Tecnologias

- Java 17 e Spring Boot 3.5 (Web, Data JPA, Security, Validation)
- Hibernate / JPA com H2 (local) ou PostgreSQL (deploy)
- Swagger / OpenAPI (springdoc)
- Frontend em HTML, CSS e JavaScript (consumindo a API REST)
- API externa: [ViaCEP](https://viacep.com.br)
- Testes: JUnit 5, Mockito e MockMvc
- Docker (deploy)

## Como executar

Pré-requisitos: Java 17+ e Maven.

```bash
mvn spring-boot:run
```

Acesse http://localhost:8080 e entre com:

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

- Deploy: https://SEU-APP.onrender.com
- Swagger: https://SEU-APP.onrender.com/swagger-ui.html (local: http://localhost:8080/swagger-ui.html, requer login)

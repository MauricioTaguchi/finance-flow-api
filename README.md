# FinanceFlow API

API REST desenvolvida com **Java e Spring Boot** para cadastro e consulta de transações financeiras.

O projeto demonstra a aplicação prática de padrões de projeto, organização de camadas e construção de endpoints REST para controle financeiro.

## Objetivo do projeto

O objetivo do FinanceFlow API é simular uma aplicação backend para gerenciamento de transações financeiras, permitindo cadastrar, listar e consultar movimentações.

Além das funcionalidades principais, o projeto foi desenvolvido para demonstrar o uso de **padrões de projeto GoF** em um contexto prático com Spring Boot.

## Funcionalidades

- Cadastro de transações financeiras
- Listagem de transações
- Busca de transação por ID
- Validação dos dados de entrada
- Persistência em banco H2
- Notificações simuladas por diferentes estratégias
- Aplicação de padrões de projeto
- Separação de responsabilidades em camadas

## Tecnologias utilizadas

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- Bean Validation
- H2 Database
- Maven

## Padrões de projeto aplicados

| Padrão | Aplicação no projeto |
|---|---|
| Strategy | Define diferentes formas de notificação, como e-mail, SMS e push |
| Factory | Escolhe dinamicamente a estratégia de notificação |
| Observer | Utiliza eventos do Spring após o cadastro de uma transação |
| Facade | Centraliza o fluxo de cadastro de transações |
| Repository | Abstrai a camada de persistência com Spring Data JPA |
| Dependency Injection | Gerencia componentes por meio do Spring Framework |

## Como executar o projeto

É necessário ter **Java 17 ou superior** instalado.

O Maven não precisa estar instalado separadamente, pois o projeto utiliza Maven Wrapper.

No terminal, dentro da pasta do projeto, execute:

```bash
.\mvnw.cmd spring-boot:run
```

A aplicação ficará disponível em:

```text
http://localhost:8080
```

## Endpoints

### Cadastrar uma transação

```text
POST /transactions
Content-Type: application/json
```

Exemplo de requisição:

```json
{
  "description": "Compra no supermercado",
  "value": 150.90,
  "category": "ALIMENTACAO",
  "notificationType": "EMAIL"
}
```

Tipos de notificação disponíveis:

```text
EMAIL
SMS
PUSH
```

### Listar transações

```text
GET /transactions
```

### Buscar transação por ID

```text
GET /transactions/{id}
```

## Console do banco H2

Acesse:

```text
http://localhost:8080/h2-console
```

Configuração:

```text
JDBC URL: jdbc:h2:mem:financeflow
User Name: sa
Password:
```

## Estrutura do projeto

```text
finance-flow-api/
├── src/
│   └── main/
│       └── java/
│           └── ...
├── pom.xml
├── mvnw.cmd
└── README.md
```

## O que este projeto demonstra

- Criação de APIs REST com Spring Boot
- Aplicação prática de Design Patterns
- Validação de dados com Bean Validation
- Persistência com Spring Data JPA
- Uso de banco H2 para ambiente local
- Organização de endpoints e regras de negócio
- Estruturação de projeto backend para portfólio

## Melhorias futuras

- Adicionar autenticação com JWT
- Criar documentação com Swagger/OpenAPI
- Implementar testes unitários
- Adicionar banco PostgreSQL
- Criar filtros por categoria e período
- Implementar atualização e exclusão de transações
- Adicionar deploy em nuvem

## Autor

Maurício Ryo Toita Taguchi  
GitHub: [MauricioTaguchi](https://github.com/MauricioTaguchi)

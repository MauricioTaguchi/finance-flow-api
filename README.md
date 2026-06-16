# FinanceFlow API

API REST desenvolvida em Java com Spring Boot para cadastro e consulta de transações financeiras.

O projeto demonstra, na prática, padrões de projeto clássicos do GoF e abstrações do Spring Framework.

## Padrões aplicados

- Strategy: diferentes formas de notificação.
- Factory: escolha da estratégia de notificação.
- Observer: eventos do Spring após o cadastro de uma transação.
- Facade: centralização do fluxo de cadastro.
- Repository: persistência com Spring Data JPA.
- Injeção de dependências: gerenciamento de componentes pelo Spring.

## Tecnologias

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- Bean Validation
- H2 Database
- Maven

## Como executar

É necessário ter o Java 17 ou superior instalado. O Maven não precisa estar instalado, pois o projeto contém o Maven Wrapper.

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

```http
POST /transactions
Content-Type: application/json
```

Exemplo:

```json
{
  "description": "Compra no supermercado",
  "value": 150.90,
  "category": "ALIMENTACAO",
  "notificationType": "EMAIL"
}
```

Tipos de notificação disponíveis:

- EMAIL
- SMS
- PUSH

### Listar transações

```http
GET /transactions
```

### Buscar transação por ID

```http
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

## Como enviar ao GitHub

Crie um repositório vazio no GitHub e execute:

```bash
git init
git branch -M main
git add .
git commit -m "Adiciona projeto FinanceFlow com Design Patterns"
git remote add origin https://github.com/SEU-USUARIO/finance-flow-api.git
git push -u origin main
```

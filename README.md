# FinanceFlow API — Design Patterns Lab

API REST em Java 17/Spring Boot criada para demonstrar padrões GoF em um fluxo financeiro pequeno, testável e fácil de discutir em entrevista técnica.

[![CI](https://github.com/MauricioTaguchi/finance-flow-api/actions/workflows/ci.yml/badge.svg)](https://github.com/MauricioTaguchi/finance-flow-api/actions/workflows/ci.yml)

## O que o projeto demonstra

| Padrão | Aplicação |
|---|---|
| Strategy | canais Email, SMS e Push implementam o mesmo contrato |
| Factory | seleciona o canal dinamicamente e valida tipos inválidos |
| Observer | evento desacopla o cadastro da notificação |
| Facade | concentra a orquestração exposta ao controller |
| Repository | Spring Data JPA abstrai a persistência |

O listener executa somente **depois do commit**, evitando notificar uma transação revertida. As entidades também protegem invariantes de valor, descrição e categoria, independentemente da validação HTTP.

[Veja o diagrama e as decisões](docs/ARCHITECTURE.md).

## Executar

```powershell
.\mvnw.cmd spring-boot:run
```

API: `http://localhost:8080` · Health: `http://localhost:8080/actuator/health` · H2: `http://localhost:8080/h2-console`

## Exemplo

```powershell
curl.exe -X POST http://localhost:8080/transactions `
  -H "Content-Type: application/json" `
  -d '{"description":"Mercado","value":150.90,"category":"ALIMENTACAO","notificationType":"EMAIL"}'
```

| Método | Endpoint | Resultado |
|---|---|---|
| POST | `/transactions` | cria e publica evento |
| GET | `/transactions` | lista transações |
| GET | `/transactions/{id}` | busca por identificador |

## Qualidade

```powershell
.\mvnw.cmd verify
```

A suíte cobre regras de domínio, seleção da Strategy e contexto Spring. O pipeline gera relatório JaCoCo e o Dependabot acompanha Maven e GitHub Actions.

## Escopo consciente

As notificações são adapters simulados por log; isso mantém o laboratório autocontido. Em produção, implementações reais fariam I/O assíncrono com retentativa, idempotência e dead-letter queue. Autenticação e PostgreSQL também seriam responsabilidades da camada de implantação, não dos padrões demonstrados aqui.

## Autor

**Mauricio Taguchi** · [LinkedIn](https://www.linkedin.com/in/mauriciotaguchi/) · [GitHub](https://github.com/MauricioTaguchi)

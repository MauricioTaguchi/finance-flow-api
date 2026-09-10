# FinanceFlow API — Design Patterns Lab

API REST em Java 17/Spring Boot que demonstra padrões GoF em um fluxo financeiro com autenticação por token opaco, contas persistidas e isolamento das transações por proprietário.

[![CI](https://github.com/MauricioTaguchi/finance-flow-api/actions/workflows/ci.yml/badge.svg)](https://github.com/MauricioTaguchi/finance-flow-api/actions/workflows/ci.yml)

## O que o projeto demonstra

| Padrão | Aplicação |
|---|---|
| Strategy | canais Email, SMS e Push implementam o mesmo contrato |
| Factory | seleciona o canal dinamicamente e valida tipos inválidos |
| Observer | evento desacopla o cadastro da notificação |
| Facade | concentra a orquestração exposta ao controller |
| Repository | Spring Data JPA abstrai a persistência e as consultas por proprietário |

O listener executa somente **depois do commit**, evitando notificar uma transação revertida. As entidades também protegem invariantes de valor, descrição e categoria, independentemente da validação HTTP.

O servidor obtém o proprietário da identidade autenticada. Cada usuário, inclusive um administrador, cria e consulta apenas suas próprias transações. O papel `ADMIN` permite gerenciar credenciais e contas; ele não libera a leitura global de dados financeiros.

[Arquitetura e decisões](docs/ARCHITECTURE.md) · [Configuração de acesso e migração](docs/ACCESS_CONTROL.md)

## Executar

Requisitos: JDK 17 e PowerShell. Na primeira inicialização de um banco novo, o bootstrap exige um token forte de 32 bytes aleatórios, codificado em Base64 URL-safe sem padding. **Não há credencial padrão.**

Este exemplo gera o segredo somente na sessão e abre o servidor em outro processo. Não imprime o token nem o grava no repositório.

```powershell
$financeFlowBytes = New-Object byte[] 32
$financeFlowRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$financeFlowRandom.GetBytes($financeFlowBytes)
$financeFlowRandom.Dispose()
$financeFlowAdminToken = [Convert]::ToBase64String($financeFlowBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
$env:APP_BOOTSTRAP_ADMIN_TOKEN = $financeFlowAdminToken
$env:APP_BOOTSTRAP_ADMIN_USERNAME = 'bootstrap-admin'

Start-Process -FilePath '.\mvnw.cmd' -ArgumentList 'spring-boot:run'
Remove-Item Env:APP_BOOTSTRAP_ADMIN_TOKEN
Remove-Item Env:APP_BOOTSTRAP_ADMIN_USERNAME
```

Aguarde a inicialização da API em `http://localhost:8080`. O processo filho herda as variáveis antes de sua remoção na sessão chamadora. O health público é `http://localhost:8080/actuator/health`; os endpoints financeiros exigem autenticação. O console web H2 está desativado.

O padrão H2 em memória perde dados e principais ao encerrar o processo: cada nova base exige bootstrap. Para persistência local, configure `SPRING_DATASOURCE_URL=jdbc:h2:file:./data/financeflow` antes de iniciar. Bases antigas exigem revisão antes da migração; veja [o guia de acesso](docs/ACCESS_CONTROL.md).

O endereço de escuta padrão é `127.0.0.1`. Acesso remoto exige configuração explícita de `SERVER_ADDRESS` e proteção HTTPS/rede descrita no guia.

## Exemplo autenticado

No mesmo PowerShell, provisione uma conta `USER` com a credencial administrativa. Guarde o token devolvido em um gerenciador de segredos: ele é apresentado somente na criação ou rotação.

```powershell
$financeFlowAdminHeaders = @{ Authorization = "Bearer $financeFlowAdminToken" }
$financeFlowAccount = Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/admin/principals' `
  -Headers $financeFlowAdminHeaders `
  -ContentType 'application/json' `
  -Body '{"username":"mauricio-local","role":"USER"}'

$financeFlowUserToken = $financeFlowAccount.token
$financeFlowUserHeaders = @{ Authorization = "Bearer $financeFlowUserToken" }

Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/transactions' `
  -Headers $financeFlowUserHeaders `
  -ContentType 'application/json' `
  -Body '{"description":"Mercado","value":150.90,"category":"ALIMENTACAO","notificationType":"EMAIL"}'

Invoke-RestMethod `
  -Method Get `
  -Uri 'http://localhost:8080/transactions' `
  -Headers $financeFlowUserHeaders
```

Não imprima `$financeFlowAccount`, pois ele contém o segredo recém-criado. Os exemplos equivalentes com `curl` estão no [guia de acesso](docs/ACCESS_CONTROL.md). Use HTTPS fora do loopback.

| Método | Endpoint | Acesso e resultado |
|---|---|---|
| POST | `/transactions` | `USER` ou `ADMIN`: cria transação para o próprio principal e publica evento |
| GET | `/transactions` | `USER` ou `ADMIN`: lista apenas as próprias transações |
| GET | `/transactions/{id}` | `USER` ou `ADMIN`: busca apenas dentro do próprio escopo |
| POST | `/admin/principals` | `ADMIN`: cria conta e entrega seu token uma única vez |
| GET | `/admin/principals` | `ADMIN`: lista resumos sem tokens ou hashes |
| POST | `/admin/principals/{id}/rotate-token` | `ADMIN`: substitui o token e reativa a conta |
| POST | `/admin/principals/{id}/revoke` | `ADMIN`: desativa a conta e revoga sua credencial |

Rotação invalida o token anterior. Não é permitido revogar a própria conta durante a chamada nem eliminar o último administrador ativo com credencial válida. A reativação de uma conta revogada exige outro administrador apto.

## Qualidade

```powershell
.\mvnw.cmd verify
```

A suíte verifica regras de domínio, seleção da Strategy e comportamento da aplicação. Consulte os testes do repositório para a cobertura atual de autenticação e isolamento. O pipeline gera relatório JaCoCo e o Dependabot acompanha Maven e GitHub Actions.

## Persistência e evolução de esquema

Flyway controla o esquema: `V1` representa a estrutura legada de transações e `V2` adiciona principais e vínculo de proprietário. O Hibernate usa `ddl-auto=validate` e o baseline automático fica desativado.

Um banco legado populado não é adotado automaticamente. Antes de qualquer migração, o operador deve executar inspeção somente leitura, produzir e verificar backup, comparar o esquema e, apenas após validar a compatibilidade, fazer baseline explícito na versão `1`. **Transações antigas com proprietário nulo ficam em quarentena e não são atribuídas ao administrador de bootstrap.** O procedimento está em [ACCESS_CONTROL.md](docs/ACCESS_CONTROL.md).

## Escopo consciente

As notificações continuam sendo adapters simulados por log. A autenticação não altera esse comportamento nem remove informações que esses logs já registravam. Em produção, canais reais exigiriam I/O assíncrono com retentativa, idempotência e dead-letter queue.

Esta implementação trata controle de acesso e propriedade dos registros. Avisos de finalidade, política de privacidade, registro de ciência ou consentimento, atendimento ao titular, exportação/eliminação e inventário de destinatários exigem trabalho próprio e não estão implementados por este controle de acesso.

## Autor

**Mauricio Taguchi** · [LinkedIn](https://www.linkedin.com/in/mauriciotaguchi/) · [GitHub](https://github.com/MauricioTaguchi)

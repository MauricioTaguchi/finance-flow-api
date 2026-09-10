# Controle de acesso e operação

## Modelo de identidade

| Campo persistido | Função |
|---|---|
| `id` | UUID estável do principal; referência de propriedade das transações |
| `username` | identificador de 3 a 64 caracteres; primeiro caractere alfanumérico minúsculo, seguido por letras minúsculas, números, ponto, hífen ou sublinhado |
| `role` | `USER` ou `ADMIN` |
| `active` | permite suspender o acesso do principal |
| `token_hash` | SHA-256 do token, com 64 caracteres hexadecimais; nulo após revogação |
| `created_at` | instante de criação da identidade |

A credencial é um token Bearer opaco de 32 bytes aleatórios, representado por 43 caracteres Base64 URL-safe sem `=`. Não é um JWT. Quem obtiver esse segredo poderá agir como a identidade correspondente; armazene-o em um gerenciador de segredos e use HTTPS fora do loopback.

O servidor consulta o hash e o estado ativo para autenticar cada requisição. A listagem de principais omite tanto o token original quanto seu hash. Criação e rotação respondem com `principal` (campos `id`, `username`, `role`, `active` e `createdAt`) e `token`. O novo segredo é entregue somente naquela resposta. O nome de usuário não é senha nem comprovação de identidade civil.

## Bootstrap de um banco novo

| Variável | Uso |
|---|---|
| `APP_BOOTSTRAP_ADMIN_TOKEN` | obrigatória em banco sem principais; token forte, sem padrão embutido |
| `APP_BOOTSTRAP_ADMIN_USERNAME` | nome da primeira conta; padrão `bootstrap-admin` |
| `SPRING_DATASOURCE_URL` | seleção explícita da base pelo operador, quando necessário |
| `SERVER_ADDRESS` | endereço de escuta; padrão `127.0.0.1`; altere apenas para uma implantação com proteção de rede e HTTPS |

No PowerShell, gere o token somente em memória:

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

Execute na raiz do repositório, com JDK 17 disponível. O processo filho recebe as variáveis antes de sua remoção na sessão chamadora. Aguarde a inicialização antes de enviar requisições. O console H2 não fica exposto; `/actuator/health` permanece público para monitoramento, sem liberar as rotas financeiras.

O servidor escuta somente em `127.0.0.1` por padrão. Para acesso remoto, configure conscientemente `SERVER_ADDRESS`, firewall e um proxy HTTPS; não exponha tokens Bearer por HTTP em uma rede pública.

O bootstrap não é uma rotina de redefinição de senha: mudar a variável não troca o token de um principal já persistido. Após a criação inicial em uma base persistente, retire a variável de bootstrap da configuração de implantação e conserve a credencial em armazenamento apropriado. Nunca adicione tokens ao Git, a um arquivo de exemplo ou aos logs.

O banco H2 em memória é o padrão. Dados e principais são perdidos ao encerrar o processo; cada nova instância desse banco exige novo bootstrap. Para persistência local, o operador pode definir antes da primeira inicialização:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:h2:file:./data/financeflow'
```

Uma base em arquivo também exige proteção dos arquivos, backup e restauração verificada. Não alterne entre URLs de banco supondo que as mesmas contas e tokens estarão presentes.

## Provisionar um usuário e acessar transações

Use a credencial administrativa apenas para a administração. Provisione uma conta `USER` antes de registrar os dados do exemplo:

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

Não imprima `$financeFlowAccount`: sua resposta inclui o segredo recém-criado. Guarde a credencial antes de fechar a sessão. Não existe endpoint que recupere o token original depois disso; um administrador autorizado deve rotacioná-lo.

Equivalente HTTP com valores simbólicos:

```bash
curl --request POST 'http://localhost:8080/admin/principals' \
  --header 'Authorization: Bearer <ADMIN_TOKEN>' \
  --header 'Content-Type: application/json' \
  --data '{"username":"mauricio-local","role":"USER"}'

curl --request POST 'http://localhost:8080/transactions' \
  --header 'Authorization: Bearer <USER_TOKEN>' \
  --header 'Content-Type: application/json' \
  --data '{"description":"Mercado","value":150.90,"category":"ALIMENTACAO","notificationType":"EMAIL"}'

curl 'http://localhost:8080/transactions/<TRANSACTION_ID>' \
  --header 'Authorization: Bearer <USER_TOKEN>'
```

`<ADMIN_TOKEN>`, `<USER_TOKEN>` e `<TRANSACTION_ID>` são marcadores de documentação, não credenciais utilizáveis. Não cole segredos reais em comandos de um terminal compartilhado: argumentos e histórico podem expô-los. Os exemplos nativos de PowerShell acima enviam a credencial pela biblioteca HTTP do processo.

## Rotação, revogação e administração

| Operação | Requisição | Efeito |
|---|---|---|
| Criar | `POST /admin/principals` com `{"username":"nome","role":"USER"}` ou `ADMIN` | cria UUID, papel e credencial |
| Listar | `GET /admin/principals` | devolve resumos sem segredo e sem hash |
| Rotacionar | `POST /admin/principals/{id}/rotate-token` | preserva UUID e propriedade, invalida token antigo, entrega novo segredo e reativa a conta |
| Revogar | `POST /admin/principals/{id}/revoke` | desativa a conta e remove a credencial utilizável |

Todas essas operações exigem o papel `ADMIN`. Não há `PATCH` de atividade nem acesso financeiro global implícito.

```bash
curl --request POST 'http://localhost:8080/admin/principals/<PRINCIPAL_UUID>/rotate-token' \
  --header 'Authorization: Bearer <ADMIN_TOKEN>'

curl --request POST 'http://localhost:8080/admin/principals/<PRINCIPAL_UUID>/revoke' \
  --header 'Authorization: Bearer <ADMIN_TOKEN>'
```

A revogação do próprio chamador é bloqueada. A aplicação também protege o último administrador ativo com credencial válida. Para retirar uma conta administrativa antiga de operação, provisione outro administrador, confirme seu acesso e execute a revogação usando essa segunda identidade. Uma conta revogada só pode ser reativada pela rotação executada por outro administrador utilizável.

Rotação e revogação afetam autenticações posteriores; não devem ser tratadas como cancelamento de uma transação que já estava em execução. Elas não apagam transações nem alteram seu proprietário. A inicialização falha quando uma base existente não possui administrador ativo com credencial; a variável de bootstrap não sobrescreve automaticamente as identidades para corrigir esse estado.

## Migração e dados legados

O esquema é controlado pelo Flyway:

| Migração | Responsabilidade |
|---|---|
| `V1` | estrutura de transações correspondente ao modelo legado |
| `V2` | tabela `auth_principals` e referência de proprietário das transações |

`spring.jpa.hibernate.ddl-auto=validate` impede atualização implícita do esquema pelo Hibernate. `spring.flyway.baseline-on-migrate=false` impede assumir automaticamente que uma base preexistente corresponde à versão esperada.

Para banco novo e vazio, a inicialização executa as migrações normalmente. Para base legada populada, pare antes de tentar forçar a inicialização:

1. Identifique a URL, o usuário do banco e a versão da aplicação que criou o esquema. Faça a inspeção inicial somente leitura.
2. Interrompa gravadores e produza um backup consistente. Verifique a restauração em ambiente isolado; conserve uma cópia imutável do original.
3. Compare tabelas, colunas, tipos, constraints, sequência de IDs e dados com o script `V1` entregue nesta versão. Confirme que não existe histórico Flyway incompatível.
4. Se a base corresponder exatamente ao estado representado por `V1`, um operador autorizado pode executar **baseline explícito na versão `1`** usando uma instalação Flyway compatível e as credenciais do banco. O baseline registra o estado inicial; ele não reconstrói a tabela existente nem confirma que seus dados estão corretos.
5. Teste a aplicação de `V2` e a inicialização na cópia restaurada. Verifique contagem de transações, integridade das referências e ausência de exposição de registros sem proprietário.
6. Somente após essa validação, repita o procedimento aprovado na base original com sua janela de manutenção e plano de retorno. Para o primeiro bootstrap dos principais, injete o token forte pelo mecanismo de segredos da implantação.

Exemplo de **operação manual**, somente após as verificações acima:

```text
flyway -baselineVersion=1 baseline
```

Configure URL, usuário e credencial de modo seguro na instalação Flyway usada pelo operador. Esse comando não é executado automaticamente pela aplicação. Não habilite `baseline-on-migrate` nem use `ddl-auto=update` para contornar uma divergência de esquema. Não rode `clean` em uma base com dados a preservar.

`owner_id` pode permanecer nulo nos registros legados. Esses dados ficam em **quarentena lógica**: continuam armazenados, mas as consultas autenticadas não os retornam. Isso não é anonimização, eliminação nem comprovação de quem é o titular. Não há atribuição automática ao administrador, distribuição por nome ou reivindicação de propriedade pela API. Qualquer futura reatribuição exige comprovação externa, revisão específica e procedimento próprio; ela está fora desta migração.

## Limites de privacidade

Autenticação e consultas por proprietário reduzem exposição entre contas, mas não implementam o restante de um programa de privacidade. Os logs de notificação simulada permanecem como antes. Avisos de finalidade, política versionada, registro de ciência/consentimento, exportação e eliminação por titular, retenção de logs/backups e inventário de destinatários não são entregues por este incremento.

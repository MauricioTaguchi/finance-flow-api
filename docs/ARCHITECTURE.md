# Arquitetura e padrões

```mermaid
sequenceDiagram
    participant C as Controller
    participant F as Facade
    participant S as Service
    participant R as Repository
    participant E as Event Publisher
    participant N as Notification Strategy
    C->>F: register(request)
    F->>S: create(...)
    S->>R: save(transaction)
    R-->>S: persisted transaction
    S->>E: publish event
    S-->>C: HTTP 201
    E->>N: notify after commit
```

| Padrão | Papel e motivo |
|---|---|
| Facade | oferece uma entrada simples para o fluxo de cadastro |
| Strategy | isola os canais de notificação e permite novas implementações |
| Factory | seleciona a Strategy sem condicionais no consumidor |
| Observer | desacopla persistência e notificação por evento de domínio |
| Repository | abstrai armazenamento e consultas JPA |

A notificação é disparada `AFTER_COMMIT`: uma transação que sofreu rollback não gera efeito externo enganoso.

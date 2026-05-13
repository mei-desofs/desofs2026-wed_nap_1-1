# DESOFS App2 (Java)

Projeto Java Spring Boot que replica a API do projecto original em JavaScript, adaptada para uma loja de venda de filmes (movie).

## Notas Rápidas
- **UI omitida**: Use Postman ou curl para testar (`http://localhost:8081`).
- **Autenticação**: Auth0 + JWT (OAuth2 Resource Server). Configure `AUTH0_ISSUER_URI`.
- **Base de dados**: H2 em memória por omissão; mude em `application.yml` para MySQL se preferir.
- **Seed data**: Filmes e usuários pré-carregados via Flyway.

## Executar Localmente

```bash
cd App2
mvn clean install
mvn spring-boot:run
```

A aplicação inicia em `http://localhost:8081`.

## Endpoints Disponíveis

### Movies
- `GET /api/movies` - Listar todos os filmes
- `GET /api/movies/{id}` - Obter filme por ID
- `POST /api/movies` - Criar novo filme

### Users
- `GET /api/users` - Listar todos os usuários
- `GET /api/users/{id}` - Obter usuário por ID
- `POST /api/users` - Criar novo usuário
- `PUT /api/users/{id}` - Atualizar usuário

### Orders
- `GET /api/orders` - Listar todos os pedidos
- `GET /api/orders/{id}` - Obter pedido por ID
- `POST /api/orders` - Criar novo pedido (checkout)

### Refunds
- `GET /api/refunds` - Listar todos os pedidos de reembolso
- `GET /api/refunds/{id}` - Obter pedido de reembolso por ID
- `POST /api/refunds` - Criar novo pedido de reembolso
- `PUT /api/refunds/{id}/approve` - Aprovar reembolso
- `PUT /api/refunds/{id}/reject` - Rejeitar reembolso
- `PUT /api/refunds/{id}/complete` - Completar reembolso

### Audit Logs
- `GET /api/audit-logs` - Listar todos os registos de auditoria

## Exemplos com curl

**Listar filmes:**
```bash
curl -X GET http://localhost:8081/api/movies
```

**Criar usuário:**
```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "name": "John Doe"}'
```

**Criar pedido:**
```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      {"movieId": 1, "quantity": 2},
      {"movieId": 3, "quantity": 1}
    ]
  }'
```

**Criar pedido de reembolso:**
```bash
curl -X POST http://localhost:8081/api/refunds \
  -H "Content-Type: application/json" \
  -d '{"orderId": 1, "userId": 1, "amount": 29.98, "reason": "Customer request"}'
```

**Aprovar reembolso:**
```bash
curl -X PUT http://localhost:8081/api/refunds/1/approve
```

**Listar registos de auditoria:**
```bash
curl -X GET http://localhost:8081/api/audit-logs
```

## Configuração Auth0

Configure a variável de ambiente antes de iniciar:

```bash
# PowerShell
$env:AUTH0_ISSUER_URI="https://YOUR_AUTH0_DOMAIN/"
mvn spring-boot:run

# Bash
export AUTH0_ISSUER_URI="https://YOUR_AUTH0_DOMAIN/"
mvn spring-boot:run
```

## Postman Collection

Importe `POSTMAN_COLLECTION.json` no Postman para testar todos os endpoints com exemplos pré-configurados.

## Estrutura do Projecto

```
App2/
├── pom.xml
├── Dockerfile
├── README.md
├── POSTMAN_COLLECTION.json
├── src/main/java/com/example/desofs/
│   ├── Application.java
│   ├── config/
│   │   └── SecurityConfig.java
│   ├── domain/
│   │   ├── entities/
│   │   │   ├── Movie.java
│   │   │   ├── User.java
│   │   │   ├── Order.java
│   │   │   ├── OrderItem.java
│   │   │   ├── RefundRequest.java
│   │   │   ├── MfaToken.java
│   │   │   └── AuditLog.java
│   │   └── repositories/
│   │       ├── MovieRepository.java
│   │       ├── UserRepository.java
│   │       ├── OrderRepository.java
│   │       ├── RefundRequestRepository.java
│   │       ├── MfaTokenRepository.java
│   │       └── AuditLogRepository.java
│   ├── application/
│   │   ├── services/
│   │   │   ├── MovieService.java
│   │   │   ├── UserService.java
│   │   │   ├── OrderService.java
│   │   │   ├── RefundService.java
│   │   │   ├── MfaTokenService.java
│   │   │   └── AuditLogService.java
│   │   └── dtos/
│   │       ├── MovieDTO.java
│   │       ├── UserDTO.java
│   │       ├── OrderDTO.java
│   │       ├── OrderItemDTO.java
│   │       ├── CreateOrderRequest.java
│   │       ├── RefundRequestDTO.java
│   │       └── CreateRefundRequest.java
│   └── interfaces/
│       └── controllers/http/
│           ├── MovieController.java
│           ├── UserController.java
│           ├── OrderController.java
│           ├── RefundController.java
│           └── AuditLogController.java
└── src/main/resources/
    ├── application.yml
    └── db/migration/
        ├── V1__create_tables.sql
        ├── V2__insert_seed_data.sql
        ├── V3__add_account_lockout_and_timestamps.sql
        ├── V4__create_mfa_tokens.sql
        ├── V5__create_refund_requests.sql
        └── V6__create_audit_logs.sql
```

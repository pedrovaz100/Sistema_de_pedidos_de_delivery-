# Projeto Diamante 02 - Delivery System

Sistema de pedidos de delivery com arquitetura de microsserviços (Java Advanced - segundo semestre).

## Integrantes

| Nome | RM |
|------|----|
| Pedro Vaz | 566551 |
| João Victor Luiz Oliveira Resende | 565139 |

## Objetivo

Cenário: sexta-feira, 20h, promoção relâmpago. O sistema precisa lidar com:

| Problema | Solução aplicada |
|----------|------------------|
| Estoque limitado (10 unidades) | Lock pessimista (`PESSIMISTIC_WRITE`) + `@Transactional` no `POST /orders` |
| Pagamento instável, 2 instâncias | Eureka + `@LoadBalanced RestTemplate` + `@Retryable` com backoff exponencial e jitter |
| Avaliações em massa | RabbitMQ + buffer `ConcurrentHashMap` + flush agendado a cada 5 s (backpressure) |
| Dúvidas sobre o cardápio | Assistente com Spring AI (`ChatClient`) usando o cardápio do banco no prompt |
| Bônus | Rate limit de 20 req/s no `POST /orders` (429) |

## Arquitetura

```text
                    App React Native (professor)
                              |
                              | REST (:8080 e :8083)
                              v
   +----------------+  registro  +-------------------+  AMQP   +---------------------------+
   | eureka-server  |<-----------|   order-service   |--------->| RabbitMQ (Docker)         |
   |     :8761      |            |       :8080       |          | delivery.exchange (topic) |
   +----------------+            +---+-----------+---+          |   --reviews.new-->        |
          ^                          |           |              | reviews.queue (durable)   |
          | registro                 | LB+retry  | ChatClient   +-------------+-------------+
          |                          v           v                            |
          |            +-----------------+   +-----------------+               v
          +------------| payment-service |   | Modelo de IA    |     +------------------+
          |            |  :8081  / :8082 |   | OpenAI / LM     |     |  review-service  |
          |            +-----------------+   | Studio / Ollama |     |      :8083       |
          +--------------------------------------------------------->| buffer + flush 5s|
                                             +-----------------+     +------------------+
```

## Tecnologias

- Java 25, Gradle 9 (wrapper incluso)
- Spring Boot 4.0.8
- Spring Cloud 2025.1.3 (Netflix Eureka, LoadBalancer)
- Spring Framework 7 Resilience (`@Retryable`, `@EnableResilientMethods`)
- Spring Data JPA + H2 (em memória)
- Spring AMQP + RabbitMQ 4 (Docker)
- Spring AI 2.0.1 (provedor compatível com OpenAI: OpenAI, LM Studio ou Ollama)
- Lombok

## Estrutura do monorepo

```text
delivery-system/
├── eureka-server/      Service Discovery (:8761)
├── order-service/      Cardápio, pedidos, avaliações (producer) e assistente IA (:8080)
├── payment-service/    Pagamento instável, 2 instâncias (:8081 e :8082)
├── review-service/     Consumer das avaliações, buffer, ranking (:8083)
├── scripts/            Scripts de teste de concorrência
├── docker-compose.yml  RabbitMQ
├── settings.gradle
├── gradle.properties   Versões compartilhadas (Boot, Cloud, AI)
├── .env.example
└── README.md
```

Os quatro serviços são projetos Gradle independentes (cada um com seu `build.gradle`) agrupados pelo `settings.gradle` da raiz.
Os serviços Java **não** rodam dentro do Docker: o Docker Compose sobe somente o RabbitMQ, e os serviços são executados via Gradle (`bootRun`).

Nenhuma chamada entre serviços usa `localhost`: o `order-service` chama `http://PAYMENT-SERVICE/payments` e o Eureka resolve a instância.

## Pré-requisitos

- JDK 25
- Docker (para o RabbitMQ)
- (Opcional) chave da OpenAI ou LM Studio/Ollama para o assistente

## Como executar

Todos os comandos abaixo são executados na raiz `delivery-system/`, cada um em um terminal separado.
No Windows PowerShell use `.\gradlew.bat`; em Linux/macOS/Git Bash use `./gradlew`.

### 1. RabbitMQ

```bash
docker compose up
```

Painel de administração: http://localhost:15672 (usuário `guest`, senha `guest`).

### 2. Eureka

```bash
./gradlew :eureka-server:bootRun
```

Dashboard: http://localhost:8761

### 3. payment-service (instância 1, porta 8081)

```bash
./gradlew :payment-service:bootRun
```

### 4. payment-service (instância 2, porta 8082)

```bash
./gradlew :payment-service:bootRun --args='--server.port=8082'
```

Se preferir entrar na pasta do serviço, o resultado é o mesmo:

```bash
cd payment-service
../gradlew bootRun --args='--server.port=8082'
```

### 5. review-service

```bash
./gradlew :review-service:bootRun
```

### 6. order-service

```bash
./gradlew :order-service:bootRun
```

Aguarde alguns segundos até os serviços aparecerem no dashboard do Eureka (`ORDER-SERVICE`, `PAYMENT-SERVICE` com 2 instâncias e `REVIEW-SERVICE`).

Para rodar todos os testes automatizados:

```bash
./gradlew test
```

## Variáveis de ambiente

A chave de API **nunca** vai para o repositório. Duas formas de configurar (valem para o `order-service`):

1. Variáveis de ambiente do terminal:

   ```powershell
   $env:OPENAI_API_KEY = "sua-chave"
   .\gradlew.bat :order-service:bootRun
   ```

   ```bash
   export OPENAI_API_KEY="sua-chave"
   ./gradlew :order-service:bootRun
   ```

2. Arquivo `.env` na raiz (copie de `.env.example`; ele está no `.gitignore`). Valores sem aspas.

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `OPENAI_API_KEY` | `not-configured` | Chave da OpenAI (qualquer texto não vazio para LM Studio/Ollama) |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | URL do provedor. LM Studio: `http://localhost:1234/v1`. Ollama: `http://localhost:11434/v1` |
| `OPENAI_MODEL` | `gpt-4o-mini` | Nome do modelo (para modelos locais, o identificador exibido no LM Studio/Ollama) |
| `RATE_LIMIT_ORDERS_ENABLED` | `true` | Liga/desliga o rate limit do `POST /orders` |
| `RATE_LIMIT_ORDERS_PERMITS_PER_SECOND` | `20` | Limite de requisições por segundo no `POST /orders` |
| `SPRING_RABBITMQ_HOST` | padrão do Spring AMQP | Host do RabbitMQ, se não for a própria máquina |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | padrão do Eureka | URL do Eureka, se não for a própria máquina |

## Endpoints

### order-service (:8080)

| Método | Rota | Corpo | Respostas |
|--------|------|-------|-----------|
| GET | `/dishes` | - | 200 lista de Dish |
| GET | `/dishes/{id}` | - | 200 Dish, 404 |
| POST | `/orders` | `{"dishId": 1, "quantity": 2}` | 201 Order, 400, 404, 409, 429, 502 |
| GET | `/orders/{id}` | - | 200 Order, 404 |
| POST | `/reviews` | `{"dishId": 1, "rating": 5, "comment": "Great"}` | 202 sem corpo, 400, 404 |
| POST | `/assistant` | `{"question": "Tem opção vegana?"}` | 200 `{"answer": "..."}` |

### review-service (:8083)

| Método | Rota | Resposta |
|--------|------|----------|
| GET | `/reviews/ranking` | 200 lista ordenada pela média |

### payment-service (:8081 e :8082, uso interno)

| Método | Rota | Corpo | Respostas |
|--------|------|-------|-----------|
| POST | `/payments` | `{"amount": 79.80}` | 200 `{"status": "APPROVED", "instance": 8081}`, 500 (falha simulada em ~50%) |

### Formato dos erros

```json
{ "error": "Dish out of stock" }
```

## Exemplos de requisições e respostas

```bash
curl http://localhost:8080/dishes
```

```json
[
  {
    "id": 1,
    "name": "House Burger",
    "description": "Brioche bun, beef patty, cheddar and caramelized onions",
    "price": 39.90,
    "stock": 10
  }
]
```

```bash
curl -X POST http://localhost:8080/orders -H "Content-Type: application/json" -d '{"dishId": 1, "quantity": 2}'
```

```json
{
  "id": 1,
  "dishId": 1,
  "quantity": 2,
  "totalPrice": 79.80,
  "status": "CONFIRMED",
  "createdAt": "2026-10-08T20:15:00"
}
```

Erros do pedido:

| Situação | Status | Corpo |
|----------|--------|-------|
| `quantity` menor que 1 | 400 | `{"error": "quantity must be at least 1"}` |
| Prato inexistente | 404 | `{"error": "Dish not found"}` |
| Sem estoque | 409 | `{"error": "Dish out of stock"}` |
| Excedeu 20 req/s | 429 | `{"error": "Too many requests, please try again shortly"}` |
| Pagamento falhou em todas as tentativas | 502 | `{"error": "Payment could not be processed"}` |

```bash
curl -X POST http://localhost:8080/reviews -H "Content-Type: application/json" -d '{"dishId": 1, "rating": 5, "comment": "Great"}'
```

Resposta: `202 Accepted` sem corpo.

```bash
curl http://localhost:8083/reviews/ranking
```

```json
[
  { "dishId": 1, "dishName": "House Burger", "average": 4.6, "count": 128 }
]
```

```bash
curl -X POST http://localhost:8080/assistant -H "Content-Type: application/json" -d '{"question": "Tem opção vegana?"}'
```

```json
{ "answer": "Temos o Vegan Buddha Bowl por R$ 36,50 e a Fresh Lemonade por R$ 12,00." }
```

## Como testar cada requisito

### Eureka

Abra http://localhost:8761 e confira `ORDER-SERVICE`, `REVIEW-SERVICE` e `PAYMENT-SERVICE` (duas instâncias: `payment-service:8081` e `payment-service:8082`).

### Concorrência (race condition)

O prato `House Burger` (id 1) começa com `stock = 10`. Dispare 50 pedidos simultâneos:

```powershell
.\scripts\concurrency-test.ps1
```

```bash
./scripts/concurrency-test.sh
```

Resultado esperado: exatamente 10 respostas `201` e `stock` final `0`. As demais são `409` ou `429`.

Com o rate limit ligado (padrão), apenas 20 das 50 requisições são admitidas no mesmo segundo; as outras recebem `429`. Para ver somente `201` e `409`, suba o `order-service` com o rate limit desligado:

```powershell
$env:RATE_LIMIT_ORDERS_ENABLED = "false"
.\gradlew.bat :order-service:bootRun
```

O estoque fica em memória: reinicie o `order-service` para voltar ao estoque 10.

Também existe um teste automatizado: `OrderConcurrencyTest` (`./gradlew :order-service:test`).

### Load balance e retry

1. Com as duas instâncias de pagamento no ar, faça vários pedidos (`POST /orders`).
2. No log do `order-service`, veja `Payment of ... approved by payment-service instance 8081` alternando com `8082`.
3. Falhas simuladas aparecem como `Payment attempt for ... failed: 500` e são repetidas automaticamente (backoff exponencial com jitter).
4. Nos logs de cada `payment-service`, a porta que atendeu é registrada (`Instance 8081 approved ...` / `Instance 8082 simulated a payment failure ...`).
5. Para provocar o `502`, pare as duas instâncias do `payment-service` e faça um pedido: a resposta será `502` e o estoque do prato permanece intacto (`GET /dishes/{id}`).

Configuração do retry (`order-service/src/main/resources/application.yml`):

| Propriedade | Valor | Significado |
|-------------|-------|-------------|
| `payment.retry.max-retries` | 5 | Tentativas extras após a primeira |
| `payment.retry.delay` | 200 ms | Espera inicial |
| `payment.retry.multiplier` | 2.0 | Backoff exponencial |
| `payment.retry.jitter` | 100 ms | Variação aleatória |
| `payment.retry.max-delay` | 2000 ms | Espera máxima |

Observação de projeto: o lock do prato é mantido durante a chamada de pagamento. Assim o estoque só é decrementado depois da aprovação, e uma falha definitiva do pagamento deixa o estoque exatamente como estava.

### RabbitMQ e backpressure

1. Envie avaliações:

   ```bash
   curl -X POST http://localhost:8080/reviews -H "Content-Type: application/json" -d '{"dishId": 1, "rating": 5, "comment": "Great"}'
   ```

2. No painel http://localhost:15672 confira a exchange `delivery.exchange` (topic), a fila durável `reviews.queue` e o binding com a routing key `reviews.new`.
3. O `review-service` acumula as avaliações em memória e grava no H2 a cada 5 segundos (log `Flushed review summaries for N dishes`).
4. Consulte `GET http://localhost:8083/reviews/ranking` logo após enviar (ainda vazio) e novamente após ~5 s (já com os dados).

Para simular uma carga de avaliações:

```bash
seq 1 300 | xargs -P 30 -I{} curl -s -o /dev/null -X POST http://localhost:8080/reviews -H "Content-Type: application/json" -d '{"dishId": 1, "rating": 4, "comment": "Load"}'
```

### Spring AI

Configure o provedor (veja "Variáveis de ambiente") e pergunte:

```bash
curl -X POST http://localhost:8080/assistant -H "Content-Type: application/json" -d '{"question": "Tem prato vegetariano até R$ 40?"}'
curl -X POST http://localhost:8080/assistant -H "Content-Type: application/json" -d '{"question": "Quem ganhou a Copa de 2002?"}'
```

A primeira é respondida com base no cardápio atual (nome, preço e estoque lidos do banco a cada pergunta). A segunda deve ser recusada educadamente. Se o provedor estiver indisponível, a API responde `503` com `{"error": "Assistant is currently unavailable"}`.

### Testes automatizados

```bash
./gradlew test
```

| Serviço | Cobertura |
|---------|-----------|
| order-service | Dishes, pedidos (válido, 400, 404, 409, 502, total), concorrência (50 requisições, 10 pedidos), retry do `PaymentClient`, reviews (rating válido/inválido, publicação), assistente (cardápio no prompt, recusa fora do tema), rate limit |
| payment-service | Aprovação, falha, porta da instância, taxa de ~50% |
| review-service | Buffer concorrente, flush, ranking (média, contagem, ordenação), conversão da mensagem JSON |

## Observações

- Os bancos H2 são em memória: os dados voltam ao estado inicial a cada reinicialização.
- O `DataLoader` cadastra 7 pratos; o `House Burger` (id 1) é o prato da promoção, com estoque 10.
- Os serviços se registram no Eureka usando o IP da máquina (`prefer-ip-address`).
- CORS está liberado nos serviços acessados pelo app para permitir testes via Expo Web.
- O app React Native usa as portas 8080 (order-service) e 8083 (review-service); informe o IP da máquina nas configurações do app.

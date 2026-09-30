# Credit Card Service

Spring Boot 3.5 and Spring Data JPA implementation of the credit card service. The root Java package is `com.ofss`; supporting packages include `com.ofss.controller`, `com.ofss.dto`, `com.ofss.entity`, `com.ofss.repository`, `com.ofss.service`, and `com.ofss.exception`. Import the `credit-card-service` directory into Spring Tool Suite as **Existing Maven Projects**. The application starts on port `8082` and maps its JPA entity to Oracle's `CREDIT_CARD` table.

## Prerequisites

- Java 25 and Maven installed and configured in STS.
- Oracle Database with the `CREDIT_CARD` table from the project schema. `CARD_NUMBER` is the primary key; `CUSTOMER_ID`, `CARD_TYPE`, `CREDIT_LIMIT`, `AVAILABLE_CREDIT`, `OUTSTANDING_AMOUNT`, `EXPIRY_DATE`, and `CARD_STATUS` are required columns. The SQL from the earlier project exercise is compatible. If its `CUSTOMER_ID` foreign key exists, the referenced customer must already exist.
- Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the application environment. The default URL is `jdbc:oracle:thin:@localhost:1521/FREEPDB1`; replace it if the host, port, or service differs. Never commit passwords. Spring Data JPA handles persistence; the Oracle JDBC dependency is the database driver, and the code does not use JDBC directly.

Run with **Run As > Spring Boot App** in STS or use `mvn spring-boot:run`. `spring.jpa.hibernate.ddl-auto=validate` checks the existing table and does not create or delete tables.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/cards` | Issue a card with starting available credit equal to its limit |
| GET | `/api/cards/{cardNumber}` | Return card status and balances |
| GET | `/api/cards?customerId=1` | List all cards or only one customer's cards |
| PUT | `/api/cards/{cardNumber}` | Change type, credit limit, or expiry |
| PATCH | `/api/cards/{cardNumber}/block` | Block a card |
| PATCH | `/api/cards/{cardNumber}/unblock` | Unblock a card |
| POST | `/api/cards/{cardNumber}/purchase` | Reduce available credit and increase outstanding |
| POST | `/api/cards/{cardNumber}/payment` | Reduce outstanding and restore available credit |

Create a card (choose a unique card number and an existing customer ID):

```http
POST http://localhost:8082/api/cards
Content-Type: application/json

{"cardNumber":"1234567890123456","customerId":1,"cardType":"GOLD","creditLimit":100000.00,"expiryDate":"2029-12-31"}
```

Your requested GET response:

```http
GET http://localhost:8082/api/cards/1234567890123456
```

```json
{"cardNumber":"1234567890123456","cardStatus":"ACTIVE","availableCredit":100000.00,"outstandingAmount":0.00}
```

Your requested purchase:

```http
POST http://localhost:8082/api/cards/1234567890123456/purchase
Content-Type: application/json

{"amount":5000.00}
```

```json
{"success":true,"cardNumber":"1234567890123456","availableCredit":95000.00,"outstandingAmount":5000.00}
```

Invalid amounts and malformed requests return `400`, unknown cards return `404`, and blocked cards, insufficient credit, and other business conflicts return `409`. Each balance update uses a JPA transaction and a database row lock so simultaneous purchases on the same card cannot both spend the same available credit.

## Integration boundary

This service owns card status and balances. The wider project also requires successful and failed purchases and payments to be recorded in its transaction history. Coordinate with the Transaction service before treating this API as a complete financial workflow: this starter does **not** log transaction records or prevent replay of duplicate purchase requests. Customer existence is enforced by Oracle only if the existing schema has a customer foreign key; otherwise connect to the Customer service for that validation.

Run the included unit tests with `mvn test`.

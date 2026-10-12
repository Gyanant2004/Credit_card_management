# Transaction Service

Transaction Service is a separate Spring Boot application in the Credit Card Management project. It reads purchase records written by `purchase-service` from the shared Oracle `TRANSACTION` table, provides transaction reports, and processes bill payments through `credit-card-service`. It registers with Eureka as `TRANSACTION-SERVICE` and runs on port `8084`.

Purchases are created through `POST http://localhost:8083/api/purchases`. Payments are created through this service so that the payment and its outcome appear in transaction history.


## Before testing

1. Start Eureka on port `8761`, credit-card-service on `8082`, purchase-service on `8083`, and transaction-service on `8084`.
2. Check `http://localhost:8761` and confirm `TRANSACTION-SERVICE` and `CREDIT-CARD-SERVICE` are **UP**.
3. Configure transaction-service to use the same Oracle database and schema as purchase-service. Set its `DB_USER` and `DB_PASSWORD` in its own STS run configuration.
4. Use a real card number from your database. For payment tests, the card must be `ACTIVE` and its outstanding amount must be at least the amount you intend to pay.



## Seven endpoints and API tests

| No. | Method | URL | How to test | Expected result |
| --- | --- | --- | --- | --- |
| 1 | GET | `http://localhost:8084/api/transactions` | `200 OK`; an array of transactions, or `[]` when empty. |
| 2 | GET | `http://localhost:8084/api/transactions/1` | Existing ID: `200 OK` with one transaction. Missing ID: `404 Not Found`. |
| 3 | POST | `http://localhost:8084/api/transactions/payments` | `201 Created`; a `PAYMENT` transaction with `SUCCESS` status and `merchantId: null`. |
| 4 | GET | `http://localhost:8084/api/transactions/reports/purchases/today` | Compare `amount` with today's successful purchases in endpoint 1. | `200 OK`; today's successful purchase total, or `0.00`. **Report 11.** |
| 5 | GET | `http://localhost:8084/api/transactions/reports/payments/today` | Check before and after a successful payment. | `200 OK`; today's successful payment total. It increases by the payment amount. **Report 12.** |
| 6 | GET | `http://localhost:8084/api/transactions/reports/purchases/average` | Sum successful purchase amounts across all dates and divide by their count. | `200 OK`; average rounded to two decimals, or `0.00` if none. **Report 18.** |
| 7 | GET | `http://localhost:8084/api/transactions/reports/purchases/largest` | Compare the returned record with all successful purchases. | `200 OK` with the largest successful purchase, or `404 Not Found` if none. **Report 19.** |



Use this body for endpoint 3; replace the example number with an existing card:

```json
{
  "cardNumber": 1234567890123456,
  "amount": 100
}
```


# Product Catalog REST API

A minimal Spring Boot RESTful API for managing a product catalog. It exposes full
CRUD (Create, Read, Update, Delete) operations over a single `Product` entity backed by
an in-memory H2 database, so it runs with no external setup.

## Domain model

A `Product` has:

| Field   | Type      | Description           |
|---------|-----------|-----------------------|
| id      | Long      | Auto-generated ID     |
| name    | String    | Product name          |
| price   | BigDecimal| Product price         |

### Example payload

```json
{
  "id": 1,
  "name": "Laptop",
  "price": 999.99
}
```

## API endpoints

Base path: `/api/products`

| Method   | Endpoint            | Description               | Status codes      |
|----------|---------------------|----------------------------|-------------------|
| GET      | `/api/products`     | Get all products           | 200 OK            |
| GET      | `/api/products/{id}`| Get a single product       | 200 OK, 404 Not Found |
| POST     | `/api/products`     | Create a new product       | 200 OK            |
| PUT      | `/api/products/{id}`| Update an existing product | 200 OK, 404 Not Found |
| DELETE   | `/api/products/{id}`| Delete a product           | 204 No Content, 404 Not Found |

## Running the application

```bash
./mvnw spring-boot:run
```

Once started, the API is available at `http://localhost:8080/api/products`.

## Example requests & responses

### Get all products

```bash
curl -X GET http://localhost:8080/api/products
```

```json
[
  { "id": 1, "name": "Laptop", "price": 999.99 },
  { "id": 2, "name": "Mouse", "price": 19.99 }
]
```

### Get a product by ID

```bash
curl -X GET http://localhost:8080/api/products/1
```

```json
{ "id": 1, "name": "Laptop", "price": 999.99 }
```

If the product does not exist:

```http
HTTP/1.1 404 Not Found
```

### Create a product

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "price": 999.99}'
```

```json
{ "id": 1, "name": "Laptop", "price": 999.99 }
```

### Update a product

```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Updated Laptop", "price": 1299.99}'
```

```json
{ "id": 1, "name": "Updated Laptop", "price": 1299.99 }
```

### Delete a product

```bash
curl -X DELETE http://localhost:8080/api/products/1
```

```http
HTTP/1.1 204 No Content
```

## Running the tests

```bash
./mvnw test
```

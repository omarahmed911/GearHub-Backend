# GearHub API Documentation

## Authentication
### 1. Register User
- **Method**: `POST`
- **Endpoint**: `/api/auth/register`
- **Body**:
```json
{
  "username": "john_doe",
  "password": "password123",
  "role": "CUSTOMER" // TRADER, CUSTOMER
}
```

### 2. Login User
- **Method**: `POST`
- **Endpoint**: `/api/auth/login`
- **Body**:
```json
{
  "username": "john_doe",
  "password": "password123"
}
```

## Products
### 1. Get All Products
- **Method**: `GET`
- **Endpoint**: `/api/products`

### 2. Add New Product (TRADER only)
- **Method**: `POST`
- **Endpoint**: `/api/products`
- **Body**:
```json
{
  "name": "Brake Pads",
  "description": "High quality brake pads",
  "price": 45.99,
  "stockQuantity": 100,
  "category": "Brakes"
}
```

## Orders
### 1. Place Order (CUSTOMER only)
- **Method**: `POST`
- **Endpoint**: `/api/orders`
- **Body**:
```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

### 2. Update Order Status (TRADER only)
- **Method**: `PUT`
- **Endpoint**: `/api/orders/{id}/status`
- **Body**:
```json
{
  "status": "PROCESSING" // PENDING, PROCESSING, DELIVERED
}
```


# Postman API Collection & Automated Testing

## 1. Overview

E-ShoppingZone includes an enterprise-grade Postman collection and environment in the `postman/` directory:
- `postman/collections/E-ShoppingZone.postman_collection.json`
- `postman/environments/E-ShoppingZone-Local.postman_environment.json`

---

## 2. Test Execution Flow

The Postman collection is organized in sequential folders mirroring real customer and merchant journeys:
1. **01-Public Browsing**: Product catalog listing, category filtering, search.
2. **02-Authentication**: Customer registration, customer login, token capture into environment variable `{{accessToken}}`, refresh token cookie validation.
3. **03-Profile Operations**: Fetch profile, update profile.
4. **04-Cart Operations**: Add products to cart, update quantity, retrieve cart totals.
5. **05-Wallet Operations**: Check balance, deposit funds, view ledger transactions.
6. **06-Order Checkout**: Checkout with wallet, verify order status `CONFIRMED`, checkout with COD.
7. **07-Merchant Operations**: Merchant login, create product, update inventory, list merchant orders.
8. **08-Security Verification**: Attempt customer access on merchant endpoint (assert 403), expired token (assert 401), invalid credentials (assert 401).

---

## 3. Running with Newman CLI

Run headless tests across all microservices using Newman:
```bash
npx newman run postman/collections/E-ShoppingZone.postman_collection.json \
  -e postman/environments/E-ShoppingZone-Local.postman_environment.json \
  --reporters cli,json
```


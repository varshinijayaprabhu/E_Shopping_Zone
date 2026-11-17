# Profile API Contract

## 1. Get Current User Profile

- **Endpoint**: `GET /api/profile`
- **Access**: Protected (`ROLE_CUSTOMER`, `ROLE_MERCHANT`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Retrieves full profile details for the authenticated user based on JWT subject.

### Response `200 OK`
```json
{
  "success": true,
  "message": "Profile retrieved successfully",
  "data": {
    "id": 1,
    "email": "customer@eshopping.com",
    "fullName": "Alice Customer",
    "phoneNumber": "+91-9876543210",
    "addressLine1": "Flat 402, Green Glen Layout, Bellandur",
    "addressLine2": null,
    "city": "Bengaluru",
    "state": "Karnataka",
    "postalCode": "560103",
    "country": "India",
    "profilePictureUrl": "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
    "authProvider": "LOCAL",
    "roles": ["ROLE_CUSTOMER"],
    "createdAt": "2026-11-07T08:00:00Z"
  }
}
```

---

## 2. Update Current User Profile

- **Endpoint**: `PUT /api/profile`
- **Access**: Protected (`ROLE_CUSTOMER`, `ROLE_MERCHANT`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Updates profile attributes for the authenticated user. Email and role changes are restricted.

### Request Body
```json
{
  "fullName": "Alice R. Customer",
  "phoneNumber": "+91-9876543999",
  "addressLine1": "Tower B, Prestige Tech Park",
  "addressLine2": "Marathahalli-Sarjapur Outer Ring Rd",
  "city": "Bengaluru",
  "state": "Karnataka",
  "postalCode": "560103",
  "country": "India",
  "profilePictureUrl": "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"
}
```

### Response `200 OK`
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": 1,
    "email": "customer@eshopping.com",
    "fullName": "Alice R. Customer",
    "phoneNumber": "+91-9876543999",
    "addressLine1": "Tower B, Prestige Tech Park",
    "addressLine2": "Marathahalli-Sarjapur Outer Ring Rd",
    "city": "Bengaluru",
    "state": "Karnataka",
    "postalCode": "560103",
    "country": "India",
    "profilePictureUrl": "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
    "authProvider": "LOCAL",
    "roles": ["ROLE_CUSTOMER"],
    "updatedAt": "2026-11-07T10:30:00Z"
  }
}
```

### Errors
- `400 Bad Request` (`VALIDATION_FAILED`): Missing or invalid phone number or full name format.
- `401 Unauthorized`: Missing or expired access token.


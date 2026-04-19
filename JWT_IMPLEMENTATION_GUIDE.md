# JWT (JSON Web Token) Implementation Guide

## 📌 How JWT Works in SocializerAI

### 1. **Token Generation (Login)**
```
User sends credentials (username + password)
    ↓
Server validates credentials
    ↓
Server generates JWT token containing:
  - username
  - userId
  - email
  - issued time
  - expiration time (24 hours)
    ↓
Server signs token with secret key
    ↓
Token sent to frontend
```

### 2. **Token Storage (Frontend)**
```javascript
// After login, frontend stores token
localStorage.setItem('token', response.token);
```

### 3. **Token Usage (Authenticated Requests)**
```
Frontend sends every request with Authorization header:
  Authorization: Bearer <token>
    ↓
Server receives request
    ↓
Server extracts token from Authorization header
    ↓
Server validates token signature & expiration
    ↓
If valid: User authenticated ✅
If invalid/expired: Request denied ❌
```

---

## 🔧 API Endpoints

### **1. Login Endpoint**
```
POST /api/auth/login
Content-Type: application/json

Request:
{
  "username": "user1",
  "password": "password123"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "user1",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user1@example.com",
  "expiresIn": 86400000
}

Response (401 Unauthorized):
{
  "token": null,
  "message": "Invalid credentials"
}
```

### **2. Verify Token Endpoint**
```
GET /api/auth/verify
Authorization: Bearer <token>

Response (200 OK):
{
  "valid": true,
  "username": "user1",
  "message": "Token valid"
}

Response (401 Unauthorized):
{
  "valid": false,
  "message": "Token expired or invalid"
}
```

### **3. Get Current User Endpoint**
```
GET /api/auth/me
Authorization: Bearer <token>

Response (200 OK):
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "user1",
  "email": "user1@example.com",
  "fullName": "User One",
  ...
}

Response (401 Unauthorized):
{
  "message": "Unauthorized"
}
```

---

## 🛡️ JWT Secret & Expiration

Your JWT configuration in `application.properties`:
```properties
# JWT Secret (min 32 chars for HMAC-SHA256)
app.jwt.secret=${APP_JWT_SECRET:mySecureJWTSecretKeyFor2026SocializerAI123456}

# Expiration time in milliseconds (86400000 = 24 hours)
app.jwt.expiration=${APP_JWT_EXPIRATION:86400000}
```

**Important:** In production, use a strong random secret:
```bash
# Generate a strong 32+ character secret
openssl rand -hex 32
# Output: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6
```

---

## 📦 Files Created

1. **JwtTokenProvider.java** - Generates and validates JWT tokens
2. **JwtAuthenticationFilter.java** - Filters requests and extracts JWT
3. **LoginRequestDTO.java** - Request model for login
4. **LoginResponseDTO.java** - Response model with token

---

## 🧪 Testing JWT Flow

### Step 1: Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInVzZXJJZCI6IjU1MGU4NDAwLWUyOWItNDFkNC1hNzE2LTQ0NjY1NTQ0MDAwMCIsImVtYWlsIjoidGVzdEBleGFtcGxlLmNvbSIsImlhdCI6MTcwOTg0NzAwMCwiZXhwIjoxNzA5OTMzNDAwfQ.X1w2Y3z4A5B6C7D8E9F0G1H2I3J4K5L6M7N8O9P0Q1",
  "username": "testuser",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "test@example.com",
  "expiresIn": 86400000
}
```

### Step 2: Use Token in Authenticated Request
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Step 3: Verify Token
```bash
curl -X GET http://localhost:8080/api/auth/verify \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 🚀 Frontend Implementation (Angular)

Store token in localStorage after login:
```typescript
// auth.service.ts
login(username: string, password: string) {
  return this.http.post('/api/auth/login', {username, password})
    .pipe(
      tap(response => {
        if (response.token) {
          localStorage.setItem('token', response.token);
        }
      })
    );
}

// Send token in every request (via interceptor)
getToken() {
  return localStorage.getItem('token');
}
```

Add Authorization header in HTTP interceptor:
```typescript
// auth.interceptor.ts
intercept(req: HttpRequest<any>, next: HttpHandler) {
  const token = this.authService.getToken();
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }
  return next.handle(req);
}
```

---

## ⏰ Token Expiration Handling

**Token expires after 24 hours (86400000ms)**

When token expires:
```javascript
// Frontend receives 401 Unauthorized
// Clear stored token
localStorage.removeItem('token');
// Redirect to login page
router.navigate(['/login']);
```

---

## ✅ Security Best Practices

1. ✅ **Token is signed** - Cannot be modified without secret key
2. ✅ **Token has expiration** - Automatically invalid after 24 hours
3. ✅ **HTTPS recommended** - Prevent token interception
4. ✅ **Secure storage** - Frontend stores in localStorage (consider sessionStorage for better security)
5. ✅ **Logout clears token** - Frontend removes token on logout

---

## 📝 Notes

- JWT secret is stored in `.env` file (never in code)
- Token contains user info but is **not encrypted** (only signed)
- Always use HTTPS in production
- Consider refresh tokens for long sessions

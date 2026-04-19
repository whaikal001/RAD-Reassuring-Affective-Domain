# SocializerAI - Docker Deployment

## Quick Start (One Command!)

### Windows:
```batch
start.bat
```

### Linux/Mac:
```bash
chmod +x start.sh
./start.sh
```

That's it! The app will be available at:
- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080

---

## Manual Docker Commands

### Start everything:
```bash
docker-compose up --build -d
```

### View logs:
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

### Stop everything:
```bash
docker-compose down
```

### Stop and remove data:
```bash
docker-compose down -v
```

### Rebuild specific service:
```bash
docker-compose up --build -d backend
```

---

## Configuration

Edit `.env` file to customize:

```env
# Database
DB_USER=socializerai
DB_PASSWORD=your-secure-password

# JWT (CHANGE IN PRODUCTION!)
JWT_SECRET=your-very-long-secret-key-minimum-32-characters

# HuggingFace API (optional, for AI responses)
HF_API_TOKEN=hf_xxxxxxxxxxxxxxxxxxxx
```

---

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│    Frontend     │────▶│    Backend      │────▶│   PostgreSQL    │
│    (Nginx)      │     │  (Spring Boot)  │     │                 │
│    Port: 80     │     │   Port: 8080    │     │   Port: 5432    │
│                 │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## Ports Used

| Service    | Internal Port | External Port |
|------------|--------------|---------------|
| Frontend   | 80           | 80            |
| Backend    | 8080         | 8080          |
| PostgreSQL | 5432         | 5432          |

---

## Troubleshooting

### Port already in use
```bash
# Find what's using port 80
netstat -ano | findstr :80

# Or change ports in docker-compose.yml
ports:
  - "3000:80"  # Use port 3000 instead
```

### Container won't start
```bash
# Check logs
docker-compose logs backend

# Rebuild from scratch
docker-compose down -v
docker-compose up --build
```

### Database connection issues
```bash
# Check if postgres is running
docker-compose ps

# Connect to postgres directly
docker exec -it socializerai-db psql -U socializerai -d socializerai
```

---

## Development Mode

To run only specific services:

```bash
# Run only database (for local development)
docker-compose up -d postgres

# Then run backend locally
cd Backend
mvn spring-boot:run

# And frontend locally
cd Frontend
npm start
```

---

## Production Deployment

1. Update `.env` with secure passwords
2. Enable HTTPS (add SSL certificates to nginx)
3. Set `SPRING_PROFILE=prod`
4. Consider using Docker Swarm or Kubernetes for scaling

#!/bin/bash
echo "============================================"
echo "   RadAI - Docker Quick Start"
echo "============================================"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "ERROR: Docker is not running!"
    echo "Please start Docker and try again."
    exit 1
fi

# Create .env if not exists
if [ ! -f .env ]; then
    echo "Creating .env file from template..."
    cp .env.example .env
    echo ""
    echo "NOTE: Edit .env file to customize settings"
    echo ""
fi

echo "Starting RadAI..."
echo "This may take a few minutes on first run."
echo ""

# Build and start containers
docker-compose up --build -d

echo ""
echo "============================================"
echo "   RadAI is starting up!"
echo "============================================"
echo ""
echo "Frontend: http://localhost"
echo "Backend:  http://localhost:8080"
echo ""
echo "To view logs:   docker-compose logs -f"
echo "To stop:        docker-compose down"
echo ""

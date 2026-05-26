# Docker Compose Layout

The compose files are split by responsibility.

## Files

- `docker-compose.yml`: application services only (`tu-backend`, `tu-gateway`, `tu-integration-service`, `tu-rag-service`).
- `docker-compose.infra.yml`: shared infrastructure and external dependencies (`nacos`, databases, `qdrant`, `kaneo`).
- `docker-compose.dev.yml`: local development overrides that wire app services to the infra service names.

## Local All-In-One

```powershell
docker compose -f docker-compose.infra.yml -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

This starts the local infra plus all tu services. It is intended for development and demos.

## App Services Only

Copy `.env.example` to `.env`, adjust external infrastructure addresses, then run:

```powershell
docker compose up -d --build
```

Use this when Nacos, MySQL, Qdrant, and external systems are managed outside the application stack.

## Infra Only

```powershell
docker compose -f docker-compose.infra.yml up -d
```

Use this for local shared middleware without rebuilding application services.

## Kaneo

Kaneo is pulled from GHCR:

```powershell
docker compose -f docker-compose.infra.yml pull kaneo
docker compose -f docker-compose.infra.yml up -d kaneo-postgres kaneo
```

If GHCR is slow, use an external Kaneo instance and configure it from the `/tasks` page.

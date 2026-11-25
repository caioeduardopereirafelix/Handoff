# Handoff

## Como rodar localmente

1. Crie um arquivo `.env` na raiz com `DB_PASSWORD`, `RABBITMQ_PASSWORD`, `MINIO_ROOT_PASSWORD` e `HANDOFF_JWT_SECRET` (mínimo de 32 caracteres; gere com `openssl rand -base64 48`).
2. Suba os serviços:
   ```bash
   docker compose up -d
   ```
3. Backend (porta 8080):
   ```bash
   cd handoff && ./mvnw spring-boot:run
   ```
4. Frontend (porta 5173), em outro terminal:
   ```bash
   cd frontend && npm install && npm run dev
   ```

Abra http://localhost:5173 para o painel da equipe. Os links do portal do cliente têm o formato `http://localhost:5173/portal/<token>`, e os e-mails de desenvolvimento chegam no Mailpit em http://localhost:8025.

Se o backend estiver em outra porta, inicie o frontend com `BACKEND_URL=http://localhost:<porta>`.

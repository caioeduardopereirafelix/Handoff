# Handoff

## Como rodar localmente

1. Copie `.env.example` para `.env` e preencha as senhas e a `HANDOFF_JWT_SECRET`.
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

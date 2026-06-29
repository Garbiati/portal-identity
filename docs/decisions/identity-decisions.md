# Decisões de Identidade (ADR local) — portal-identity

> Decisões **deste repo**. As transversais à empresa também entram em
> `../../../portal-platform/docs/decisions/platform-decisions.md` (série P-xxx).

| ID | Decisão | Confirmado em | Notas |
|----|---------|---------------|-------|
| **I-001** | **Keycloak como IdP da empresa, em repo próprio `portal-identity`, realm único `portal`.** Materializa o **P-003**. Produto = client; papéis = client roles. Doctor-Hub é o 1º consumidor (`doctor-hub-web` público + `doctor-hub-api` resource com `admin/demandas/regulacao/gestor`). Nome vendor-neutral (Keycloak = implementação). | 2026-06-29 | Confirmado por Alessandro (nome `portal-identity`, extrair agora, realm único). Origem: o IdP nasceu dentro do `doctor-hub-api` (D-140) e foi extraído. Pendências: API valida JWT (slice 2), web loga via OIDC (slice 2), produção (DB próprio + IaC), federação de Teleconsulta/Telediagnóstico (com os donos). |

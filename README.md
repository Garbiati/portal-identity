# portal-identity — Plataforma de Identidade (Portal Telemedicina)

> **IdP da empresa.** Centraliza autenticação/SSO de toda a Portal Telemedicina via **Keycloak**.
> Repo de **infra compartilhada** — não tem código de app. Governança: ver [`../portal-platform`](../portal-platform)
> (constituição da empresa) + decisão de plataforma **P-003**.

## Por que existe
Identidade é infra **transversal**: serve vários produtos (Doctor-Hub hoje; Teleconsulta e
Telediagnóstico no futuro), tem ciclo de vida e dono próprios, e merece fronteira de segurança
isolada. Por isso vive em repo separado, registrado no manifest `repos.yml` do `portal-platform`.

> O nome é **vendor-neutral de propósito**: o Keycloak é a *implementação* atual; o repo é a
> *plataforma de identidade*. Se um dia trocar a ferramenta, o nome não mente.

## Modelo (alvo) — realm único da empresa
- **Um realm `portal`** → uma identidade por pessoa, SSO entre produtos (objetivo: unificar os doutores).
- **Cada produto = um client** do Keycloak. Papéis do produto = **client roles** desse client.
  - Doctor-Hub: client de login `doctor-hub-web` (público, PKCE) + client resource `doctor-hub-api`
    (bearer-only) que **define os papéis**: `admin · demandas · regulacao · gestor`.
- A **API** valida o JWT pela sua audiência e lê os papéis de `resource_access["doctor-hub-api"].roles`.

> **Teleconsulta / Telediagnóstico já têm login próprio.** Unificá-los = **migração de usuários ou
> federação** — esforço grande, com os donos desses produtos. **Não agora.** Hoje só o Doctor-Hub
> consome o realm (greenfield).

## Rodar (dev)
```bash
cp .env.example .env       # ajuste se quiser
make up                    # sobe Keycloak + importa o realm 'portal'
make status                # estado
```
- Console admin: http://localhost:8089/admin (usuário `admin`, senha do `.env`).
- Issuer OIDC: `http://localhost:8089/realms/portal`.
- Porta **8089** (a 8080 é usada por outro processo neste ambiente); health em :9000.
- **DEV é efêmero** (`start-dev`, H2 em memória): a fonte da verdade é `realms/portal-realm.json`,
  re-importado a cada boot. Mudou no console? **Exporte de volta pro JSON** ou se perde no próximo `up`.

### Usuários-semente (DEV-only, senha `Dev@12345`)
| usuário | papel (client role em doctor-hub-api) |
|---|---|
| `mariana` | demandas |
| `aldair` | regulacao |
| `eronildes` | gestor |
| `admin-dh` | admin |

## Estrutura
```
portal-identity/
├── docker-compose.yml     ← Keycloak (dev); prod = DB próprio + modo `start` (futuro)
├── realms/portal-realm.json  ← realm como código (fonte da verdade)
├── .env(.example)         ← admin do console (gitignored; prod = Secret Manager)
├── Makefile               ← up / down / status / logs
└── docs/decisions/        ← ADRs próprias (self-hosted)
```

## Roadmap curto
- [ ] Doctor-Hub: API valida JWT + RBAC; web loga via OIDC (substitui login fake).
- [ ] Produção: Keycloak com **DB próprio** (Cloud SQL) + modo `start` persistente + IaC.
- [ ] Migrar **realm-as-code** de export JSON → **Terraform** (provider keycloak) quando estabilizar.
- [ ] Federação/migração de Teleconsulta + Telediagnóstico (com os donos) — unificar os doutores.

# portal-identity — instruções para a IA

> Repo de **infra de identidade** (Keycloak) da Portal Telemedicina. A **constituição da empresa**
> está em [`../../CLAUDE.md`](../../CLAUDE.md) — **leia-a antes de qualquer
> ação**. Aqui ficam só os recortes deste repo. Decisão de plataforma: **P-003**.

## Diretriz Suprema (herdada)
**NÃO INFERIR REGRA DE NEGÓCIO / DE IDENTIDADE. NA DÚVIDA, PERGUNTAR.** Modelo de realm, mapeamento
de papéis, quem-vira-doutor, base legal/LGPD, federação com Teleconsulta/Telediagnóstico — **nada
disso se inventa**. Sem decisão registrada (P-xxx ou ADR aqui), não existe.

## Segurança (inegociável)
- **Zero segredo no código/realm versionado.** `.env` é gitignored; prod = **GCP Secret Manager**.
  As senhas de usuário-semente no `realms/*.json` são **DEV-only** (realm efêmero) — nunca em prod.
- **LGPD:** dado de pessoa real (doutor/paciente) é sensível. Realm de prod ≠ realm de dev.
- Verificar todo provider/SPI/imagem antes de adicionar.

## Modelo travado (P-003)
- **Realm único `portal`** (uma identidade por pessoa; SSO entre produtos).
- **Produto = client**; papéis do produto = **client roles**. Doctor-Hub: `doctor-hub-web` (público,
  login PKCE) + `doctor-hub-api` (bearer-only, define `admin/demandas/regulacao/gestor`).
- A API valida o JWT pela audiência e lê `resource_access["doctor-hub-api"].roles`.

## Como trabalhar aqui
- **Realm como código** é a fonte da verdade (`realms/portal-realm.json`). Mudou no console do
  Keycloak? **Exporte de volta** pro JSON — o `start-dev --import-realm` re-importa a cada boot e
  descarta o que não estiver no arquivo.
- **Lotes pequenos.** Toda decisão de identidade confirmada → registrar em `docs/decisions/` (ADR
  local) e, se transversal, também em `../../docs/decisions/platform-decisions.md`.
- **Dev efêmero, prod persistente:** `start-dev`+H2 só local; prod terá DB próprio + modo `start`.

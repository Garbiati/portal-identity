#!/usr/bin/env python3
"""Gera o realm de PRODUÇÃO a partir do realm de DEV (fonte da verdade), aplicando as diferenças
de produção — sem driftar da estrutura do dev. Ver D-143 / P-006.

Diferenças aplicadas:
  • remove os usuários-semente HUMANOS (mariana/aldair/eronildes/admin-dh) — em prod os usuários
    reais são criados pela aplicação/console (LGPD: nada de senha DEV no git);
  • MANTÉM o service account (service-account-doctor-hub-admin) com seus role-mappings;
  • parametriza as URLs do client de login por ${FRONT_BASE_URL} (o front é deployado depois);
  • sslRequired = external (Cloud Run termina TLS na borda).
SMTP (${SMTP_*}) e o secret do service account (${ADMIN_CLIENT_SECRET}) já vêm por env do dev.

Uso: python3 scripts/gerar-realm-prod.py  → escreve realms-prod/portal-realm.json
"""
import json
import pathlib

RAIZ = pathlib.Path(__file__).resolve().parent.parent
dev = json.loads((RAIZ / "realms" / "portal-realm.json").read_text(encoding="utf-8"))

# 1) usuários: mantém só os service accounts (descarta os humanos de demo)
dev["users"] = [u for u in dev.get("users", []) if u.get("serviceAccountClientId")]

# 2) TLS na borda (Cloud Run)
dev["sslRequired"] = "external"

# 3) URLs do client de login por env (front deployado depois)
FRONT = "${FRONT_BASE_URL}"
for c in dev.get("clients", []):
    if c.get("clientId") == "doctor-hub-web":
        c["rootUrl"] = FRONT
        c["baseUrl"] = "/"
        c["redirectUris"] = [f"{FRONT}/*"]
        c["webOrigins"] = [FRONT]
        attrs = c.setdefault("attributes", {})
        attrs["post.logout.redirect.uris"] = f"{FRONT}/*"

out = RAIZ / "realms-prod" / "portal-realm.json"
out.parent.mkdir(exist_ok=True)
out.write_text(json.dumps(dev, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print(f"✓ {out.relative_to(RAIZ)} gerado "
      f"({len(dev['users'])} service account(s), 0 usuários humanos, sslRequired={dev['sslRequired']})")

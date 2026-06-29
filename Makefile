## portal-identity — IdP da empresa (Keycloak). `make help` lista os alvos.
.DEFAULT_GOAL := help
.PHONY: help up down status logs realm-export

KC_URL  := http://localhost:8089
REALM   := portal

help: ## Lista os alvos
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-13s\033[0m %s\n", $$1, $$2}'

up: ## Sobe o Keycloak (importa o realm) e espera ficar pronto
	@set -a; . ./.env; set +a; docker compose up -d keycloak
	@printf '  aguardando Keycloak'; for i in $$(seq 1 60); do \
		curl -fsS http://localhost:9000/health/ready >/dev/null 2>&1 && break; printf '.'; sleep 1; done; echo
	@if curl -fsS $(KC_URL)/realms/$(REALM)/.well-known/openid-configuration >/dev/null 2>&1; then \
		printf '  \033[32m✓\033[0m Keycloak pronto — realm %s em %s (admin: %s/admin)\n' "$(REALM)" "$(KC_URL)" "$(KC_URL)"; \
	else echo '  ✗ realm não respondeu — veja: docker logs portal-keycloak'; fi

down: ## Derruba o Keycloak (realm é re-importado no próximo up)
	docker compose down

status: ## Estado do Keycloak + realm
	@printf 'Keycloak: '; curl -fsS -o /dev/null -w 'OK\n' http://localhost:9000/health/ready 2>/dev/null || echo 'fora do ar'
	@printf 'Realm %s: ' "$(REALM)"; curl -fsS -o /dev/null -w 'OK\n' $(KC_URL)/realms/$(REALM)/.well-known/openid-configuration 2>/dev/null || echo 'fora do ar'

logs: ## Tail dos logs do Keycloak
	docker logs -f --tail 50 portal-keycloak

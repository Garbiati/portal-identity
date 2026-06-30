---
id: SPEC-OTP-DEV
title: Login por código de uso único (OTP) — modo DEV
status: implemented          # draft | specified | tested | implemented
owner: portal-identity
area: Acesso
clickup:
figma:
validated_by: "Alessandro Garbiati"
validated_at: "2026-06-29"
last_update: 2026-06-29
---

# Login por código de uso único (OTP) — modo DEV

> **Materializa o fio em aberto do HANDOFF** ("OTP login e-mail + SMS"). Esta spec cobre o **modo DEV**:
> o código é **gerado e escrito no log** do Keycloak — **não** há envio real (SMTP/SMS) ainda. O envio
> real (e-mail via SMTP e SMS via gateway pago) é uma entrega posterior, fora do escopo aqui.

## 1. Problema / Dor  _(Definition of Success)_
- **Dor:** hoje só se entra com **senha**. Profissionais de saúde esquecem senha e/ou preferem um
  código rápido; e o "Esqueceu a senha?" depende de canal de envio que ainda não existe.
- **De quem:** qualquer usuário do Portal (médico/regulação/gestor/admin).
- **Evidência:** decisão do dono (Alessandro) de oferecer login por código além da senha (HANDOFF §5).
- **Sucesso = quando:** um usuário consegue entrar **sem digitar a senha**, pedindo um código de uso
  único enviado ao seu canal (em DEV, lido no log), e a senha continua funcionando como antes.

## 2. Função  _(o "o quê")_
Na tela de login o usuário informa **um identificador** (usuário, e-mail, CPF ou telefone — como já
hoje, I-002). Em seguida escolhe **como provar quem é**:
- **Senha** (caminho atual), ou
- **Código por e-mail** (se tiver e-mail), ou
- **Código por SMS** (se tiver telefone).

Ao escolher um canal de código, o sistema gera um código numérico, "envia" pelo canal (em **DEV**:
escreve no log) e mostra uma tela para digitar o código. Código correto e dentro da validade → entra.

## 3. Regras de negócio  _(somente CONFIRMADAS)_
- ✅ O código é **login ALTERNATIVO** (senha **OU** código), **não** segundo fator/2FA — _Confirmado por Alessandro em 2026-06-29_.
- ✅ **O usuário escolhe o canal** (e-mail e/ou SMS), entre os que ele tem cadastrados — _Confirmado por Alessandro em 2026-06-29_.
- ✅ Parâmetros do código: **6 dígitos**, validade **5 minutos**, **5 tentativas** — _Confirmado por Alessandro em 2026-06-29_.
- ✅ **Modo DEV:** o código é **escrito no log** do Keycloak; **sem envio real** (SMTP/SMS fica para depois) — _Confirmado por Alessandro em 2026-06-29 (HANDOFF §5)_.
- ✅ Resolução do identificador (username/e-mail/CPF/telefone, só dígitos) segue a regra do **I-002**.

## 4. Critérios de aceite  _(fonte do teste — Gherkin/BDD)_
```gherkin
Cenário: Entrar por código de e-mail (DEV)
  Dado um usuário com e-mail cadastrado
  E que ele informou um identificador válido (usuário/e-mail/CPF/telefone)
  Quando ele escolhe "Código por e-mail"
  Então um código de 6 dígitos é escrito no log do Keycloak
  E ao digitar esse código dentro de 5 minutos ele é autenticado

Cenário: Entrar por código de SMS (DEV)
  Dado um usuário com telefone cadastrado
  Quando ele escolhe "Código por SMS"
  Então um código de 6 dígitos é escrito no log do Keycloak
  E ao digitá-lo corretamente ele é autenticado

Cenário: Senha continua funcionando (não-regressão do I-002)
  Dado um usuário com senha
  Quando ele informa o identificador (username/CPF/telefone) e escolhe "Senha"
  Então ao digitar a senha correta ele é autenticado

Cenário: Canal indisponível não aparece
  Dado um usuário SEM e-mail cadastrado
  Quando ele chega na escolha de credencial
  Então a opção "Código por e-mail" NÃO é oferecida

Cenário: Código expirado é rejeitado
  Dado um código gerado há mais de 5 minutos
  Quando o usuário o digita
  Então a autenticação é recusada e ele pode pedir um novo código

Cenário: Tentativas esgotadas
  Dado um código válido
  Quando o usuário erra 5 vezes
  Então a autenticação por código é bloqueada nesta sessão
```

## 5. Definition of Done
- [x] Cenários principais verificados **E2E** (Keycloak rodando, via curl + PKCE, 2026-06-29):
      e-mail, SMS, senha (não-regressão) e código errado. Ver §10.
- [~] Cenários verificados por **revisão de código** (não cronometrados em E2E): expiração (5 min),
      tentativas esgotadas (5), "canal indisponível não aparece" (`configuredFor`).
- [x] Sem perguntas abertas 🔴 pendentes.
- [x] Validado por humano (`validated_by`).
- [x] Realm versionado (`realms/portal-realm.json`) é a fonte da verdade do flow (`browser-otp`).
- [x] Código nunca aparece em resposta HTTP; só no log (DEV) e com destino mascarado (LGPD). Zero segredo no git.

## 6. Fora de escopo
- **Envio real** por **SMTP** (e-mail) e por **gateway de SMS pago** (Twilio/Zenvia) — entrega futura.
- **"Esqueceu a senha?"** com envio real (depende do SMTP) — futura.
- **Rate-limit por destinatário** / antifraude de SMS — futura (em DEV não há custo de envio).
- TOTP/authenticator-app e WebAuthn/passkey.

## 7. Dependências & Integrações
- Keycloak (I-001) + provider Java SPI no molde do **I-002** (`providers/login-cpf-telefone`).
- Tema `portal` (login) — nova página de digitação do código + labels PT-BR.
- Futuro: Secret Manager (GCP) para credenciais de SMTP/SMS no envio real.

## 8. Perguntas abertas  _(NÃO INFERIR — perguntar)_
- 🟢 Reenvio de código: cooldown entre reenvios (relevante só no envio real, por custo de SMS).
- 🟢 Texto/identidade visual da mensagem de e-mail/SMS (definir junto do envio real).

## 9. Consequência de UX (registrar, não é regra de negócio)
Para oferecer "senha OU código" a partir de **um único identificador**, o login passa a ter **duas
etapas** no caminho da senha (1: identificador → 2: senha), em vez de uma só tela. É o padrão Keycloak
para login com fatores alternativos (passwordless). Se preferir manter a senha numa tela única, é uma
decisão a registrar — abre 🟡.

## 10. Verificação (E2E 2026-06-29)
Flow `browser-otp` provado via curl + Auth Code/PKCE (client `doctor-hub-web`), sem trocar o code
(basta chegar no redirect com `?code=`):
- **Etapa 1 só-identificador:** página inicial **sem** campo de senha (`login-username.ftl`).
- **Senha (não-regressão I-002):** identificador por **CPF formatado** (`233.490.661-05`) → resolve
  `aldair` → senha → **authorization code**. ✅
- **Código por e-mail:** "tentar outra forma" → opção e-mail → código no log
  (`[OTP-DEV] … via e-mail (al***@…): 194419`) → submete → **authorization code**. ✅
- **Código por SMS:** idem, código no log (`… via SMS: 542238`) → **authorization code**. ✅
- **Código errado:** `otp=000000` → re-render com "Código inválido. Confira e tente novamente." ✅

> Detalhe técnico: o `UsernameForm` do Keycloak é `final`; replicamos estendendo `UsernamePasswordForm`
> e sobrescrevendo `authenticate()`/`createLoginForm()` para `LoginFormsProvider.createLoginUsername()`
> (sobrescrever só `createLoginForm` **não** basta — o GET inicial não passa por ele nesta versão).

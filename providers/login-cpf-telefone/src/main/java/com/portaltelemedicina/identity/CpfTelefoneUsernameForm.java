package com.portaltelemedicina.identity;

import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Form de login que aceita o usuário por <b>CPF</b> ou <b>telefone</b> (além de username/email).
 *
 * <p>Estende o {@link UsernamePasswordForm} padrão e só intercepta a resolução do usuário: se o
 * que foi digitado não bate como username/email, procura um usuário cujo atributo {@code cpf} ou
 * {@code telefone} (comparado <b>só por dígitos</b>) seja igual. Achando, troca o identificador
 * digitado pelo username real e deixa o fluxo padrão seguir (senha, brute-force, OTP, eventos).</p>
 *
 * <p>Os atributos {@code cpf}/{@code telefone} devem estar gravados <b>normalizados (só dígitos)</b>
 * no usuário — ver o realm. Nada de regra de negócio aqui: é só roteamento de identidade.</p>
 */
public class CpfTelefoneUsernameForm extends UsernamePasswordForm {

    static final String FIELD_USERNAME = "username";
    static final String ATTR_CPF = "cpf";
    static final String ATTR_TELEFONE = "telefone";

    @Override
    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        String login = formData.getFirst(FIELD_USERNAME);
        if (login != null && !login.isBlank()) {
            KeycloakSession session = context.getSession();
            RealmModel realm = context.getRealm();

            // 1) Já resolve como username (ou email, se permitido)? deixa o padrão cuidar.
            UserModel user = session.users().getUserByUsername(realm, login.trim());
            if (user == null && realm.isLoginWithEmailAllowed() && login.contains("@")) {
                user = session.users().getUserByEmail(realm, login.trim());
            }

            // 2) Senão, tenta CPF e depois telefone, comparando só os dígitos.
            if (user == null) {
                String digits = login.replaceAll("\\D", "");
                if (digits.length() >= 8) { // evita lookup com lixo curto
                    user = first(session, realm, ATTR_CPF, digits);
                    if (user == null) {
                        user = first(session, realm, ATTR_TELEFONE, digits);
                    }
                }
            }

            // 3) Achou por atributo → substitui o identificador digitado pelo username real,
            //    para o resto do fluxo seguir exatamente como no login padrão.
            if (user != null && !user.getUsername().equalsIgnoreCase(login)) {
                formData.putSingle(FIELD_USERNAME, user.getUsername());
            }
        }
        return super.validateForm(context, formData);
    }

    private static UserModel first(KeycloakSession session, RealmModel realm, String attr, String value) {
        return session.users().searchForUserByUserAttributeStream(realm, attr, value)
                .findFirst().orElse(null);
    }
}

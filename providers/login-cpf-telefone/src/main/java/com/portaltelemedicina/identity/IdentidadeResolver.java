package com.portaltelemedicina.identity;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Resolução de identidade do realm (I-002): aceita o login por <b>username</b>, <b>e-mail</b>,
 * <b>CPF</b> ou <b>telefone</b>. Não há regra de negócio aqui — é só roteamento de identidade.
 *
 * <p>Os atributos {@code cpf}/{@code telefone} devem estar gravados <b>normalizados (só dígitos)</b>
 * no usuário (ver o realm). A comparação é feita só por dígitos, então o identificador digitado pode
 * vir formatado ou não.</p>
 */
public final class IdentidadeResolver {

    static final String ATTR_CPF = "cpf";
    static final String ATTR_TELEFONE = "telefone";

    private IdentidadeResolver() {
    }

    /**
     * Acha o usuário a partir do identificador digitado. Retorna {@code null} se nada bate.
     */
    public static UserModel resolver(KeycloakSession session, RealmModel realm, String login) {
        if (login == null || login.isBlank()) {
            return null;
        }
        String id = login.trim();

        // 1) username, ou e-mail (se permitido).
        UserModel user = session.users().getUserByUsername(realm, id);
        if (user == null && realm.isLoginWithEmailAllowed() && id.contains("@")) {
            user = session.users().getUserByEmail(realm, id);
        }

        // 2) senão, CPF e depois telefone — comparando só os dígitos.
        if (user == null) {
            String digits = id.replaceAll("\\D", "");
            if (digits.length() >= 8) { // evita lookup com lixo curto
                user = first(session, realm, ATTR_CPF, digits);
                if (user == null) {
                    user = first(session, realm, ATTR_TELEFONE, digits);
                }
            }
        }
        return user;
    }

    private static UserModel first(KeycloakSession session, RealmModel realm, String attr, String value) {
        return session.users().searchForUserByUserAttributeStream(realm, attr, value)
                .findFirst().orElse(null);
    }
}

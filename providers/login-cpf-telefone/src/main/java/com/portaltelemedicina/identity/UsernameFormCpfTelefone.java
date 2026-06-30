package com.portaltelemedicina.identity;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Form que coleta <b>só o identificador</b> (sem senha) aceitando username, e-mail, CPF ou telefone
 * (I-002). É a 1ª etapa do flow "senha OU código": resolve o usuário e o coloca no contexto; o passo
 * seguinte escolhe a credencial (senha ou código de uso único).
 *
 * <p>Estende o {@link UsernamePasswordForm} (o {@code UsernameForm} do Keycloak é {@code final}) e
 * espelha o que ele faz: renderiza o template só-identificador via
 * {@link LoginFormsProvider#createLoginUsername()} (tanto no 1º GET quanto no re-render de erro) e,
 * na submissão, valida <b>só o usuário</b> — a credencial vem no próximo passo do flow. A única
 * adição é a resolução por CPF/telefone ({@link IdentidadeResolver}).</p>
 */
public class UsernameFormCpfTelefone extends UsernamePasswordForm {

    static final String FIELD_USERNAME = "username";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.challenge(context.form().createLoginUsername());
    }

    @Override
    public boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        String login = formData.getFirst(FIELD_USERNAME);
        if (login != null && !login.isBlank()) {
            KeycloakSession session = context.getSession();
            RealmModel realm = context.getRealm();
            UserModel user = IdentidadeResolver.resolver(session, realm, login);
            if (user != null && !user.getUsername().equalsIgnoreCase(login.trim())) {
                formData.putSingle(FIELD_USERNAME, user.getUsername());
            }
        }
        // Só resolve/valida o usuário — a credencial (senha OU código) vem no próximo passo do flow.
        return validateUser(context, formData);
    }

    @Override
    protected Response createLoginForm(LoginFormsProvider form) {
        return form.createLoginUsername();
    }
}

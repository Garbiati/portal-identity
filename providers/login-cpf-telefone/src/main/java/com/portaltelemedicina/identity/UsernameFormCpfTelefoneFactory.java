package com.portaltelemedicina.identity;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Fábrica do {@link UsernameFormCpfTelefone}. Authenticator de 1ª etapa (só identificador), usado no
 * lugar do "Username Form" padrão no flow de browser do realm.
 */
public class UsernameFormCpfTelefoneFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "auth-username-form-cpf-telefone";

    private static final UsernameFormCpfTelefone SINGLETON = new UsernameFormCpfTelefone();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getDisplayType() {
        return "Username Form (CPF/telefone)";
    }

    @Override
    public String getReferenceCategory() {
        return "username";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Coleta só o identificador aceitando username, email, CPF ou telefone (1ª etapa do login).";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }
}

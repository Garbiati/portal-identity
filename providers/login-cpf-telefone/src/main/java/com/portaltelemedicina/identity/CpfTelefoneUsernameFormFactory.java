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
 * Fábrica do {@link CpfTelefoneUsernameForm}. Registra um authenticator com id próprio para ser
 * usado no lugar do "Username Password Form" padrão, no flow de browser do realm.
 */
public class CpfTelefoneUsernameFormFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "auth-username-password-cpf-telefone";

    // Stateless → instância única (mesmo padrão do UsernamePasswordFormFactory do Keycloak).
    private static final CpfTelefoneUsernameForm SINGLETON = new CpfTelefoneUsernameForm();

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
        // sem configuração
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // nada a fazer
    }

    @Override
    public void close() {
        // nada a fechar
    }

    @Override
    public String getDisplayType() {
        return "Username (CPF/telefone) Password Form";
    }

    @Override
    public String getReferenceCategory() {
        return "password";
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
        return "Valida usuário + senha aceitando o login por username, email, CPF ou telefone.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }
}

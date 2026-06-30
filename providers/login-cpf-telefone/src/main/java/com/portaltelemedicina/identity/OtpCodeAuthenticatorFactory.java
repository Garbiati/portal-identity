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
 * Base das fábricas do {@link OtpCodeAuthenticator}. Cada canal (e-mail/SMS) tem sua própria fábrica
 * para virar uma opção distinta no "tentar outra forma" do login.
 */
public abstract class OtpCodeAuthenticatorFactory implements AuthenticatorFactory {

    private final OtpCodeAuthenticator authenticator;

    protected OtpCodeAuthenticatorFactory(OtpCodeAuthenticator.Channel channel) {
        this.authenticator = new OtpCodeAuthenticator(channel);
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return authenticator;
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
    public String getReferenceCategory() {
        return "otp";
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
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }
}

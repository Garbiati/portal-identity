package com.portaltelemedicina.identity;

/** Fábrica do login por código via <b>e-mail</b> (modo DEV). */
public class EmailCodeAuthenticatorFactory extends OtpCodeAuthenticatorFactory {

    public static final String PROVIDER_ID = "auth-otp-email-dev";

    public EmailCodeAuthenticatorFactory() {
        super(OtpCodeAuthenticator.Channel.EMAIL);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Código de uso único (e-mail) [DEV]";
    }

    @Override
    public String getHelpText() {
        return "Login por código de uso único enviado ao e-mail. DEV: o código vai para o log.";
    }
}

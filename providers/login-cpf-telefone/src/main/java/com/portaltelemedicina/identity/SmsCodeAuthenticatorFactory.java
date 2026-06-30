package com.portaltelemedicina.identity;

/** Fábrica do login por código via <b>SMS</b> (modo DEV). */
public class SmsCodeAuthenticatorFactory extends OtpCodeAuthenticatorFactory {

    public static final String PROVIDER_ID = "auth-otp-sms-dev";

    public SmsCodeAuthenticatorFactory() {
        super(OtpCodeAuthenticator.Channel.SMS);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Código de uso único (SMS) [DEV]";
    }

    @Override
    public String getHelpText() {
        return "Login por código de uso único enviado por SMS. DEV: o código vai para o log.";
    }
}

package com.portaltelemedicina.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * {@link OtpCodeAuthenticator#authenticate}: geração + envio inicial do código, mais os ramos de
 * "sem canal" e "falha de envio". Também valida o <b>formato do OTP gerado</b> (sempre 6 dígitos,
 * zero-padded). Canal EMAIL com {@link EmailSenderProvider} mockado — nenhum SMTP/rede real.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpCodeAuthenticatorAuthenticateTest {

    @Mock AuthenticationFlowContext ctx;
    @Mock AuthenticationSessionModel authSession;
    @Mock KeycloakSession session;
    @Mock RealmModel realm;
    @Mock UserModel user;
    @Mock EmailSenderProvider emailSender;

    LoginFormsProvider forms;
    Response response;
    OtpCodeAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        authenticator = new OtpCodeAuthenticator(OtpCodeAuthenticator.Channel.EMAIL);
        forms = mock(LoginFormsProvider.class, Answers.RETURNS_SELF);
        response = mock(Response.class);
        lenient().when(forms.createForm(anyString())).thenReturn(response);
        lenient().when(ctx.form()).thenReturn(forms);
        lenient().when(ctx.getAuthenticationSession()).thenReturn(authSession);
        lenient().when(ctx.getUser()).thenReturn(user);
        lenient().when(ctx.getSession()).thenReturn(session);
        lenient().when(ctx.getRealm()).thenReturn(realm);
        lenient().when(session.getProvider(EmailSenderProvider.class)).thenReturn(emailSender);
        lenient().when(user.getUsername()).thenReturn("dr.teste");
    }

    @Test
    @DisplayName("sem canal (e-mail nulo) → attempted(), deixa o flow seguir por outra via")
    void semCanal_attempted() {
        when(user.getEmail()).thenReturn(null);

        authenticator.authenticate(ctx);

        verify(ctx).attempted();
        verify(ctx, never()).challenge(any());
    }

    @Test
    @DisplayName("e-mail em branco também cai em attempted()")
    void canalEmBranco_attempted() {
        when(user.getEmail()).thenReturn("   ");

        authenticator.authenticate(ctx);

        verify(ctx).attempted();
        verify(ctx, never()).challenge(any());
    }

    @Test
    @DisplayName("com destino e envio OK → challenge + gera código de 6 dígitos, TTL e zera tentativas")
    void envioOk_desafiaEGeraCodigo() {
        when(user.getEmail()).thenReturn("dr@example.com");

        authenticator.authenticate(ctx);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(authSession).setAuthNote(eq("OTP_CODE"), codeCaptor.capture());
        assertTrue(codeCaptor.getValue().matches("\\d{6}"), "código deve ser 6 dígitos");
        verify(authSession).setAuthNote(eq("OTP_EXPIRES_AT"), anyString());
        verify(authSession).setAuthNote("OTP_ATTEMPTS", "0");
        verify(ctx).challenge(response);
    }

    @Test
    @DisplayName("falha no envio (SMTP explode) → ainda desafia, mas anexa erro amigável no form")
    void envioFalha_desafiaComErro() throws Exception {
        when(user.getEmail()).thenReturn("dr@example.com");
        doThrow(new RuntimeException("smtp down")).when(emailSender)
                .send(any(), any(UserModel.class), anyString(), anyString(), anyString());

        authenticator.authenticate(ctx);

        verify(forms).addError(any(FormMessage.class)); // mostra "otpCodeSendError"
        verify(ctx).challenge(response);
    }

    @Test
    @DisplayName("o gerador nunca produz código fora de 6 dígitos (loop de estresse — cobre zero-padding)")
    void geradorSempre6Digitos() {
        when(user.getEmail()).thenReturn("dr@example.com");
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        for (int i = 0; i < 2000; i++) {
            authenticator.authenticate(ctx);
        }

        verify(authSession, org.mockito.Mockito.atLeast(2000)).setAuthNote(org.mockito.ArgumentMatchers.eq("OTP_CODE"), codeCaptor.capture());
        for (String code : codeCaptor.getAllValues()) {
            assertEquals(6, code.length(), "código fora de 6 dígitos: " + code);
            assertTrue(code.matches("\\d{6}"), "código não numérico: " + code);
        }
    }
}

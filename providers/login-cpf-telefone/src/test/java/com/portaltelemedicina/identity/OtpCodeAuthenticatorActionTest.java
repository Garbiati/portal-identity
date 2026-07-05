package com.portaltelemedicina.identity;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.common.util.Time;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * <b>O teste mais importante em auth: os caminhos de NEGAÇÃO.</b> Exercita
 * {@link OtpCodeAuthenticator#action} garantindo que só o código certo, ativo e dentro do limite de
 * tentativas autentica — e que código errado, ausente ou expirado <b>NUNCA</b> chama
 * {@code context.success()}.
 *
 * <p>Todo o {@link AuthenticationFlowContext} e a sessão de autenticação são mockados; nada de rede
 * nem banco. Usamos o canal {@code EMAIL} (o envio real só acontece no reenvio, e é mockado à parte).</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpCodeAuthenticatorActionTest {

    private static final String NOTE_CODE = "OTP_CODE";
    private static final String NOTE_EXPIRES = "OTP_EXPIRES_AT";
    private static final String NOTE_ATTEMPTS = "OTP_ATTEMPTS";

    @Mock AuthenticationFlowContext ctx;
    @Mock AuthenticationSessionModel authSession;
    @Mock HttpRequest httpRequest;
    @Mock UserModel user;

    LoginFormsProvider forms;
    Response response;
    MultivaluedMap<String, String> form;
    OtpCodeAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        authenticator = new OtpCodeAuthenticator(OtpCodeAuthenticator.Channel.EMAIL);

        // Form provider encadeável (setAttribute/addError devolvem o próprio provider).
        forms = mock(LoginFormsProvider.class, Answers.RETURNS_SELF);
        response = mock(Response.class);
        lenient().when(forms.createForm(anyString())).thenReturn(response);
        lenient().when(ctx.form()).thenReturn(forms);

        form = new MultivaluedHashMap<>();
        lenient().when(ctx.getHttpRequest()).thenReturn(httpRequest);
        lenient().when(httpRequest.getDecodedFormParameters()).thenReturn(form);
        lenient().when(ctx.getAuthenticationSession()).thenReturn(authSession);
        lenient().when(ctx.getUser()).thenReturn(user);
    }

    private void codigoAtivo(String codigo, String attempts) {
        when(authSession.getAuthNote(NOTE_CODE)).thenReturn(codigo);
        when(authSession.getAuthNote(NOTE_EXPIRES)).thenReturn(String.valueOf(Time.currentTime() + 120));
        when(authSession.getAuthNote(NOTE_ATTEMPTS)).thenReturn(attempts);
    }

    // ---- SUCESSO -------------------------------------------------------------------------------

    @Test
    @DisplayName("código certo + ativo → success() e limpa as notas")
    void codigoCerto_autentica() {
        codigoAtivo("123456", "0");
        form.putSingle("otp", "123456");

        authenticator.action(ctx);

        verify(ctx).success();
        verify(ctx, never()).failure(any());
        verify(ctx, never()).failureChallenge(any(), any());
        verify(authSession).removeAuthNote(NOTE_CODE);
    }

    @Test
    @DisplayName("código certo com espaços em volta é aceito (trim)")
    void codigoCertoComEspacos() {
        codigoAtivo("123456", "0");
        form.putSingle("otp", "  123456  ");

        authenticator.action(ctx);

        verify(ctx).success();
        verify(ctx, never()).failureChallenge(any(), any());
    }

    // ---- NEGAÇÃO: código errado ----------------------------------------------------------------

    @Test
    @DisplayName("código errado (tentativas abaixo do máx) → INVALID_CREDENTIALS e NÃO autentica")
    void codigoErrado_negaEReDesafia() {
        codigoAtivo("123456", "0");
        form.putSingle("otp", "000000");

        authenticator.action(ctx);

        verify(ctx, never()).success();
        verify(ctx).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), any());
        verify(authSession).setAuthNote(NOTE_ATTEMPTS, "1");
    }

    @Test
    @DisplayName("estourou o máximo de tentativas → failure() duro (INVALID_CREDENTIALS) e limpa")
    void maxTentativas_falhaDuro() {
        codigoAtivo("123456", "4"); // próxima tentativa = 5 = MAX_ATTEMPTS
        form.putSingle("otp", "999999");

        authenticator.action(ctx);

        verify(ctx, never()).success();
        verify(ctx).failure(AuthenticationFlowError.INVALID_CREDENTIALS);
        verify(authSession).removeAuthNote(NOTE_CODE);
    }

    // ---- NEGAÇÃO: credencial ausente -----------------------------------------------------------

    @Test
    @DisplayName("campo OTP ausente (credencial não enviada) → NÃO autentica, re-desafia")
    void otpAusente_naoAutentica() {
        codigoAtivo("123456", "0");
        // form vazio: getFirst("otp") == null

        authenticator.action(ctx);

        verify(ctx, never()).success();
        verify(ctx).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), any());
    }

    // ---- NEGAÇÃO: código expirado / inexistente ------------------------------------------------

    @Test
    @DisplayName("código correto porém EXPIRADO → EXPIRED_CODE e NÃO autentica")
    void codigoExpirado_naoAutentica() {
        when(authSession.getAuthNote(NOTE_CODE)).thenReturn("123456");
        when(authSession.getAuthNote(NOTE_EXPIRES)).thenReturn(String.valueOf(Time.currentTime() - 10));
        form.putSingle("otp", "123456"); // mesmo o código certo não passa se expirou

        authenticator.action(ctx);

        verify(ctx, never()).success();
        verify(ctx).failureChallenge(eq(AuthenticationFlowError.EXPIRED_CODE), any());
    }

    @Test
    @DisplayName("sem código na sessão (nada foi gerado) → EXPIRED_CODE e NÃO autentica")
    void semCodigoNaSessao_naoAutentica() {
        when(authSession.getAuthNote(NOTE_CODE)).thenReturn(null);
        form.putSingle("otp", "123456");

        authenticator.action(ctx);

        verify(ctx, never()).success();
        verify(ctx).failureChallenge(eq(AuthenticationFlowError.EXPIRED_CODE), any());
    }

    // ---- REENVIO --------------------------------------------------------------------------------

    @Test
    @DisplayName("reenvio: gera um código NOVO (6 dígitos) e re-desafia, sem autenticar")
    void reenvio_geraNovoCodigo() throws Exception {
        // Canal EMAIL: o envio real usa o EmailSenderProvider — mockado para não fazer nada.
        org.keycloak.email.EmailSenderProvider emailSender = mock(org.keycloak.email.EmailSenderProvider.class);
        org.keycloak.models.KeycloakSession session = mock(org.keycloak.models.KeycloakSession.class);
        org.keycloak.models.RealmModel realm = mock(org.keycloak.models.RealmModel.class);
        when(ctx.getSession()).thenReturn(session);
        when(ctx.getRealm()).thenReturn(realm);
        when(session.getProvider(org.keycloak.email.EmailSenderProvider.class)).thenReturn(emailSender);
        when(user.getEmail()).thenReturn("dr@example.com");

        form.putSingle("resend", "");

        authenticator.action(ctx);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(authSession).setAuthNote(eq(NOTE_CODE), codeCaptor.capture());
        assertTrue(codeCaptor.getValue().matches("\\d{6}"), "código deve ter 6 dígitos");
        verify(ctx).challenge(any());
        verify(ctx, never()).success();
    }
}

package com.portaltelemedicina.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Montagem da mensagem por canal ({@link OtpCodeAuthenticator.Channel}). Verifica que o código entra
 * no corpo certo e que o SMS chama o Twilio com a mensagem esperada — <b>sem tocar a rede</b>: o
 * {@link EmailSenderProvider} é mockado e o {@link TwilioSms} é interceptado com {@code mockStatic}.
 */
class OtpCodeChannelTest {

    // ---- destino: de onde sai o alvo do envio --------------------------------------------------

    @Test
    @DisplayName("EMAIL.destino = e-mail do usuário; SMS.destino = atributo telefone")
    void destinoPorCanal() {
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn("dr@example.com");
        when(user.getFirstAttribute(IdentidadeResolver.ATTR_TELEFONE)).thenReturn("11999998888");

        assertEquals("dr@example.com", OtpCodeAuthenticator.Channel.EMAIL.destino(user));
        assertEquals("11999998888", OtpCodeAuthenticator.Channel.SMS.destino(user));
    }

    // ---- EMAIL: corpo do e-mail contém o código ------------------------------------------------

    @Test
    @DisplayName("EMAIL.enviar monta assunto/corpo com o código e usa o SMTP config do realm")
    void emailMontaCorpoComCodigo() throws Exception {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        EmailSenderProvider sender = mock(EmailSenderProvider.class);
        Map<String, String> smtp = Map.of("host", "smtp.local");
        when(session.getProvider(EmailSenderProvider.class)).thenReturn(sender);
        when(realm.getSmtpConfig()).thenReturn(smtp);

        OtpCodeAuthenticator.Channel.EMAIL.enviar(session, realm, user, "654321");

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq(smtp), eq(user), subject.capture(), texto.capture(), html.capture());

        assertTrue(subject.getValue().toLowerCase().contains("código"), "assunto deve citar 'código'");
        assertTrue(texto.getValue().contains("654321"), "texto deve conter o código");
        assertTrue(html.getValue().contains("654321"), "html deve conter o código");
    }

    // ---- SMS: mensagem passada ao Twilio (sem rede, via mockStatic) ----------------------------

    @Test
    @DisplayName("SMS.enviar chama TwilioSms.enviar com o telefone e a mensagem exata (sem rede)")
    void smsMontaMensagemEChamaTwilio() throws Exception {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        when(user.getFirstAttribute(IdentidadeResolver.ATTR_TELEFONE)).thenReturn("11999998888");

        try (MockedStatic<TwilioSms> twilio = mockStatic(TwilioSms.class)) {
            OtpCodeAuthenticator.Channel.SMS.enviar(session, realm, user, "654321");

            ArgumentCaptor<String> tel = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            twilio.verify(() -> TwilioSms.enviar(tel.capture(), msg.capture()));

            assertEquals("11999998888", tel.getValue());
            assertEquals("Portal Telemedicina: seu codigo de acesso e 654321 (vale 5 min).", msg.getValue());
        }
    }
}

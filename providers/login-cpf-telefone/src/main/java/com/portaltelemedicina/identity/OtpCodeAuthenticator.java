package com.portaltelemedicina.identity;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.security.SecureRandom;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Login por <b>código de uso único</b> (OTP) — <b>modo DEV</b> (SPEC-OTP-DEV).
 *
 * <p>É um fator <b>alternativo</b> à senha (login passwordless opcional), não um 2º fator. O usuário
 * já foi resolvido na 1ª etapa ({@link UsernameFormCpfTelefone}); aqui geramos um código numérico de
 * {@value #CODE_LENGTH} dígitos, válido por {@value #TTL_SECONDS}s, com até {@value #MAX_ATTEMPTS}
 * tentativas. O canal (e-mail ou SMS) é fixado pela fábrica que instancia este authenticator, então
 * cada canal aparece como uma opção própria no "tentar outra forma".</p>
 *
 * <p><b>DEV:</b> o código é apenas <b>escrito no log</b> — não há envio real (SMTP/SMS fica para uma
 * entrega futura). Nunca expomos o código em resposta HTTP.</p>
 */
public class OtpCodeAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(OtpCodeAuthenticator.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    static final int CODE_LENGTH = 6;
    static final int TTL_SECONDS = 5 * 60;   // 5 minutos
    static final int MAX_ATTEMPTS = 5;

    private static final String TEMPLATE = "otp-code-dev.ftl";
    static final String FIELD_OTP = "otp";
    static final String FIELD_RESEND = "resend";

    // Notas guardadas na sessão de autenticação (efêmeras, por tentativa de login).
    private static final String NOTE_CODE = "OTP_CODE";
    private static final String NOTE_EXPIRES = "OTP_EXPIRES_AT";   // epoch segundos
    private static final String NOTE_ATTEMPTS = "OTP_ATTEMPTS";

    private final Channel channel;

    public OtpCodeAuthenticator(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String destino = channel.destino(context.getUser());
        if (destino == null || destino.isBlank()) {
            // Sem canal cadastrado → não dá para usar este caminho; deixa o flow seguir nas alternativas.
            context.attempted();
            return;
        }
        gerarEEnviar(context, destino);
        context.challenge(montarForm(context, null));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> form = context.getHttpRequest().getDecodedFormParameters();

        // Reenvio: gera um código novo e mostra o form de novo.
        if (form.containsKey(FIELD_RESEND)) {
            String destino = channel.destino(context.getUser());
            gerarEEnviar(context, destino);
            context.challenge(montarForm(context, null));
            return;
        }

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String esperado = authSession.getAuthNote(NOTE_CODE);
        String expiraEm = authSession.getAuthNote(NOTE_EXPIRES);
        String digitado = form.getFirst(FIELD_OTP);

        // Expirado (ou sem código na sessão) → recusa, oferecendo gerar outro.
        if (esperado == null || expiraEm == null || Time.currentTime() > Long.parseLong(expiraEm)) {
            context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
                    montarForm(context, new FormMessage(FIELD_OTP, "otpCodeExpired")));
            return;
        }

        int tentativas = lerTentativas(authSession) + 1;
        authSession.setAuthNote(NOTE_ATTEMPTS, String.valueOf(tentativas));

        if (digitado != null && esperado.equals(digitado.trim())) {
            limpar(authSession);
            context.success();
            return;
        }

        if (tentativas >= MAX_ATTEMPTS) {
            limpar(authSession);
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
            return;
        }

        context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                montarForm(context, new FormMessage(FIELD_OTP, "otpCodeInvalid")));
    }

    /** Gera um código novo, zera as tentativas e "envia" (DEV: log). */
    private void gerarEEnviar(AuthenticationFlowContext context, String destino) {
        String codigo = String.format("%0" + CODE_LENGTH + "d", RANDOM.nextInt(1_000_000));
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        authSession.setAuthNote(NOTE_CODE, codigo);
        authSession.setAuthNote(NOTE_EXPIRES, String.valueOf(Time.currentTime() + TTL_SECONDS));
        authSession.setAuthNote(NOTE_ATTEMPTS, "0");

        UserModel user = context.getUser();
        // DEV: sem envio real. O código vai SÓ para o log do servidor (nunca para a resposta HTTP).
        LOG.infof("[OTP-DEV] codigo de login para '%s' via %s (%s): %s  (valido por %ds)",
                user.getUsername(), channel.label, mascarar(destino), codigo, TTL_SECONDS);
    }

    private Response montarForm(AuthenticationFlowContext context, FormMessage erro) {
        var forms = context.form().setAttribute("otpChannelLabel", channel.label);
        if (erro != null) {
            forms.addError(erro);
        }
        return forms.createForm(TEMPLATE);
    }

    private static int lerTentativas(AuthenticationSessionModel authSession) {
        String n = authSession.getAuthNote(NOTE_ATTEMPTS);
        try {
            return n == null ? 0 : Integer.parseInt(n);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void limpar(AuthenticationSessionModel authSession) {
        authSession.removeAuthNote(NOTE_CODE);
        authSession.removeAuthNote(NOTE_EXPIRES);
        authSession.removeAuthNote(NOTE_ATTEMPTS);
    }

    /** Mascara o destino no log para não jogar e-mail/telefone completos em texto puro (LGPD). */
    private static String mascarar(String destino) {
        if (destino.contains("@")) {
            int at = destino.indexOf('@');
            String nome = destino.substring(0, at);
            String visivel = nome.length() <= 2 ? nome.substring(0, 1) : nome.substring(0, 2);
            return visivel + "***" + destino.substring(at);
        }
        return destino.length() <= 4 ? "****" : "****" + destino.substring(destino.length() - 4);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // Só aparece como opção se o usuário tem o canal cadastrado.
        String destino = channel.destino(user);
        return destino != null && !destino.isBlank();
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Nada a configurar no usuário — o código é por-sessão, não uma credencial persistida.
    }

    @Override
    public void close() {
    }

    /** Canal de entrega do código. O destino sai dos dados do próprio usuário. */
    public enum Channel {
        EMAIL("e-mail") {
            @Override
            String destino(UserModel user) {
                return user.getEmail();
            }
        },
        SMS("SMS") {
            @Override
            String destino(UserModel user) {
                return user.getFirstAttribute(IdentidadeResolver.ATTR_TELEFONE);
            }
        };

        final String label;

        Channel(String label) {
            this.label = label;
        }

        abstract String destino(UserModel user);
    }
}

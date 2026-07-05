package com.portaltelemedicina.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes puros do {@link TwilioSms}. Foco: normalização E.164 do telefone (determinística) e o
 * <b>guard de credencial ausente</b> — que garante que NÃO tentamos rede sem Twilio configurado.
 *
 * <p>Nenhum teste aqui faz chamada de rede real: o único caminho até o HTTP é bloqueado por uma
 * precondição ({@code assumeTrue}) de que o ambiente NÃO tem credenciais Twilio.</p>
 */
class TwilioSmsTest {

    // ---- e164(): normalização para E.164 (Brasil +55 por padrão) --------------------------------

    @Test
    @DisplayName("e164: null vira string vazia (não NPE)")
    void e164_null() {
        assertEquals("", TwilioSms.e164(null));
    }

    @Test
    @DisplayName("e164: já com + é respeitado como está (trim)")
    void e164_jaComMais() {
        assertEquals("+5511999998888", TwilioSms.e164("+5511999998888"));
        assertEquals("+5511999998888", TwilioSms.e164("  +5511999998888  "));
    }

    @Test
    @DisplayName("e164: só dígitos começando com 55 recebe o + na frente")
    void e164_comCodigoPais() {
        assertEquals("+5511999998888", TwilioSms.e164("5511999998888"));
    }

    @Test
    @DisplayName("e164: número nacional (sem 55) ganha +55")
    void e164_nacional() {
        assertEquals("+5511999998888", TwilioSms.e164("11999998888"));
    }

    @Test
    @DisplayName("e164: entrada formatada tem símbolos removidos antes de normalizar")
    void e164_formatado() {
        assertEquals("+5511999998888", TwilioSms.e164("(11) 99999-8888"));
        assertEquals("+5511999998888", TwilioSms.e164("11 99999.8888"));
    }

    @Test
    @DisplayName("e164: string vazia/só símbolos degrada para +55 (documenta o comportamento atual)")
    void e164_vazioOuLixo() {
        assertEquals("+55", TwilioSms.e164(""));
        assertEquals("+55", TwilioSms.e164("---"));
    }

    // ---- guard de credencial ausente: sem TWILIO_* configurado, enviar LANÇA (e não chama a rede) ----

    @Test
    @DisplayName("enviar sem credencial configurada lança IllegalStateException ANTES de qualquer rede")
    void enviar_semCredencial_lanca() {
        // Garante que NÃO há credenciais no ambiente: assim a exceção é levantada no guard, muito
        // antes do POST HTTP. Se por acaso o ambiente tiver Twilio, pulamos (não queremos rede real).
        assumeTrue(!TwilioSms.configurado(),
                "ambiente tem Twilio configurado — pulando para não fazer chamada de rede real");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> TwilioSms.enviar("11999998888", "Portal Telemedicina: seu codigo de acesso e 123456 (vale 5 min)."));
        assertTrue(ex.getMessage().toLowerCase().contains("twilio"),
                "mensagem deve indicar que o Twilio não está configurado");
    }

    @Test
    @DisplayName("configurado(): false quando as TWILIO_* não estão no ambiente")
    void configurado_semEnv() {
        assumeTrue(System.getenv("TWILIO_ACCOUNT_SID") == null
                        && System.getenv("TWILIO_AUTH_TOKEN") == null
                        && System.getenv("TWILIO_FROM") == null,
                "ambiente tem alguma TWILIO_* setada — assunção não vale aqui");
        assertTrue(!TwilioSms.configurado());
    }
}

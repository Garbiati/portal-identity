package com.portaltelemedicina.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LGPD: o {@code mascarar} evita jogar e-mail/telefone completos no log (só é usado em DEV para
 * conferência). É {@code private static} e puro — testamos via reflexão, sem alterar produção.
 */
class OtpMascararTest {

    private static Method mascarar;

    @BeforeAll
    static void findMethod() throws Exception {
        mascarar = OtpCodeAuthenticator.class.getDeclaredMethod("mascarar", String.class);
        mascarar.setAccessible(true);
    }

    private static String call(String in) throws Exception {
        return (String) mascarar.invoke(null, in);
    }

    @Test
    @DisplayName("null vira '?'")
    void nulo() throws Exception {
        assertEquals("?", call(null));
    }

    @Test
    @DisplayName("e-mail: mostra até 2 primeiros do nome e o domínio, resto escondido")
    void email() throws Exception {
        assertEquals("jo***@example.com", call("joao@example.com"));
    }

    @Test
    @DisplayName("e-mail de nome curto: mostra 1 caractere")
    void emailNomeCurto() throws Exception {
        assertEquals("a***@b.com", call("a@b.com"));
    }

    @Test
    @DisplayName("telefone: só os 4 últimos dígitos ficam visíveis")
    void telefone() throws Exception {
        assertEquals("****8888", call("11999998888"));
    }

    @Test
    @DisplayName("destino curto (<= 4) é totalmente mascarado")
    void curto() throws Exception {
        assertEquals("****", call("123"));
    }
}

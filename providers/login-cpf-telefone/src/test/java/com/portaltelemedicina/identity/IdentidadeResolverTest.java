package com.portaltelemedicina.identity;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Testes de {@link IdentidadeResolver#resolver}: o roteamento de identidade (username, e-mail, CPF,
 * telefone). É pura lógica de resolução — não há dígito verificador de CPF no código de produção
 * (a busca é feita só por dígitos), então testamos os <b>ramos reais</b> e as guardas (login vazio,
 * lixo curto, e-mail desabilitado). Todo o {@link KeycloakSession} é mockado — zero acesso a banco.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IdentidadeResolverTest {

    @Mock KeycloakSession session;
    @Mock RealmModel realm;
    @Mock UserProvider users;
    @Mock UserModel user;

    @BeforeEach
    void setUp() {
        lenient().when(session.users()).thenReturn(users);
    }

    @Test
    @DisplayName("login null retorna null e nem toca a sessão")
    void loginNull() {
        assertNull(IdentidadeResolver.resolver(session, realm, null));
        verify(session, never()).users();
    }

    @Test
    @DisplayName("login em branco retorna null")
    void loginBranco() {
        assertNull(IdentidadeResolver.resolver(session, realm, "   "));
        verify(session, never()).users();
    }

    @Test
    @DisplayName("username: casa direto pelo username (com trim)")
    void porUsername() {
        when(users.getUserByUsername(realm, "joao")).thenReturn(user);
        assertSame(user, IdentidadeResolver.resolver(session, realm, "  joao  "));
        verify(users).getUserByUsername(realm, "joao");
    }

    @Test
    @DisplayName("e-mail: quando permitido e tem @, cai no getUserByEmail")
    void porEmail() {
        when(users.getUserByUsername(realm, "a@b.com")).thenReturn(null);
        when(realm.isLoginWithEmailAllowed()).thenReturn(true);
        when(users.getUserByEmail(realm, "a@b.com")).thenReturn(user);
        assertSame(user, IdentidadeResolver.resolver(session, realm, "a@b.com"));
        verify(users).getUserByEmail(realm, "a@b.com");
    }

    @Test
    @DisplayName("e-mail desabilitado no realm: não consulta por e-mail e não acha (sem dígitos suficientes)")
    void emailDesabilitado() {
        when(users.getUserByUsername(realm, "a@b.com")).thenReturn(null);
        when(realm.isLoginWithEmailAllowed()).thenReturn(false);
        assertNull(IdentidadeResolver.resolver(session, realm, "a@b.com"));
        verify(users, never()).getUserByEmail(any(), anyString());
        // "a@b.com" não tem >= 8 dígitos → nem tenta busca por atributo.
        verify(users, never()).searchForUserByUserAttributeStream(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("CPF: identificador formatado é buscado por DÍGITOS no atributo cpf")
    void porCpf() {
        when(users.getUserByUsername(any(), anyString())).thenReturn(null);
        when(users.searchForUserByUserAttributeStream(eq(realm), eq(IdentidadeResolver.ATTR_CPF), eq("52998224725")))
                .thenAnswer(inv -> Stream.of(user));
        assertSame(user, IdentidadeResolver.resolver(session, realm, "529.982.247-25"));
        verify(users).searchForUserByUserAttributeStream(realm, IdentidadeResolver.ATTR_CPF, "52998224725");
    }

    @Test
    @DisplayName("telefone: quando o CPF não bate, tenta o atributo telefone (só dígitos)")
    void porTelefone() {
        when(users.getUserByUsername(any(), anyString())).thenReturn(null);
        when(users.searchForUserByUserAttributeStream(eq(realm), eq(IdentidadeResolver.ATTR_CPF), anyString()))
                .thenAnswer(inv -> Stream.empty());
        when(users.searchForUserByUserAttributeStream(eq(realm), eq(IdentidadeResolver.ATTR_TELEFONE), eq("11999998888")))
                .thenAnswer(inv -> Stream.of(user));
        assertSame(user, IdentidadeResolver.resolver(session, realm, "(11) 99999-8888"));
        verify(users).searchForUserByUserAttributeStream(realm, IdentidadeResolver.ATTR_TELEFONE, "11999998888");
    }

    @Test
    @DisplayName("lixo curto (< 8 dígitos) não dispara busca por atributo — evita lookup com sujeira")
    void lixoCurtoNaoBusca() {
        when(users.getUserByUsername(any(), anyString())).thenReturn(null);
        assertNull(IdentidadeResolver.resolver(session, realm, "12345"));
        verify(users, never()).searchForUserByUserAttributeStream(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("nada bate: retorna null mesmo tendo tentado CPF e telefone")
    void nadaBate() {
        when(users.getUserByUsername(any(), anyString())).thenReturn(null);
        when(users.searchForUserByUserAttributeStream(any(), anyString(), anyString()))
                .thenAnswer(inv -> Stream.empty());
        assertNull(IdentidadeResolver.resolver(session, realm, "00000000000"));
        verify(users).searchForUserByUserAttributeStream(realm, IdentidadeResolver.ATTR_CPF, "00000000000");
        verify(users).searchForUserByUserAttributeStream(realm, IdentidadeResolver.ATTR_TELEFONE, "00000000000");
    }
}

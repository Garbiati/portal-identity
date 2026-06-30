package com.portaltelemedicina.identity;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Envio de SMS via <b>Twilio</b> (REST API). Credenciais lidas do ambiente (nunca do git):
 * {@code TWILIO_ACCOUNT_SID}, {@code TWILIO_AUTH_TOKEN}, {@code TWILIO_FROM}. Em DEV vêm do
 * {@code .env} (compose); em prod, do Secret Manager.
 */
final class TwilioSms {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private TwilioSms() {
    }

    /** Há credenciais Twilio no ambiente? (usado p/ só oferecer o canal SMS quando dá pra enviar). */
    static boolean configurado() {
        return !env("TWILIO_ACCOUNT_SID").isBlank()
                && !env("TWILIO_AUTH_TOKEN").isBlank()
                && !env("TWILIO_FROM").isBlank();
    }

    /** Envia o SMS. Lança em qualquer falha (sem credencial, erro de rede, HTTP >= 300). */
    static void enviar(String telefone, String mensagem) throws Exception {
        String sid = env("TWILIO_ACCOUNT_SID");
        String token = env("TWILIO_AUTH_TOKEN");
        String from = env("TWILIO_FROM");
        if (sid.isBlank() || token.isBlank() || from.isBlank()) {
            throw new IllegalStateException("Twilio não configurado (TWILIO_ACCOUNT_SID/AUTH_TOKEN/FROM ausentes)");
        }

        String corpo = "To=" + enc(e164(telefone)) + "&From=" + enc(from) + "&Body=" + enc(mensagem);
        String auth = Base64.getEncoder()
                .encodeToString((sid + ":" + token).getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + sid + "/Messages.json"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new IllegalStateException("Twilio respondeu HTTP " + resp.statusCode() + ": " + resp.body());
        }
    }

    /**
     * Normaliza o telefone para E.164. O atributo {@code telefone} é gravado só com dígitos (I-002);
     * assumimos <b>Brasil (+55)</b> quando não há código de país. Já com "+" é respeitado como está.
     */
    static String e164(String telefone) {
        if (telefone == null) {
            return "";
        }
        String t = telefone.trim();
        if (t.startsWith("+")) {
            return t;
        }
        String d = t.replaceAll("\\D", "");
        if (d.startsWith("55")) {
            return "+" + d;
        }
        return "+55" + d;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String env(String name) {
        String v = System.getenv(name);
        return v == null ? "" : v.trim();
    }
}

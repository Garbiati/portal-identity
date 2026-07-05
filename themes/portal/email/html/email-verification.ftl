<#--
  VERIFICAÇÃO DE E-MAIL (confirmar que o endereço é do usuário).
  Variáveis: ${link}, ${linkExpiration}, ${linkExpirationFormatter(linkExpiration)}.
-->
<#import "template.ftl" as layout>
<#assign greetName = (user.firstName)!(firstName!'')>
<@layout.emailLayout preheader=msg("emailVerificationPreheader")>

<h1 style="margin:0 0 18px 0; font-size:22px; line-height:1.3; color:#054671; font-weight:700; font-family:'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">Confirme seu e-mail</h1>

<p style="margin:0 0 16px 0;"><#if greetName?has_content>Olá, ${greetName},<#else>Olá,</#if></p>

<p style="margin:0 0 18px 0;">Para concluir a configuração do seu acesso ao <strong>doc hub</strong>, confirme que este é o seu endereço de e-mail clicando no botão abaixo.</p>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 26px 0;">
  <tr><td style="background-color:#F4F7FA; border-left:4px solid #0073BD; border-radius:6px; padding:14px 16px; font-size:14px; line-height:1.55; color:#51606E;">${msg("productDescription")}</td></tr>
</table>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 24px 0;">
  <tr><td align="center">
    <table role="presentation" cellpadding="0" cellspacing="0" border="0">
      <tr><td align="center" bgcolor="#0073BD" style="border-radius:8px;">
        <a href="${link}" target="_blank" style="display:inline-block; padding:14px 34px; font-family:'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size:16px; font-weight:600; line-height:1.2; color:#ffffff; text-decoration:none; border-radius:8px;">Confirmar e-mail</a>
      </td></tr>
    </table>
  </td></tr>
</table>

<p style="margin:0 0 6px 0; font-size:14px; color:#51606E;">Se o botão não funcionar, copie e cole este endereço no seu navegador:</p>
<p style="margin:0 0 22px 0; font-size:13px; line-height:1.5; word-break:break-all;"><a href="${link}" target="_blank" style="color:#0073BD; text-decoration:underline;">${link}</a></p>

<p style="margin:0 0 16px 0; font-size:14px; color:#51606E;">Por segurança, este link expira em <strong>${linkExpirationFormatter(linkExpiration)}</strong>.</p>

<p style="margin:0; font-size:14px; color:#51606E;">Se você não reconhece este e-mail, pode ignorá-lo com segurança.</p>

</@layout.emailLayout>

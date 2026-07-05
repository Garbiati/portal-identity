<#--
  LAYOUT compartilhado dos e-mails do doc hub (D-074).
  - Table-based + CSS 100% INLINE (clientes de e-mail descartam <style>/CSS externo).
  - Largura fixa 600px, responsivo simples (max-width + width:100% nos elementos internos).
  - Sem imagens (marca em texto/HTML) → menos peso, menos chance de cair no spam.
  - Cores da marca D-074: navy #054671 · azul #0073BD · âmbar #F07A1E (versão clara p/ contraste no navy).
  Uso: os templates filhos importam este arquivo e chamam <@layout.emailLayout preheader="..."> ... </@>.
-->
<#macro emailLayout preheader="">
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="pt-BR">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<meta http-equiv="X-UA-Compatible" content="IE=edge"/>
<meta name="color-scheme" content="light"/>
<title>${msg("brandName")}</title>
</head>
<body style="margin:0; padding:0; width:100%; background-color:#F4F7FA; -webkit-text-size-adjust:100%; -ms-text-size-adjust:100%;">

<#-- Preheader: texto de pré-visualização da caixa de entrada. Oculto no corpo do e-mail. -->
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#F4F7FA; opacity:0;">
${preheader}&#847;&zwnj;&nbsp;&#847;&zwnj;&nbsp;&#847;&zwnj;&nbsp;&#847;&zwnj;&nbsp;&#847;&zwnj;&nbsp;&#847;&zwnj;&nbsp;
</div>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#F4F7FA;">
  <tr>
    <td align="center" style="padding:24px 12px;">

      <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:600px; max-width:600px; background-color:#ffffff; border:1px solid #D5DEE6; border-radius:14px; overflow:hidden; font-family:'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">

        <#-- Cabeçalho / marca. NOTE(marca): "doc hub" estilizado em duas cores direto aqui. -->
        <tr>
          <td style="background-color:#054671; padding:26px 40px;">
            <span style="font-size:26px; font-weight:700; letter-spacing:-0.4px; color:#ffffff; font-family:'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">doc&nbsp;<span style="color:#F07A1E;">hub</span></span>
            <div style="margin-top:5px; font-size:13px; color:#AFCBE0;">por ${msg("companyName")}</div>
          </td>
        </tr>

        <#-- Faixa fina de acento (azul da marca). -->
        <tr><td style="height:4px; line-height:4px; font-size:0; background-color:#0073BD;">&nbsp;</td></tr>

        <#-- Corpo (conteúdo específico de cada e-mail). -->
        <tr>
          <td style="padding:36px 40px 12px 40px; color:#1B2733; font-size:16px; line-height:1.55; font-family:'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
            <#nested>
          </td>
        </tr>

        <#-- Rodapé / assinatura da empresa. Dados reais vêm dos PLACEHOLDERS em messages_pt_BR.properties. -->
        <tr>
          <td style="padding:22px 40px 30px 40px; border-top:1px solid #E4EAF0; color:#51606E; font-size:12px; line-height:1.65; font-family:'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
            <strong style="color:#054671;">${msg("companyName")}</strong><br/>
            CNPJ ${msg("companyCnpj")} &nbsp;&middot;&nbsp; ${msg("companyAddress")}<br/>
            Suporte: <a href="mailto:${msg("supportEmail")}" style="color:#0073BD; text-decoration:none;">${msg("supportEmail")}</a>
            &nbsp;&middot;&nbsp;
            <a href="${msg("companySite")}" style="color:#0073BD; text-decoration:none;">${msg("companySite")}</a>
            <div style="margin-top:14px; color:#8895A2;">${msg("footerAutomated")}</div>
          </td>
        </tr>

      </table>

    </td>
  </tr>
</table>
</body>
</html>
</#macro>

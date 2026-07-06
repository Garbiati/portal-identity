<#ftl output_format="plainText">
<#assign greetName = (user.firstName)!(firstName!'')>
<#if greetName?has_content>Olá, ${greetName}!<#else>Olá!</#if>

Confirme seu e-mail — Doctor Hub

Para concluir a configuração do seu acesso ao Doctor Hub, confirme que este é o seu endereço de e-mail acessando o link abaixo:

${link}

${msg("productDescription")}

Por segurança, este link expira em ${linkExpirationFormatter(linkExpiration)}.

Se você não reconhece este e-mail, pode ignorá-lo com segurança.

--
${msg("companyName")}
CNPJ ${msg("companyCnpj")} · ${msg("companyAddress")}
Suporte: ${msg("supportEmail")} · ${msg("companySite")}
${msg("footerAutomated")}

<#ftl output_format="plainText">
<#assign greetName = (user.firstName)!(firstName!'')>
<#if greetName?has_content>Olá, ${greetName}!<#else>Olá!</#if>

Bem-vindo(a) ao Doctor Hub.

Você está recebendo este e-mail porque um administrador criou o seu acesso ao Doctor Hub. Para começar a usar, defina a sua senha neste endereço:

${link}

${msg("productDescription")}

Por segurança, este link expira em ${linkExpirationFormatter(linkExpiration)}. Se ele expirar, peça um novo convite ao administrador.

Se você não esperava este convite, pode ignorar este e-mail com segurança — nada será alterado.

--
${msg("companyName")}
CNPJ ${msg("companyCnpj")} · ${msg("companyAddress")}
Suporte: ${msg("supportEmail")} · ${msg("companySite")}
${msg("footerAutomated")}

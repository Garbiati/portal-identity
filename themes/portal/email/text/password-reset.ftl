<#ftl output_format="plainText">
<#assign greetName = (user.firstName)!(firstName!'')>
<#if greetName?has_content>Olá, ${greetName}!<#else>Olá!</#if>

Redefinição de senha — Doctor Hub

Recebemos um pedido para redefinir a senha da sua conta no Doctor Hub. Se foi você, crie uma nova senha neste endereço:

${link}

${msg("productDescription")}

Por segurança, este link expira em ${linkExpirationFormatter(linkExpiration)}.

Se você não pediu para redefinir sua senha, ignore este e-mail — sua senha atual continua valendo e nada será alterado.

--
${msg("companyName")}
CNPJ ${msg("companyCnpj")} · ${msg("companyAddress")}
Suporte: ${msg("supportEmail")} · ${msg("companySite")}
${msg("footerAutomated")}

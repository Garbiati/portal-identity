<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        ${msg("otpCodeTitle")}
    <#elseif section = "form">
        <p id="kc-otp-instruction">${msg("otpCodeInstruction", (otpChannelLabel!""))}</p>

        <form id="kc-otp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="otp" class="${properties.kcLabelClass!}">${msg("otpCodeLabel")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input tabindex="1" id="otp" name="otp" type="text"
                           inputmode="numeric" autocomplete="one-time-code" autofocus
                           class="${properties.kcInputClass!}"/>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input tabindex="2" type="submit"
                           class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           value="${msg("doLogIn")}"/>
                </div>
            </div>
        </form>

        <form id="kc-otp-resend-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" name="resend" value="true"/>
            <div class="${properties.kcFormGroupClass!}">
                <input tabindex="3" type="submit"
                       class="${properties.kcButtonClass!} ${properties.kcButtonSecondaryClass!} ${properties.kcButtonBlockClass!}"
                       value="${msg("otpCodeResend")}"/>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>

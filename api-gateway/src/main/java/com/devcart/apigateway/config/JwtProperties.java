package com.devcart.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de validacao e mapeamento do JWT aceito pelo gateway.
 * Prefixo: {@code security.jwt}.
 */
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * Segredo compartilhado (HMAC / HS256) usado para validar a assinatura do token.
     * Deve ter no minimo 32 bytes. Em producao, injete via variavel de ambiente.
     */
    private String secret;

    /** Claim que carrega o identificador do usuario. Propagada no header X-User-Id. */
    private String userIdClaim = "sub";

    /** Claim (lista de strings) que carrega os papeis/roles do usuario. */
    private String rolesClaim = "roles";

    /** Issuer esperado. Se vazio, a validacao de issuer nao e aplicada. */
    private String issuer;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getUserIdClaim() {
        return userIdClaim;
    }

    public void setUserIdClaim(String userIdClaim) {
        this.userIdClaim = userIdClaim;
    }

    public String getRolesClaim() {
        return rolesClaim;
    }

    public void setRolesClaim(String rolesClaim) {
        this.rolesClaim = rolesClaim;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}

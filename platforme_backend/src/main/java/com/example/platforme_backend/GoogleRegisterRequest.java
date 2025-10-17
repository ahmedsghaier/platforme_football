package com.example.platforme_backend;

class GoogleRegisterRequest {
    private String token;
    private String accountType;
    private String organization;
    private boolean newsletter;
    private boolean acceptTerms;

    public GoogleRegisterRequest() {}

    public GoogleRegisterRequest(String token, String accountType, String organization,
                                 boolean newsletter, boolean acceptTerms) {
        this.token = token;
        this.accountType = accountType;
        this.organization = organization;
        this.newsletter = newsletter;
        this.acceptTerms = acceptTerms;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public boolean isNewsletter() { return newsletter; }
    public void setNewsletter(boolean newsletter) { this.newsletter = newsletter; }
    public boolean isAcceptTerms() { return acceptTerms; }
    public void setAcceptTerms(boolean acceptTerms) { this.acceptTerms = acceptTerms; }
}
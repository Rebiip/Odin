package configurations.exceptions.tags;

public enum EnTenancyExceptionTag implements IExceptionTag {

    TENANT_ID_MISSING("TENANCY.ERRORS.TENANT_ID_MISSING", "MISSING tenant_id claim"),
    TENANT_ID_INVALID("TENANCY.ERRORS.TENANT_ID_INVALID", "INVALID tenant_id claim");

    private final String i18n;
    private final String description;

    EnTenancyExceptionTag(String i18n, String description) {
        this.i18n = i18n;
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getI18n() {
        return i18n;
    }
}

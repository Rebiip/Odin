package configurations.exceptions.tags;

/**
 * @author Alexandre Marinho de Souza Júnior on 24/06/2022
 */
public enum EnCommonExceptionTag implements IExceptionTag {

    DONT_HAS_ROLE_TO_PERFORM_THIS_ACTION("COMMON.ERRORS.DONT_HAS_ROLE_TO_PERFORM_THIS_ACTION", "USER_WITHOUT_PERMISSION_TO_PERFORM_THE_REQUEST_ACTION"),
    NO_OBJECT_SENT("COMMON.ERRORS.NO_OBJECT_SENT",
            "O objeto a ser cadastrado não foi enviado, por favor envie conforme especificações, caso o erro" +
                    " persista entre em contato com o nosso time de experiência do cliente!"),
    OBJECT_ALREADY_EXISTS("COMMON.ERRORS.OBJECT_ALREADY_EXISTS", "THE OBJECT THAT YOU ARE TRYING TO CREATE ALREADY EXISTS"),
    START_AFTER_END("COMMON.ERRORS.START_AFTER_END", "THE START DATE OF THE INTERVAL IS AFTER THE END DATE"),
    INVALID_ID_INFORMED("COMMON.ERRORS.INVALID_ID_INFORMED", "THE INFORMED ID ISN'T A VALID ID"),
    OBJECT_NOT_FOUND("COMMON.ERRORS.OBJECT_NOT_FOUND", "THE OBJECT THAT YOU ARE TRYING TO GET DOESN'T EXISTS"),
    JSON_PROCESSING_EXCEPTION("COMMON.ERRORS.JSON_PROCESSING_EXCEPTION", "A JSON PROCESSING ERROR OCCURRED"),
    PROBLEM_LISTING_BUCKET_OBJECTS("COMMON.ERRORS.BUCKET_LISTING", "A PROBLEM OCCURRED WHILE LISTING THE BUCKET OBJECTS");


    private final String i18n;
    private final String description;

    EnCommonExceptionTag(final String i18n, final String description) {
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

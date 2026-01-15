package configurations.exceptions.dtos;


/**
 * This DTO represents the format about how the exception will be thrown.
 *
 * @param enTag        - String, the TAG that hold the error meaning.
 * @param errorMessage - String, the message describing the error.
 * @author Alexandre Marinho de Souza Júnior
 */
public record WSExceptionDTO(
        String enTag,
        String errorMessage
) {
}

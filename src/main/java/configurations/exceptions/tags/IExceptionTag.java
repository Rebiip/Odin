package configurations.exceptions.tags;

/**
 * This interface create a contract between the TagExceptions allowing it to be extensible to specific errors in each micro service
 *
 * @author Alexandre Marinho de Souza Júnior on 24/06/2022
 */
public interface IExceptionTag {

    String getDescription();
    String getI18n();

}

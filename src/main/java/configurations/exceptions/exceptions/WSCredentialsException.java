package configurations.exceptions.exceptions;


import configurations.exceptions.tags.IExceptionTag;

/**
 * This class represents exceptions related with user credentials
 * this exceptions handle the 403 http code that are closely related to business rules and access configurations.
 *
 * @author Alexandre Marinho de Souza Júnior on 04/07/2022
 */
public class WSCredentialsException extends WSException{

    public WSCredentialsException(IExceptionTag tagException) {
        super(tagException);
    }
}

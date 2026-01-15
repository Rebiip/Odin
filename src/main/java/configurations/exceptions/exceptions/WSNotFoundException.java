package configurations.exceptions.exceptions;


import configurations.exceptions.tags.IExceptionTag;

/**
 * This class represents exceptions related to object no found
 * this exceptions handle the 404 http code that are closely related to object that are not found.
 *
 * @author Alexandre Marinho de Souza Júnior on 04/07/2022
 */
public class WSNotFoundException extends WSException{

    public WSNotFoundException(IExceptionTag tagException) {
        super(tagException);
    }
}

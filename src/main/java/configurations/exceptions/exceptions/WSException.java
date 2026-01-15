package configurations.exceptions.exceptions;


import configurations.exceptions.tags.IExceptionTag;

/**
 * This class represents a most common exception that must occur, this exceptions handle the 409 http code that are
 * closely related to business rules.
 *
 * @author Alexandre Marinho de Souza Júnior on 24/06/2022
 */
public class WSException extends RuntimeException {

    private IExceptionTag tagException;

    public WSException(IExceptionTag tagException) {
        super(tagException.getDescription());
        this.tagException = tagException;
    }

    public IExceptionTag getTagException() {
        return this.tagException;
    }


}

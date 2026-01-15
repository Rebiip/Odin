package configurations.exceptions;

import application.exceptions.DownstreamRequestFailedException;
import application.exceptions.RouteNotFoundException;
import configurations.exceptions.dtos.WSExceptionDTO;
import configurations.exceptions.exceptions.WSCredentialsException;
import configurations.exceptions.exceptions.WSException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;


/**
 * This class contains all mappers to handle custom exceptions that might be thrown.
 * @author Alexandre Marinho de Souza Júnior.
 */

@Slf4j
public class ExceptionMapper {

    @ServerExceptionMapper
    public RestResponse<WSExceptionDTO> mapException(WSCredentialsException exception) {
        return RestResponse.status(
                Response.Status.FORBIDDEN,
                new WSExceptionDTO(
                        exception.getTagException().getI18n(),
                        exception.getTagException().getDescription()
                )
        );
    }

    @ServerExceptionMapper
    public RestResponse<WSExceptionDTO> mapException(WSException exception) {
        return RestResponse.status(
                Response.Status.CONFLICT,
                new WSExceptionDTO(
                        exception.getTagException().getI18n(),
                        exception.getTagException().getDescription()
                )
        );
    }

    @ServerExceptionMapper
    public RestResponse<WSExceptionDTO> mapException(RouteNotFoundException exception) {
        return RestResponse.status(
                Response.Status.NOT_FOUND,
                new WSExceptionDTO("GATEWAY.ROUTE_NOT_FOUND", exception.getMessage())
        );
    }

    @ServerExceptionMapper
    public RestResponse<WSExceptionDTO> mapException(DownstreamRequestFailedException exception) {
        log.error("Downstream request failed", exception);
        return RestResponse.status(
                Response.Status.BAD_GATEWAY,
                new WSExceptionDTO("GATEWAY.DOWNSTREAM_ERROR", exception.getMessage())
        );
    }

    @ServerExceptionMapper
    public RestResponse<WSExceptionDTO> mapException(IllegalArgumentException exception) {
        return RestResponse.status(
                Response.Status.BAD_REQUEST,
                new WSExceptionDTO("GATEWAY.BAD_REQUEST", exception.getMessage())
        );
    }

    @ServerExceptionMapper
    public RestResponse<WSExceptionDTO> mapException(Exception exception) {
        log.error("An unexpected error occurred", exception);
        return RestResponse.status(
                Response.Status.INTERNAL_SERVER_ERROR,
                new WSExceptionDTO("GATEWAY.UNEXPECTED_ERROR", "Unexpected error")
        );
    }



}

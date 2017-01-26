/*
 * This work is protected under copyright law in the Kingdom of
 * The Netherlands. The rules of the Berne Convention for the
 * Protection of Literary and Artistic Works apply.
 * Digital Me B.V. is the copyright owner.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.qiy.oic.op.api;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Make sure any exception is handled in a OAuth specified way. Please remove any default handlers and use this one
 * instead.
 *
 * @author Friso Vrolijken
 * @since 10 mei 2016
 */
@Provider
public class OAuthExceptionMapper implements ExceptionMapper<Throwable> {
    /**
     * Standard SLF4J Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        String logKey = Long.toString(ThreadLocalRandom.current().nextLong());
        LOGGER.warn("Error handling a request: {}", logKey);

        AuthenticationRequest inputs = AuthenticationRequest.fromStorage();
        ResponseBuilder result;
        if (exception instanceof InputException) {
            LOGGER.info("caught InputException, reporting error");
            // standard validation errors, report them back to the caller
            result = reportError(inputs, (InputException) exception);
        } else {
            LOGGER.warn("caught Exception, reporting error", exception);
            // we did not expect these, so do not give out information to the caller, to prevent exposing more than we
            // should.
            InputException ie = new InputException(ErrorCode.SERVER_ERROR, "Unknown server error");
            result = reportError(inputs, ie);
        }

        result.header("log-key", logKey);
        return result.build();
    }

    /**
     * Reports back errors to the caller. If the redirectURI is know and belongs to the customer, use that. If not, give
     * out a status code 400 (BAD REQUEST) and provide error details in the body
     * 
     * @param inputs
     *            the user's input, used to find the errorUri
     * @param e
     *            the error
     * @return see description
     */
    private static ResponseBuilder reportError(AuthenticationRequest inputs, InputException e) {
        if (inputs != null) {
            URI errorUri = inputs.getErrorUri(e);
            if (errorUri != null) {
                LOGGER.info("Redirecting user agent to {}", errorUri);
                return Response.seeOther(errorUri);
            }
            // else
        }
        LOGGER.info("displaying error {} / {}", e.getError(), e.getErrorDescription());
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getError().toString());
        error.put("error_description", e.getErrorDescription());
        return Response.status(Status.BAD_REQUEST).entity(error);
    }
}

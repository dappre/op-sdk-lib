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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.api.param.Prompt;
import nl.qiy.oic.op.domain.IDToken;
import nl.qiy.oic.op.domain.OAuthUser;
import nl.qiy.oic.op.service.AuthorizationFlowService;
import nl.qiy.oic.op.service.OAuthUserService;

/**
 * Handles the AuthenticationRequest as defined in the OpenId Connect spec for the
 * <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthRequest">code flow</a>, the
 * <a href="http://openid.net/specs/openid-connect-core-1_0.html#ImplicitAuthRequest">implicit flow</a> and the
 * <a href="http://openid.net/specs/openid-connect-core-1_0.html#HybridAuthRequest">hybrid flow</a>
 *
 * @author Friso Vrolijken
 * @since 25 apr. 2016
 */
@Path("")
@SuppressWarnings("ucd")
public class AuthenticationResource {
    /**
     * Standard SLF4J Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationResource.class);
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    /**
     * Authorisation request is the OAuth name. We're actually trying to authenticate the user.
     * 
     * @param ui
     *            uriInfo that contains the GET request parameters
     * @param request
     *            the request that originated this call
     * @return a (redirect to a?) page where the user can authenticate herself
     */
    @GET
    @Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
    public static Response authorizationRequest(@Context UriInfo ui, @Context HttpServletRequest request) {
        LOGGER.debug("authorizationRequest GET called");
        return handleAuthNRequest(ui.getQueryParameters(), request.getSession());
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
    public static Response authorizationRequest(MultivaluedMap<String, String> formParams,
            @Context HttpServletRequest request) {
        LOGGER.debug("authorizationRequest POST called");
        return handleAuthNRequest(formParams, request.getSession());
    }

    @Path("user-info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public static Response getUserInfo(@HeaderParam("Authorization") String bearerToken) {
        if (!bearerToken.toLowerCase().startsWith("bearer ")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        String bearerKey = bearerToken.substring(7).trim();
        IDToken idToken = OAuthUserService.getBearer(bearerKey);
        if (idToken == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        // else
        return Response.ok(idToken.getStandardClaims()).build();
    }

    /**
     * As both doGet and doPost basically do the same thing, the common behaviour lives here.
     * <ul>
     * <li>Create a {@link AuthenticationRequest} from the parameters. This will also do validation of the input.
     * <li>Handle 'business logic' to see whether or not the current user is logged in
     * <li>If the user is logged in and the prompt does not contain 'login' send the auth response
     * <li>If the user is not logged in and the prompt is 'none' send 'unauthenticated' response
     * <li>In all other cases: enter the authentication flow
     * <li>If any error happened when processing the points above: send an error back to either the redirect_uri (if
     * that's valid) or return an error response code.
     * </ul>
     * 
     * @param params
     *            the user's input
     * @param session
     *            the session belonging to the request that originated this call
     * @return see description
     */
    private static Response handleAuthNRequest(MultivaluedMap<String, String> params, @Context HttpSession session) {
        AuthenticationRequest inputs = new AuthenticationRequest(params, VALIDATOR_FACTORY.getValidator());
        LOGGER.debug("successfully parsed user input: {}", inputs);

        Optional<OAuthUser> optUser = OAuthUserService.getLoggedIn(session);

        if (needsLogout(optUser, inputs)) {
            OAuthUserService.logout(session);
            optUser = Optional.empty();
        }

        if (optUser.isPresent()) {
            // user is found, prompt does not contain login, flow ends here
            return AuthenticationResponse.getResponse(inputs, optUser.get());
        }
        // else
        if (inputs.prompt.contains(Prompt.NONE)) {
            // The Authentication Request contains the prompt parameter with the value none. In this case, the
            // Authorization Server MUST return an error if an End-User is not already Authenticated or could not be
            // silently Authenticated.
            return Response.status(Status.UNAUTHORIZED).build();
        }

        // no user found and the prompt did not contain 'none'. try to authenticate the user
        return AuthorizationFlowService.startFlow(inputs, session);
    }

    /**
     * @param optUser
     * @param inputs
     * @return true if we actively need to log out the user
     */
    private static boolean needsLogout(Optional<OAuthUser> optUser, AuthenticationRequest inputs) {
        if (!optUser.isPresent()) {
            return false;
        }
        // else
        if (inputs.prompt.contains(Prompt.LOGIN)) {
            return true;
        }
        // else
        if (inputs.maxAge != null) {
            // if maxAge is given, we need to respect it.
            Duration timeLoggedIn = Duration.between(optUser.get().getLoginTime(), Instant.now());
            boolean expired = !(timeLoggedIn.minusSeconds(inputs.maxAge).isNegative());
            if (expired) {
                return true;
            }
        }

        // Checks the following specification for a found user:
        // "If the sub (subject) Claim is requested with a specific value for the ID Token, the Authorization Server
        // MUST only send a positive response if the End-User identified by that sub value has an active session with
        // the Authorization Server or has been Authenticated as a result of the request. The Authorization Server MUST
        // NOT reply with an ID Token or Access Token for a different user, even if they have an active session with the
        // Authorization Server. Such a request can be made either using an id_token_hint parameter or by requesting a
        // specific Claim Value as described in Section 5.5.1, if the claims parameter is supported by the
        // implementation."

        // for now [20160504] no claims are inspected
        return false;

    }

    @Override
    public String toString() {
        return "This exist to keep Sonar from complaining about the lack of a private constructor";
    }

}

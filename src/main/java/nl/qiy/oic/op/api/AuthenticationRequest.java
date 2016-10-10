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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.api.param.Display;
import nl.qiy.oic.op.api.param.Flow;
import nl.qiy.oic.op.api.param.Prompt;
import nl.qiy.oic.op.api.param.ResponseMode;
import nl.qiy.oic.op.api.param.ResponseType;
import nl.qiy.oic.op.domain.OAuthClient;
import nl.qiy.oic.op.service.OAuthClientService;

/**
 * http://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
 *
 * @author Friso Vrolijken
 * @since 25 apr. 2016
 */
public class AuthenticationRequest implements Serializable {
    /**
     * Default serialVersionUID
     */
    private static final long serialVersionUID = 1L;
    /**
     * Standard SLF4J Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationRequest.class);
    private static final java.util.regex.Pattern SCOPE_PATTERN = java.util.regex.Pattern
            .compile("[\\x21-\\x7e&&[^\\x22\\x5c]]+");

    private static ThreadLocal<AuthenticationRequest> storage = new ThreadLocal<>();

    /**
     * REQUIRED. OpenID Connect requests MUST contain the openid scope value. If the openid scope value is not present,
     * the behavior is entirely unspecified. Other scope values MAY be present. Scope values used that are not
     * understood by an implementation SHOULD be ignored. See Sections 5.4 and 11 for additional scope values defined by
     * this specification.
     */
    // DO NOT CHECK WITH THE BEAN VALIDATOR FRAMEWORK; needs different error
    final Set<String> scope;

    /**
     * <dl>
     * <dt>Code flow
     * <dd>REQUIRED. OAuth 2.0 Response Type value that determines the authorization processing flow to be used,
     * including what parameters are returned from the endpoints used. When using the Authorization Code Flow, this
     * value is code.
     * <dt>Implicit flow
     * <dd>REQUIRED. OAuth 2.0 Response Type value that determines the authorization processing flow to be used,
     * including what parameters are returned from the endpoints used. When using the Implicit Flow, this value is
     * id_token token or id_token. The meanings of both of these values are defined in OAuth 2.0 Multiple Response Type
     * Encoding Practices [OAuth.Responses]. No Access Token is returned when the value is id_token.
     * 
     * NOTE: While OAuth 2.0 also defines the token Response Type value for the Implicit Flow, OpenID Connect does not
     * use this Response Type, since no ID Token would be returned.
     * <dt>Hybrid flow
     * <dd>REQUIRED. OAuth 2.0 Response Type value that determines the authorization processing flow to be used,
     * including what parameters are returned from the endpoints used. When using the Hybrid Flow, this value is code
     * id_token, code token, or code id_token token. The meanings of these values are defined in OAuth 2.0 Multiple
     * Response Type Encoding Practices [OAuth.Responses].
     * </dl>
     * 
     * (OAuth) The value of the scope parameter is expressed as a list of space-delimited, case-sensitive strings.
     * 
     * <pre>
     *   scope       = scope-token *( SP scope-token )
     *   scope-token = 1*( %x21 / %x23-5B / %x5D-7E )
     * </pre>
     * 
     */
    @NotNull
    @Size(min = 1)
    final Set<ResponseType> responseType;

    /**
     * REQUIRED. OAuth 2.0 OAuthClient Identifier valid at the Authorization Server.
     * 
     * (OAuth spec) A unique string representing the registration information provided by the client. The client
     * identifier is not a secret; it is exposed to the resource owner and MUST NOT be used alone for client
     * authentication. The client identifier is unique to the authorization server.
     */
    @NotNull
    @Pattern(regexp = "\\S+")
    final String clientId;

    /**
     * <dl>
     * <dt>Code flow, Hybrid flow
     * <dd>REQUIRED. Redirection URI to which the response will be sent. This URI MUST exactly match one of the
     * Redirection URI values for the OAuthClient pre-registered at the OpenID Provider, with the matching performed as
     * described in Section 6.2.1 of [RFC3986] (Simple String Comparison). When using this flow, the Redirection URI
     * SHOULD use the https scheme; however, it MAY use the http scheme, provided that the OAuthClient Type is confidential,
     * as defined in Section 2.1 of OAuth 2.0, and provided the OP allows the use of http Redirection URIs in this case.
     * The Redirection URI MAY use an alternate scheme, such as one that is intended to identify a callback into a
     * native application.
     * <dt>Implicit flow
     * <dd>REQUIRED. Redirection URI to which the response will be sent. This URI MUST exactly match one of the
     * Redirection URI values for the OAuthClient pre-registered at the OpenID Provider, with the matching performed as
     * described in Section 6.2.1 of [RFC3986] (Simple String Comparison). When using this flow, the Redirection URI
     * MUST NOT use the http scheme unless the OAuthClient is a native application, in which case it MAY use the http: scheme
     * with localhost as the hostname.
     * </dl>
     * 
     * (OAuth spec) The endpoint URI MUST NOT include a fragment component.
     */
    // DO NOT CHECK WITH THE BEAN VALIDATOR FRAMEWORK; needs different error
    final URI redirectUri;

    /**
     * RECOMMENDED. Opaque value used to maintain state between the request and the callback. Typically, Cross-Site
     * Request Forgery (CSRF, XSRF) mitigation is done by cryptographically binding the value of this parameter with a
     * browser cookie.
     */
    final String state;

    /**
     * OPTIONAL. Informs the Authorization Server of the mechanism to be used for returning parameters from the
     * Authorization Endpoint. This use of this parameter is NOT RECOMMENDED when the Response Mode that would be
     * requested is the default mode specified for the Response Type
     */
    final ResponseMode responseMode;

    /**
     * <dl>
     * <dt>Code flow, Hybrid flow
     * <dd>OPTIONAL. String value used to associate a OAuthClient session with an ID Token, and to mitigate replay
     * attacks. The value is passed through unmodified from the AuthenticationResource Request to the ID Token.
     * Sufficient entropy MUST be present in the nonce values used to prevent attackers from guessing values. For
     * implementation notes, see Section 15.5.2.
     * <dt>Implicit flow
     * <dd>REQUIRED. String value used to associate a OAuthClient session with an ID Token, and to mitigate replay
     * attacks. The value is passed through unmodified from the AuthenticationResource Request to the ID Token.
     * Sufficient entropy MUST be present in the nonce values used to prevent attackers from guessing values. For
     * implementation notes, see Section 15.5.2.
     * </dl>
     */
    final String nonce;

    /**
     * OPTIONAL. ASCII string value that specifies how the Authorization Server displays the authentication and consent
     * user interface pages to the End-User. The defined values are:
     * <dl>
     * <dt>page
     * <dd>The Authorization Server SHOULD display the authentication and consent UI consistent with a full User Agent
     * page view. If the display parameter is not specified, this is the default display mode.
     * <dt>popup
     * <dd>The Authorization Server SHOULD display the authentication and consent UI consistent with a popup User Agent
     * window. The popup User Agent window should be of an appropriate size for a login-focused dialog and should not
     * obscure the entire window that it is popping up over.
     * <dt>touch
     * <dd>The Authorization Server SHOULD display the authentication and consent UI consistent with a device that
     * leverages a touch interface.
     * <dt>wap
     * <dd>The Authorization Server SHOULD display the authentication and consent UI consistent with a "feature phone"
     * type display.
     * </dl>
     * The Authorization Server MAY also attempt to detect the capabilities of the User Agent and present an appropriate
     * display.
     */
    final Display display;

    /**
     * OPTIONAL. Space delimited, case sensitive list of ASCII string values that specifies whether the Authorization
     * Server prompts the End-User for reauthentication and consent. The defined values are:
     * <dl>
     * <dt>none
     * <dd>The Authorization Server MUST NOT display any authentication or consent user interface pages. An error is
     * returned if an End-User is not already authenticated or the OAuthClient does not have pre-configured consent for the
     * requested Claims or does not fulfill other conditions for processing the request. The error code will typically
     * be login_required, interaction_required, or another code defined in Section 3.1.2.6. This can be used as a method
     * to check for existing authentication and/or consent.
     * <dt>login
     * <dd>The Authorization Server SHOULD prompt the End-User for reauthentication. If it cannot reauthenticate the
     * End-User, it MUST return an error, typically login_required.
     * <dt>consent
     * <dd>The Authorization Server SHOULD prompt the End-User for consent before returning information to the OAuthClient.
     * If it cannot obtain consent, it MUST return an error, typically consent_required.
     * <dt>select_account
     * <dd>The Authorization Server SHOULD prompt the End-User to select a user account. This enables an End-User who
     * has multiple accounts at the Authorization Server to select amongst the multiple accounts that they might have
     * current sessions for. If it cannot obtain an account selection choice made by the End-User, it MUST return an
     * error, typically account_selection_required.
     * </dl>
     * The prompt parameter can be used by the OAuthClient to make sure that the End-User is still present for the current
     * session or to bring attention to the request. If this parameter contains none with any other value, an error is
     * returned.
     */
    final Set<Prompt> prompt;

    /**
     * OPTIONAL. Maximum AuthenticationResource Age. Specifies the allowable elapsed time in seconds since the last time the
     * End-User was actively authenticated by the OP. If the elapsed time is greater than this value, the OP MUST
     * attempt to actively re-authenticate the End-User. (The max_age request parameter corresponds to the OpenID 2.0
     * PAPE [OpenID.PAPE] max_auth_age request parameter.) When max_age is used, the ID Token returned MUST include an
     * auth_time Claim Value.
     */
    final Integer maxAge;

    /**
     * OPTIONAL. End-User's preferred languages and scripts for the user interface, represented as a space-separated
     * list of BCP47 [RFC5646] language tag values, ordered by preference. For instance, the value "fr-CA fr en"
     * represents a preference for French as spoken in Canada, then French (without a region designation), followed by
     * English (without a region designation). An error SHOULD NOT result if some or all of the requested locales are
     * not supported by the OpenID Provider.
     */
    final List<Locale> uiLocales;

    /**
     * OPTIONAL. ID Token previously issued by the Authorization Server being passed as a hint about the End-User's
     * current or past authenticated session with the OAuthClient. If the End-User identified by the ID Token is logged in or
     * is logged in by the request, then the Authorization Server returns a positive response; otherwise, it SHOULD
     * return an error, such as login_required. When possible, an id_token_hint SHOULD be present when prompt=none is
     * used and an invalid_request error MAY be returned if it is not; however, the server SHOULD respond successfully
     * when possible, even if it is not present. The Authorization Server need not be listed as an audience of the ID
     * Token when it is used as an id_token_hint value.
     * 
     * If the ID Token received by the RP from the OP is encrypted, to use it as an id_token_hint, the OAuthClient MUST
     * decrypt the signed ID Token contained within the encrypted ID Token. The OAuthClient MAY re-encrypt the signed ID
     * token to the AuthenticationResource Server using a key that enables the server to decrypt the ID Token, and use the
     * re-encrypted ID token as the id_token_hint value.
     */
    final String idTokenHint;

    /**
     * OPTIONAL. Hint to the Authorization Server about the login identifier the End-User might use to log in (if
     * necessary). This hint can be used by an RP if it first asks the End-User for their e-mail address (or other
     * identifier) and then wants to pass that value as a hint to the discovered authorization service. It is
     * RECOMMENDED that the hint value match the value used for discovery. This value MAY also be a phone number in the
     * format specified for the phoneNumber Claim. The use of this parameter is left to the OP's discretion.
     */
    final String loginHint;

    /**
     * OPTIONAL. Requested AuthenticationResource Context Class Reference values. Space-separated string that specifies the acr
     * values that the Authorization Server is being requested to use for processing this AuthenticationResource Request, with
     * the values appearing in order of preference. The AuthenticationResource Context Class satisfied by the authentication
     * performed is returned as the acr Claim Value, as specified in Section 2. The acr Claim is requested as a
     * Voluntary Claim by this parameter.
     */
    final List<String> acrValues;

    /**
     * the input that started everything
     */
    final transient MultivaluedMap<String, String> parameters;

    /**
     * As stated in http://openid.net/specs/openid-connect-core-1_0.html#Authentication the flow can be derived from the
     * response_type
     */
    final transient Flow flow;

    private OAuthClient client;

    /**
     * Constructor for AuthenticationRequest, that will copy all the relevant values and validate it afterwards.
     * 
     * @param validator
     *            any validator, must not be null
     * @param parameters
     *            the user's input as given by either the GET or POST request
     */
    public AuthenticationRequest(MultivaluedMap<String, String> parameters, Validator validator) { // NOSONAR
        super();
        storage.set(this);
        // paramters must be set first as all the helper functions depend on it
        this.parameters = parameters;

        // errors need to be reported to the redirect uri, so better parse that first
        this.redirectUri = parseRedirectUri();

        // the redirect URI needs to belong to a client, so that's the second field we want to know
        this.clientId = paramValue("client_id");
        this.client = OAuthClientService.getById(clientId)
                .orElseThrow(() -> new InputException(ErrorCode.INVALID_REQUEST,
                        "No client was found for clientId " + this.clientId));
        if (!this.client.ownsURI(this.redirectUri)) {
            throw new InputException(ErrorCode.INVALID_REQUEST,
                    "The client " + this.clientId + " does not contol redirect uri " + this.redirectUri);
        }

        // than other required fields
        this.scope = parseScope();
        this.responseType = parseResponseType();

        // derive flow from responseType
        // NB: the order here matters! this depends on responseType being set and parseResponseMode depends on this
        // being set!
        this.flow = deriveFlow();

        this.responseMode = parseResponseMode();

        // and the optionals
        this.state = paramValue("state");
        this.nonce = paramValue("nonce");
        this.display = Display.get(paramValue("display"));
        this.prompt = parsePrompt();
        this.maxAge = parseMaxAge();
        this.uiLocales = parseLocales();
        this.idTokenHint = paramValue("id_token_hint");
        this.loginHint = paramValue("login_hint");
        this.acrValues = paramValueAsList("acr_values", Function.identity());

        // this must be the last call as it needs all the values to be set
        validateCompleteObject(validator);
    }

    /**
     * derive the flow from the response_type that must have been set by now
     * 
     * @return see description
     */
    private Flow deriveFlow() {
        Flow result;
        if (responseType.contains(ResponseType.CODE)) {
            if (responseType.size() == 1) {
                result = Flow.AUTHORIZATION_CODE;
            } else {
                result = Flow.HYBRID;
            }
        } else {
            result = Flow.IMPLICIT;
        }
        return result;
    }

    /**
     * Parses the ui locales and performs basic validation on it
     * 
     * @return the parsed value or an empty list if there was none
     */
    private List<Locale> parseLocales() {
        return paramValueAsList("ui_locales", lang -> {
            Locale locale = Locale.forLanguageTag(lang);
            if (locale.getLanguage() == null || locale.getLanguage().isEmpty()) {
                throw new InputException(ErrorCode.INVALID_REQUEST, "Illegal value %s for uiLocales", lang);
            }
            return locale;
        });
    }

    /**
     * Parses the prompt and performs basic validation on it
     * 
     * @return the parsed value or an empty list if there was none
     */
    private Set<Prompt> parsePrompt() {
        Set<Prompt> result = paramValueAsEnumSet("prompt", Prompt::get, Prompt.class);
        if (result.contains(Prompt.NONE) && result.size() != 1) {
            throw new InputException(ErrorCode.INVALID_REQUEST,
                    "If prompt has value 'none' no other value may be present");
        }
        return result;
    }

    /**
     * Parses the response type and performs basic validation on it
     * 
     * @return the parsed value or an empty list if there was none
     */
    private Set<ResponseType> parseResponseType() {
        return paramValueAsEnumSet("response_type", ResponseType::get, ResponseType.class);
    }

    /**
     * Parses the response mode and performs basic validation on it
     * 
     * @return the parsed value or an empty list if there was none
     */
    private ResponseMode parseResponseMode() {
        String rm = paramValue("response_mode");
        ResponseMode result;
        if (rm != null) {
            result = ResponseMode.get(rm);
        } else if (this.flow == Flow.AUTHORIZATION_CODE) {
            result = ResponseMode.QUERY;
        } else {
            result = ResponseMode.FRAGMENT;
        }
        return result;
    }

    /**
     * Parses the max_age attribute and performs a basic validation that it is a number
     * 
     * @return the parsed value, or null if there was none
     * @throws InputException
     *             if there was a {@link NumberFormatException}. The original exception is not wrapped, but rather a new
     *             one is thrown
     */
    private Integer parseMaxAge() {
        String mage = paramValue("max_age");
        if (mage == null) {
            return null;
        }
        // else
        try {
            BigInteger bi = new BigInteger(mage);
            // accept loss of information if this is bigger than int
            return bi.intValue();
        } catch (NumberFormatException e) {
            throw new InputException(ErrorCode.INVALID_REQUEST, "maxAge is not a valid number: %s", mage);
        }
    }

    /**
     * Parses the redirect_uri from parameters and performs basic validations.
     * 
     * @return the parsed URI
     * @throws InputException
     *             if any of the basic validations fail
     */
    private URI parseRedirectUri() {
        URI result = null;
        String uri = paramValue("redirect_uri");
        if (uri != null) {
            if (uri.contains("#")) {
                // per https://tools.ietf.org/html/rfc6749#section-3.1.2 the redirect URI must not contain a fragment
                uri = uri.substring(0, uri.indexOf('#'));
            }

            try {
                result = new URI(uri);
            } catch (URISyntaxException e) {
                LOGGER.warn("Error while doing AuthenticationRequest", e);
                throw new InputException(ErrorCode.INVALID_REQUEST, "redirect_uri is not an URI at all: " + uri);
            }
            String scheme = result.getScheme();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new InputException(ErrorCode.INVALID_REQUEST,
                        "redirectUri should be http(s) not " + scheme + " (from " + uri + ")");
            }
        }
        if (result == null) {
            throw new InputException(ErrorCode.INVALID_REQUEST, "No value was given for redirectUri");
        }
        return result;
    }

    /**
     * parses the scope parameter and performs basic validations
     * 
     * @return the parsed scope
     * @throws InputException
     *             if any of the basic validations fail
     * @throws RuntimeException
     *             if UTF is an unsupported encoding
     */
    private Set<String> parseScope() {
        // the Gluu implementation suggest that this might be URL encoded. This is not per spec so ignoring that here
        Set<String> scopes = paramValueAsSet("scope", Function.identity());
        if (!scopes.contains("openid")) {
            throw new InputException(ErrorCode.INVALID_SCOPE, "'openid' must be one of the values for scope");
        }
        if (!scopes.stream().allMatch(s -> SCOPE_PATTERN.matcher(s).matches())) {
            throw new InputException(ErrorCode.INVALID_SCOPE, "scope %s has invalid characters", scopes);
        }
        return scopes;
    }

    /**
     * Gets the value of the first parameter identified by the key. If it is null of empty (all blank characters) an
     * empty collection is returned. Otherwise the value is split by space and each of the parts is transformed using
     * the function. The transformed entity is added to the result list.
     * 
     * @param key
     *            the parameter
     * @param func
     *            a non-interfering, stateless function to apply to each element
     * @return see description
     */
    private <R> List<R> paramValueAsList(String key, Function<String, R> func) {
        String input = paramValue(key);
        if (input == null) {
            return Collections.emptyList();
        }
        // else
        try (Stream<String> stream = Arrays.stream(input.split(" "))) {
            return stream.map(func).collect(Collectors.toCollection(ArrayList::new));
        }
    }

    /**
     * Gets the value of the first parameter identified by the key. If it is null of empty (all blank characters) an
     * empty collection is returned. Otherwise the value is split by space and each of the parts is transformed using
     * the function. The transformed entity is added to the result set.
     * 
     * @param key
     *            the parameter
     * @param func
     *            a non-interfering, stateless function to apply to each element
     * @return see description
     */
    private <R> Set<R> paramValueAsSet(String key, Function<String, R> func) {
        return paramValueAsSet(key, func, HashSet::new);
    }

    private <R extends Enum<R>> Set<R> paramValueAsEnumSet(String key, Function<String, R> func, Class<R> klazz) {
        return paramValueAsSet(key, func, () -> EnumSet.noneOf(klazz));
    }

    private <R> Set<R> paramValueAsSet(String key, Function<String, R> func, Supplier<Set<R>> supplier) {
        String input = paramValue(key);
        if (input == null) {
            return Collections.emptySet();
        }
        // else
        try (Stream<String> stream = Arrays.stream(input.split(" "))) {
            return stream.map(func).collect(Collectors.toCollection(supplier));
        }
    }

    /**
     * Gets the value of the first parameter identified by the key. If it is null of empty (all blank characters) null
     * is returned. This function requires the field parameters to have been set.
     * 
     * @param key
     *            the parameter key
     * @return see description
     */
    private String paramValue(String key) {
        List<String> inputList = parameters.get(key);
        if (inputList == null || inputList.isEmpty()) {
            return null;
        }
        if (inputList.size() > 1) {
            throw new InputException(ErrorCode.INVALID_REQUEST, "Multiple parameters %s", key);
        }

        // at this point we know there is one
        String input = inputList.get(0);
        if (input == null) {
            return null;
        }

        input = input.trim();
        if (input.isEmpty()) {
            return null;
        }
        return input;
    }

    /**
     * Performs validation after all fields have been set. This makes it possible to check inter-dependent fields
     * 
     * @param validator
     *            to check all validation annotations
     */
    private void validateCompleteObject(Validator validator) {
        StringBuilder message = new StringBuilder();
        for (ConstraintViolation<AuthenticationRequest> vio : validator.validate(this)) {
            // @formatter:off
            message
                .append(vio.getPropertyPath())
                .append(" [").append(vio.getInvalidValue()).append("]: ")
                .append(vio.getMessage())
                .append("\n");
            // @formatter:on
        }
        if (message.length() > 0) {
            throw new InputException(ErrorCode.INVALID_REQUEST, message.toString());
        }
    }

    /**
     * returns the redirect uri with error parameters appended to it, or null if the redirect uri cannot be used. If
     * null is returned the redirect should not happen and the caller should be notified in some other way.
     * 
     * @param e
     *            the exception that caused this error to get reported
     * @return see description
     */
    URI getErrorUri(InputException e) {
        if (redirectUri == null || this.client == null || !this.client.ownsURI(this.redirectUri)) {
            return null;
        }
        // @formatter:off
        return UriBuilder
            .fromUri(redirectUri)
            .replaceQuery(null)
            .queryParam("error", e.getError().toString())
            .queryParam("error_description", e.getErrorDescription())
            .queryParam("state", state == null ? "" : state)
            .build();
        // @formatter:on
    }

    /**
     * @return the bytes of {@link ObjectOutputStream#writeObject(Object)} called on 'this'
     */
    public byte[] toBytes() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(this);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Error while writing AuthReqForm to byte[]", e);
        }
    }
    
    /**
     * Sonar will warn about the Lists not being Serializable, yet I want to keep the semantics of the interface rather
     * than the concrete class. This keeps clients of this class honest. This method exists to get rid of the warnings.
     * <p>
     * <strong>NB: this means that the concrete instantiations of the collection fields in this class MUST be
     * Serializable</strong>
     * 
     * @param stream
     *            where to write it to
     * @throws IOException
     *             when stream.defaultReadObject does
     */
    @SuppressWarnings("static-method")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
    }

    /**
     * Sonar will warn about the Lists not being Serializable, yet I want to keep the semantics of the interface rather
     * than the concrete class. This keeps clients of this class honest. This method exists to get rid of the warnings.
     * <p>
     * <strong>NB: this means that the concrete instantiations of the collection fields in this class MUST be
     * Serializable</strong>
     * 
     * @param stream
     *            where to read from
     * @throws IOException
     *             when stream.defaultReadObject does
     * @throws ClassNotFoundException
     *             when stream.defaultReadObject does
     */
    @SuppressWarnings("static-method")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
    }

    public static AuthenticationRequest fromBytes(byte[] input) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(input); ObjectInput in = new ObjectInputStream(bis)) {
            return (AuthenticationRequest) in.readObject();
        } catch (ClassNotFoundException e) {
            LOGGER.warn("expected input to be AuthenticationRequest, but {} wasn't", Arrays.toString(input));
            throw new InputException(ErrorCode.INVALID_REQUEST, e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException("Error while reading AuthReqForm from byte[]", e);
        }
    }

    static void resetStorage() {
        storage.remove();
    }

    static AuthenticationRequest fromStorage() {
        return storage.get();
    }

    @Override
    public String toString() {
        return "AuthenticationRequest [scope=" + scope + ", responseType=" + responseType + ", clientId=" + clientId
                + ", redirectUri=" + redirectUri + ", state=" + state + ", nonce=" + nonce + ", display=" + display
                + ", prompt=" + prompt + ", maxAge=" + maxAge + ", uiLocales=" + uiLocales + ", idTokenHint="
                + idTokenHint + ", loginHint=" + loginHint + ", acrValues=" + acrValues + ", client=" + client + "]";
    }
}

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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.api.param.ResponseMode;
import nl.qiy.oic.op.api.param.ResponseType;
import nl.qiy.oic.op.domain.IDToken;
import nl.qiy.oic.op.domain.OAuthUser;
import nl.qiy.oic.op.service.OAuthUserService;

/**
 * Given an {@link OAuthUser} and an {@link AuthenticationRequest}, this class builds a Response URI to which the user
 * should be redirected
 *
 * @author Friso Vrolijken
 * @since 20 mei 2016
 */
public class AuthenticationResponse {
    /**
     * Standard SLF4J Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationResponse.class);
    /**
     * UTF-8 for encoding-decoding
     */
    private static final String UTF8 = StandardCharsets.UTF_8.name();

    private static ThreadLocal<SecureRandom> RANDOMS = ThreadLocal.withInitial(SecureRandom::new);

    /**
     * Constructor for AuthenticationResponse
     */
    private AuthenticationResponse() {
        super();
    }

    /**
     * Returns the AuthenticationResponse for the given request and the logged in user
     * 
     * @param inputs
     *            the user's input that needs this response
     * @param user
     *            authenticated user by the back-end
     * @return see description
     */
    public static Response getResponse(AuthenticationRequest inputs, OAuthUser user) {
        Map<String, String> params = getResponseParams(user, inputs);
        if (inputs.responseMode == ResponseMode.FORM_POST) {
            StringBuilder form = new StringBuilder();
            params.forEach((key, value) -> form
                    .append(String.format("<input type=\"hidden\" name=\"%s\" value=\"%s\"/>", key, value)));

            String page = String.format("<html><head><title>Submit This Form</title></head>"
                    + "<body onload=\"javascript:document.forms[0].submit()\">"
                    + "<form method=\"post\" action=\"%s\">%s</form></body></html>", inputs.redirectUri, form);
            return Response.ok(page, MediaType.TEXT_HTML).build();
        }
        // else
        return Response.seeOther(getRedirectUri(params, inputs)).build();
    }

    private static URI getRedirectUri(Map<String, String> params, AuthenticationRequest inputs) {
        UriBuilder builder = UriBuilder.fromUri(inputs.redirectUri);
        if (inputs.responseMode != ResponseMode.FORM_POST) {
            StringBuilder sb = new StringBuilder();
            for (Entry<String, String> entry : params.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }

            if (inputs.responseMode == ResponseMode.FRAGMENT) {
                builder.fragment(sb.toString());
            } else if (inputs.responseType.contains(ResponseType.TOKEN)
                    && inputs.responseType.contains(ResponseType.ID_TOKEN)) {
                LOGGER.info(
                        "Response mode was not fragment or form_post, but response type was 'token id_token', so returning fragment");
                // disregard the requested responseType, the spec says that we MAY NOT return a query string in this
                // case. See http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#rfc.section.5
                builder.fragment(sb.toString());
            } else if (inputs.responseMode == ResponseMode.QUERY) {
                builder.replaceQuery(sb.toString());
            }
        }
        URI result = builder.build();
        LOGGER.debug("redirecting to {}", result);
        return result;
    }


    private static Map<String, String> getResponseParams(OAuthUser user, AuthenticationRequest inputs) {
        Map<String, String> params = new HashMap<>();
        if (inputs.state != null) {
            try {
                String st = URLEncoder.encode(inputs.state, UTF8);
                params.put("state", st);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        // http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html
        // is contradicting itself:
        // 3. ID Token Response Type
        // ... The Authorization Server SHOULD NOT return an OAuth 2.0 Authorization Code, Access Token, or Access Token
        // Type in a successful response to the grant request.
        //
        // 5. Definitions of Multiple-Valued Response Type Combinations
        // code id_token
        // ... response MUST include both an Authorization Code and an id_token ...
        // id_token token
        // ... response MUST include an Access Token, an Access Token Type, and an id_token.

        // ?????????????

        if (inputs.responseType.contains(ResponseType.ID_TOKEN)) {
            IDToken idt = new IDToken(user);

            if (inputs.responseType.contains(ResponseType.TOKEN)) {
                String at = buildAccessToken();

                idt.setAccessToken(at);

                Long validSeconds = OAuthUserService.addBearer(at, idt);
                params.put("token_type", "Bearer");
                params.put("expires_in", validSeconds.toString());

            }
            params.put("id_token", idt.buildStringRepresentation(inputs.clientId, inputs.nonce));
        }

        return params;
    }

    private static String buildAccessToken() {
        byte[] random = new byte[64];
        RANDOMS.get().nextBytes(random);
        return Base64.getUrlEncoder().encodeToString(random);
    }
}

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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTClaimsSet.Builder;
import com.nimbusds.jwt.SignedJWT;

import nl.qiy.oic.op.api.param.ResponseMode;
import nl.qiy.oic.op.api.param.ResponseType;
import nl.qiy.oic.op.domain.AddressClaim;
import nl.qiy.oic.op.domain.KeyUsePredicate;
import nl.qiy.oic.op.domain.OAuthUser;
import nl.qiy.oic.op.service.ConfigurationService;
import nl.qiy.oic.op.service.SecretService;
import nl.qiy.oic.op.service.spi.Configuration;

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

    private static JWSSigner jwsSigner;
    private static Map<KeyUsePredicate, JWK> keysByUse = new EnumMap<>(KeyUsePredicate.class);
    private static Map<Class<?>, Collection<Entry<String, Field>>> baseTypeFields = new HashMap<>();
    private static Map<Class<?>, Collection<Entry<String, Field>>> complexTypeFields = new HashMap<>();

    private final OAuthUser user;
    private final AuthenticationRequest inputs;
    private final Date now = new Date();
    private final Date exp = Date.from(now.toInstant().plus(Duration.ofMinutes(10)));

    /**
     * Constructor for AuthenticationResponse
     * 
     * @param user
     * @param inputs
     */
    private AuthenticationResponse(OAuthUser user, AuthenticationRequest inputs) {
        super();
        this.user = user;
        this.inputs = inputs;
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
            } else if (inputs.responseMode == ResponseMode.QUERY) {
                builder.replaceQuery(sb.toString());
            }
        }
        URI result = builder.build();
        LOGGER.debug("redirecting to {}", result);
        return result;
    }


    private static Map<String, String> getResponseParams(OAuthUser user, AuthenticationRequest inputs) {
        AuthenticationResponse response = new AuthenticationResponse(user, inputs);
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
        if (inputs.responseType.contains(ResponseType.CODE)) {
            params.put("code", response.buildAuthCode());
        }
        if (inputs.responseType.contains(ResponseType.ID_TOKEN)) {
            params.put("id_token", response.buildIdToken());
        }
        if (inputs.responseType.contains(ResponseType.TOKEN)) {
            params.put("access_token", response.buildAccessToken());
            params.put("token_type", response.tokenType());
        }
        return params;
    }

    /**
     * @return Not implemented yet, will thrown an exception
     */
    @SuppressWarnings("static-method")
    private String tokenType() {
        // TODO build this
        throw new UnsupportedOperationException();
    }

    /**
     * @return Not implemented yet, will thrown an exception
     */
    @SuppressWarnings("static-method")
    private String buildAccessToken() {
        // TODO build this
        throw new UnsupportedOperationException();
    }

    /**
     * @return Not implemented yet, will thrown an exception
     */
    @SuppressWarnings("static-method")
    private String buildAuthCode() {
        // TODO build this
        throw new UnsupportedOperationException();
    }

    /**
     * Signs a JWT token with the first approprate key it finds
     * 
     * @param idToken
     * @return see description
     */
    private static SignedJWT signToken(JWTClaimsSet idToken) {
        try {
            JWK jwk = getJWKFor(KeyUsePredicate.SIG, "idToken");
            JWSAlgorithm alg = JWSAlgorithm.parse(jwk.getAlgorithm().getName());
            JWSHeader header = new JWSHeader(alg);
            SignedJWT signedClaims = new SignedJWT(header, idToken);
            signedClaims.sign(getJWSSigner("idToken"));
            return signedClaims;
        } catch (JOSEException e) {
            // TODO [FV 20160520] ... this can probably be handled better
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param type
     *            either "idToken" or "userInfo"
     * @return see description
     */
    private static JWSSigner getJWSSigner(String type) {
        try {
            if (jwsSigner == null) {
                JWK jwk = getJWKFor(KeyUsePredicate.SIG, type);
                JWSAlgorithm alg = JWSAlgorithm.parse(jwk.getAlgorithm().getName());
                if (JWSAlgorithm.Family.RSA.contains(alg)) {
                    jwsSigner = new RSASSASigner((RSAKey) jwk);
                } else if (JWSAlgorithm.Family.EC.contains(alg)) {
                    // NB: untested! Might not even work
                    jwsSigner = new ECDSASigner((ECKey) jwk);
                } else if (JWSAlgorithm.Family.HMAC_SHA.contains(alg)) {
                    // NB: untested! Might not even work
                    jwsSigner = new MACSigner(((OctetSequenceKey) jwk).toByteArray());
                }
            }
            return jwsSigner;
        } catch (JOSEException e) {
            throw new IllegalStateException("Check the configuration of JWS", e);
        }
    }

    /**
     * returns the key from {@link SecretService#getJWKSet(String)} with the appropriate use
     * 
     * @param keyUse
     *            whether the key is used for signing or encryption
     * @param type
     *            either "idToken", "requestObject" or "userInfo"
     * @return see description
     */
    private static JWK getJWKFor(KeyUsePredicate keyUse, String type) {
        JWK result = keysByUse.get(keyUse);
        if (result == null) {
            // @formatter:off
            result = SecretService.getJWKSet(type)
                    .getKeys()
                    .stream()
                    .filter(keyUse)
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
            // @formatter:on
            keysByUse.put(keyUse, result);
        }
        return result;
    }

    /**
     * Returns the basic ID Token as described by
     * <a href="http://openid.net/specs/openid-connect-core-1_0.html#IDToken">the OpenID connect spec</a>
     * 
     * @return that token
     */
    private String buildIdToken() {
        // @formatter:off
        Builder csBuilder = new JWTClaimsSet.Builder()
                .issuer(ConfigurationService.get(Configuration.ISS))
                .subject(user.getSubject())
                .audience(inputs.clientId)
                .expirationTime(exp)
                .issueTime(now);
        // @formatter:on

        // optional claims
        if (user.getLoginTime() != null) {
            csBuilder.claim("auth_time", user.getLoginTime().getEpochSecond());
        }

        if (user.getClaims() != null) {
            Map<String, Object> claims = toMap(user.getClaims());
            claims.forEach(csBuilder::claim);
        }

        if (inputs.nonce != null) {
            csBuilder.claim("nonce", inputs.nonce);
        }

        // currently (?) unsupported claims
        // - acr
        // - amr
        // - azp

        JWTClaimsSet idToken = csBuilder.build();
        SignedJWT signedIdToken = signToken(idToken);

        // TODO [FV 20160520] encryption is left for another day

        return signedIdToken.serialize();
    }

    private static Map<String, Object> toMap(Object mapMe) {
        try {
            Map<String, Object> result = new HashMap<>();
            for (Entry<String, Field> entry : getBaseTypeFields(mapMe.getClass())) {
                Object val = entry.getValue().get(mapMe);
                if (val != null) {
                    result.put(entry.getKey(), val);
                }
            }
            for (Entry<String, Field> entry : getComplexTypeFields(mapMe.getClass())) {
                Object val = entry.getValue().get(mapMe);
                if (val != null) {
                    result.put(entry.getKey(), toMap(val));
                }
            }
            return result;
        } catch (IllegalAccessException e) {
            LOGGER.warn("Error while doing toMap", e);
            throw new IllegalStateException(e);
        }
    }

    private static Collection<Entry<String, Field>> getComplexTypeFields(Class<? extends Object> class1) {
        Collection<Entry<String, Field>> result = complexTypeFields.get(class1);
        if (result == null) {
            Set<Class<?>> keepers = new HashSet<>(); // should really use the inverse filter from getBaseTypeFields
            keepers.add(AddressClaim.class);
            // @formatter:off
            result = getFields(class1)
                .stream()
                .filter(e -> keepers.contains(e.getValue().getType()))
                .collect(Collectors.toList());// @formatter:on
            complexTypeFields.put(class1, result);
        }
        return result;
    }

    private static Collection<Entry<String, Field>> getBaseTypeFields(Class<? extends Object> class1) {
        Collection<Entry<String, Field>> result = baseTypeFields.get(class1);
        if (result == null) {
            Set<Class<?>> keepers = new HashSet<>(); // could do more, but these ar all that are currently used
            keepers.add(String.class);
            keepers.add(Boolean.class);
            keepers.add(Long.class);
            // @formatter:off
            result = getFields(class1)
                .stream()
                .filter(e -> keepers.contains(e.getValue().getType()))
                .collect(Collectors.toList());// @formatter:on
            baseTypeFields.put(class1, result);
        }
        return result;
    }

    private static Collection<Entry<String, Field>> getFields(Class<?> klazz) {
        Field[] declairedFields = klazz.getDeclaredFields();
        Map<String, Field> result = new TreeMap<>();
        for (Field field : declairedFields) {
            if (Modifier.isFinal(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
                JsonProperty annotation = field.getAnnotation(JsonProperty.class);
                String name = annotation == null ? field.getName() : annotation.value();
                result.put(name, field);
            }
        }
        return result.entrySet();
    }
}

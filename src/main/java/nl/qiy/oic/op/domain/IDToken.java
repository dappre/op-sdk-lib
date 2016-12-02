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
package nl.qiy.oic.op.domain;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
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

import nl.qiy.oic.op.service.ConfigurationService;
import nl.qiy.oic.op.service.SecretService;
import nl.qiy.oic.op.service.spi.Configuration;

/**
 * The object that can build the JSON representation of a user
 *
 * @author friso
 * @since 1 dec. 2016
 */
public class IDToken implements Serializable {
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(IDToken.class);
    private static final long serialVersionUID = 1L;
    private final OAuthUser user;
    private final Date now = new Date();
    private final Date exp = Date.from(now.toInstant().plus(Duration.ofMinutes(10)));
    private static Map<Class<?>, Collection<Entry<String, Field>>> baseTypeFields = new HashMap<>();
    private static Map<Class<?>, Collection<Entry<String, Field>>> complexTypeFields = new HashMap<>();
    private static JWSSigner jwsSigner;
    private static Map<KeyUsePredicate, JWK> keysByUse = new EnumMap<>(KeyUsePredicate.class);
    private String accessToken = null;

    /**
     * Constructor for IDToken
     */
    public IDToken(OAuthUser userImpl) {
        super();
        this.user = userImpl;
    }

    /**
     * Returns the basic ID Token as described by
     * <a href="http://openid.net/specs/openid-connect-core-1_0.html#IDToken">the OpenID connect spec</a>
     * 
     * @param aud
     *            the intended audience
     * @param nonce
     *            the value for the nonce claim
     * @return see description
     */
    public String buildStringRepresentation(String aud, String nonce) {
        // @formatter:off
        Builder csBuilder = new JWTClaimsSet.Builder()
                .issuer(ConfigurationService.get(Configuration.ISS))
                .subject(user.getSubject())
                .audience(aud)
                .expirationTime(exp)
                .issueTime(now);
        // @formatter:on

        if (accessToken != null) {
            csBuilder.claim("at_hash", calcATHash());
        }

        // optional claims
        if (user.getLoginTime() != null) {
            csBuilder.claim("auth_time", user.getLoginTime().getEpochSecond());
        }

        if (user.getClaims() != null) {
            Map<String, Object> claims = toMap(user.getClaims());
            claims.forEach(csBuilder::claim);
        }

        if (nonce != null) {
            csBuilder.claim("nonce", nonce);
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

    private String calcATHash() {
        try {
            JWK jwk = getJWKFor(KeyUsePredicate.SIG, "idToken");
            String alg = "SHA-" + jwk.getAlgorithm().getName().substring(2);
            MessageDigest md = MessageDigest.getInstance(alg);
            md.update(accessToken.getBytes(StandardCharsets.US_ASCII));
            byte[] fullHash = md.digest();
            byte[] atHash = Arrays.copyOf(fullHash, fullHash.length / 2);
            return Base64.getEncoder().encodeToString(atHash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Error while doing calcATHash", e);
            throw new IllegalStateException(e);
        }
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
            Set<Class<?>> keepers = new HashSet<>(); // could do more, but these are all that are currently used
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
     * @param at
     */
    public void setAccessToken(String at) {
        accessToken = at;
    }
}

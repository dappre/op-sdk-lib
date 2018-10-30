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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.jwk.JWKSet;

import nl.qiy.oic.op.api.param.Display;
import nl.qiy.oic.op.api.param.ResponseMode;
import nl.qiy.oic.op.api.param.ResponseType;
import nl.qiy.oic.op.api.param.SubjectType;
import nl.qiy.oic.op.domain.KeyUsePredicate;
import nl.qiy.oic.op.service.ConfigurationService;
import nl.qiy.oic.op.service.spi.Configuration;

/**
 * Serializes to JSON, to help with the .well-known/openid-provider request This type is subclassable, so if you want to
 * support other claims, subclass this and go right ahead.
 *
 * @author Friso Vrolijken
 * @since 27 mei 2016
 */
@JsonInclude(value = Include.NON_EMPTY)
public class OpenIDProviderMetadata {
    /**
     * REQUIRED. URI using the <tt>https</tt> scheme with no query or fragment component that the OP asserts as its
     * Issuer Identifier. If Issuer discovery is supported (see <a href="#IssuerDiscovery">Section&nbsp;2 (OpenID
     * Provider Issuer Discovery)</a>), this value MUST be identical to the issuer value returned by WebFinger. This
     * also MUST be identical to the <tt>iss</tt> Claim value in ID Tokens issued from this Issuer.
     */
    public final URI issuer;

    /**
     * REQUIRED. URI of the OP's OAuth 2.0 Authorization Endpoint <a href="#OpenID.Core">[OpenID.Core] (Sakimura, N.,
     * Bradley, J., Jones, M., de Medeiros, B., and C. Mortimore, “OpenID Connect Core 1.0,” November&nbsp;2014.)</a>.
     */
    @JsonProperty("authorization_endpoint")
    public final URI authorizationEndpoint;

    /**
     * URI of the OP's OAuth 2.0 Token Endpoint <a href="#OpenID.Core">[OpenID.Core] (Sakimura, N., Bradley, J., Jones,
     * M., de Medeiros, B., and C. Mortimore, “OpenID Connect Core 1.0,” November&nbsp;2014.)</a>. This is REQUIRED
     * unless only the Implicit Flow is used.
     */
    @JsonProperty("token_endpoint")
    public final URI tokenEndpoint;

    /**
     * RECOMMENDED. URI of the OP's UserInfo Endpoint <a href="#OpenID.Core">[OpenID.Core] (Sakimura, N., Bradley, J.,
     * Jones, M., de Medeiros, B., and C. Mortimore, “OpenID Connect Core 1.0,” November&nbsp;2014.)</a>. This URI MUST
     * use the <tt>https</tt> scheme and MAY contain port, path, and query parameter components.
     */
    @JsonProperty("userinfo_endpoint")
    public final URI userinfoEndpoint;

    /**
     * REQUIRED. URI of the OP's JSON Web Key Set <a href="#JWK">[JWK] (Jones, M., “JSON Web Key (JWK),”
     * July&nbsp;2014.)</a> document. This contains the signing key(s) the RP uses to validate signatures from the OP.
     * The JWK Set MAY also contain the Server's encryption key(s), which are used by RPs to encrypt requests to the
     * Server. When both signing and encryption keys are made available, a <tt>use</tt> (Key Use) parameter value is
     * REQUIRED for all keys in the referenced JWK Set to indicate each key's intended usage. Although some algorithms
     * allow the same key to be used for both signatures and encryption, doing so is NOT RECOMMENDED, as it is less
     * secure. The JWK <tt>x5c</tt> parameter MAY be used to provide X.509 representations of keys provided. When used,
     * the bare key values MUST still be present and MUST match those in the certificate.
     */
    @JsonProperty("jwks_uri")
    public final URI jwksUri;

    /**
     * RECOMMENDED. URI of the OP's Dynamic Client Registration Endpoint
     * <a href="#OpenID.Registration">[OpenID.Registration] (Sakimura, N., Bradley, J., and M. Jones, “OpenID Connect
     * Dynamic Client Registration 1.0,” November&nbsp;2014.)</a>.
     */
    @JsonProperty("registration_endpoint")
    public final URI registrationEndpoint;

    /**
     * RECOMMENDED. JSON array containing a list of the <a href="#RFC6749">OAuth 2.0 (Hardt, D., “The OAuth 2.0
     * Authorization Framework,” October&nbsp;2012.)</a> [RFC6749] scope values that this server supports. The server
     * MUST support the <tt>openid</tt> scope value. Servers MAY choose not to advertise some supported scope values
     * even when this parameter is used, although those defined in <a href="#OpenID.Core">[OpenID.Core] (Sakimura, N.,
     * Bradley, J., Jones, M., de Medeiros, B., and C. Mortimore, “OpenID Connect Core 1.0,” November&nbsp;2014.)</a>
     * SHOULD be listed, if supported.
     */
    @JsonProperty("scopes_supported")
    public final Set<String> scopesSupported;

    /**
     * REQUIRED. JSON array containing a list of the OAuth 2.0 <tt>response_type</tt> values that this OP supports.
     * Dynamic OpenID Providers MUST support the <tt>code</tt>, <tt>id_token</tt>, and the <tt>token id_token</tt>
     * Response Type values.
     */
    @JsonProperty("response_types_supported")
    public final Set<ResponseType> responseTypesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the OAuth 2.0 <tt>response_mode</tt> values that this OP supports, as
     * specified in <a href="#OAuth.Responses">OAuth 2.0 Multiple Response Type Encoding Practices (de Medeiros, B.,
     * Ed., Scurtescu, M., Tarjan, P., and M. Jones, “OAuth 2.0 Multiple Response Type Encoding Practices,”
     * February&nbsp;2014.)</a> [OAuth.Responses]. If omitted, the default for Dynamic OpenID Providers is
     * <tt>["query", "fragment"]</tt>.
     */
    @JsonProperty("response_modes_supported")
    public final Set<ResponseMode> responseModesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the OAuth 2.0 Grant Type values that this OP supports. Dynamic OpenID
     * Providers MUST support the <tt>authorization_code</tt> and <tt>implicit</tt> Grant Type values and MAY support
     * other Grant Types. If omitted, the default value is <tt>["authorization_code", "implicit"]</tt>.
     */
    @JsonProperty("grant_types_supported")
    public final Set<String> grantTypesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the Authentication Context Class References that this OP supports.
     */
    @JsonProperty("acr_values_supported")
    public final Set<String> acrValuesSupported;

    /**
     * REQUIRED. JSON array containing a list of the Subject Identifier types that this OP supports. Valid types include
     * <tt>pairwise</tt> and <tt>public</tt>.
     */
    @JsonProperty("subject_types_supported")
    public final Set<SubjectType> subjectTypesSupported;

    /**
     * REQUIRED. JSON array containing a list of the JWS signing algorithms (<tt>alg</tt> values) supported by the OP
     * for the ID Token to encode the Claims in a JWT <a href="#JWT">[JWT] (Jones, M., Bradley, J., and N. Sakimura,
     * “JSON Web Token (JWT),” July&nbsp;2014.)</a>. The algorithm <tt>RS256</tt> MUST be included. The value
     * <tt>none</tt> MAY be supported, but MUST NOT be used unless the Response Type used returns no ID Token from the
     * Authorization Endpoint (such as when using the Authorization Code Flow).
     */
    @JsonProperty("id_token_signing_alg_values_supported")
    public final Set<String> idTokenSigningAlgValuesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the JWE encryption algorithms (<tt>alg</tt> values) supported by the OP
     * for the ID Token to encode the Claims in a JWT <a href="#JWT">[JWT] (Jones, M., Bradley, J., and N. Sakimura,
     * “JSON Web Token (JWT),” July&nbsp;2014.)</a>.
     */
    @JsonProperty("id_token_encryption_alg_values_supported")
    public final Set<String> idTokenEncryptionAlgValuesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the JWE encryption algorithms (<tt>enc</tt> values) supported by the OP
     * for the ID Token to encode the Claims in a JWT <a href="#JWT">[JWT] (Jones, M., Bradley, J., and N. Sakimura,
     * “JSON Web Token (JWT),” July&nbsp;2014.)</a>.
     */
    @JsonProperty("id_token_encryption_enc_values_supported")
    public final Set<String> idTokenEncryptionEncValuesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the JWS <a href="#JWS">[JWS] (Jones, M., Bradley, J., and N. Sakimura,
     * “JSON Web Signature (JWS),” July&nbsp;2014.)</a> signing algorithms (<tt>alg</tt> values) <a href="#JWA">[JWA]
     * (Jones, M., “JSON Web Algorithms (JWA),” July&nbsp;2014.)</a> supported by the UserInfo Endpoint to encode the
     * Claims in a JWT <a href="#JWT">[JWT] (Jones, M., Bradley, J., and N. Sakimura, “JSON Web Token (JWT),”
     * July&nbsp;2014.)</a>. The value <tt>none</tt> MAY be included.
     */
    @JsonProperty("userinfo_signing_alg_values_supported")
    public final Set<String> userinfoSigningAlgValuesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the JWE <a href="#JWE">[JWE] (Jones, M., Rescorla, E., and J.
     * Hildebrand, “JSON Web Encryption (JWE),” July&nbsp;2014.)</a> encryption algorithms (<tt>alg</tt> values)
     * <a href="#JWA">[JWA] (Jones, M., “JSON Web Algorithms (JWA),” July&nbsp;2014.)</a> supported by the UserInfo
     * Endpoint to encode the Claims in a JWT <a href="#JWT">[JWT] (Jones, M., Bradley, J., and N. Sakimura, “JSON Web
     * Token (JWT),” July&nbsp;2014.)</a>.
     */
    @JsonProperty("userinfo_encryption_alg_values_supported")
    public final Set<String> userinfoEncryptionAlgValuesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the JWE encryption algorithms (<tt>enc</tt> values)
     * <a href="#JWA">[JWA] (Jones, M., “JSON Web Algorithms (JWA),” July&nbsp;2014.)</a> supported by the UserInfo
     * Endpoint to encode the Claims in a JWT <a href="#JWT">[JWT] (Jones, M., Bradley, J., and N. Sakimura, “JSON Web
     * Token (JWT),” July&nbsp;2014.)</a>.
     */
    @JsonProperty("userinfo_encryption_enc_values_supported")
    public final Set<String> userinfoEncryptionEncValuesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the JWS signing algorithms (<tt>alg</tt> values) supported by the OP
     * for Request Objects, which are described in Section 6.1 of <a href="#OpenID.Core">OpenID Connect Core 1.0
     * (Sakimura, N., Bradley, J., Jones, M., de Medeiros, B., and C. Mortimore, “OpenID Connect Core 1.0,”
     * November&nbsp;2014.)</a> [OpenID.Core]. These algorithms are used both when the Request Object is passed by value
     * (using the <tt>request</tt> parameter) and when it is passed by reference (using the <tt>request_uri</tt>
     * parameter). Servers SHOULD support <tt>none</tt> and <tt>RS256</tt>.
     */
    @JsonProperty("request_object_signing_alg_values_supported")
    public final Set<String> requestObjectSigningAlgValuesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the JWE encryption algorithms (<tt>alg</tt> values) supported by the OP
     * for Request Objects. These algorithms are used both when the Request Object is passed by value and when it is
     * passed by reference.
     */
    @JsonProperty("request_object_encryption_alg_values_supported")
    public final Set<String> requestObjectEncryptionAlgValuesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the JWE encryption algorithms (<tt>enc</tt> values) supported by the OP
     * for Request Objects. These algorithms are used both when the Request Object is passed by value and when it is
     * passed by reference.
     */
    @JsonProperty("request_object_encryption_enc_values_supported")
    public final Set<String> requestObjectEncryptionEncValuesSupported;

    /**
     * OPTIONAL. JSON array containing a list of Client Authentication methods supported by this Token Endpoint. The
     * options are <tt>client_secret_post</tt>, <tt>client_secret_basic</tt>, <tt>client_secret_jwt</tt>, and
     * <tt>private_key_jwt</tt>, as described in Section 9 of <a href="#OpenID.Core">OpenID Connect Core 1.0 (Sakimura,
     * N., Bradley, J., Jones, M., de Medeiros, B., and C. Mortimore, “OpenID Connect Core 1.0,”
     * November&nbsp;2014.)</a> [OpenID.Core]. Other authentication methods MAY be defined by extensions. If omitted,
     * the default is <tt>client_secret_basic</tt> -- the HTTP Basic Authentication Scheme specified in Section 2.3.1 of
     * <a href="#RFC6749">OAuth 2.0 (Hardt, D., “The OAuth 2.0 Authorization Framework,” October&nbsp;2012.)</a>
     * [RFC6749].
     */
    @JsonProperty("token_endpoint_auth_methods_supported")
    public final Set<String> tokenEndpointAuthMethodsSupported;

    /**
     * OPTIONAL. JSON array containing a list of the JWS signing algorithms (<tt>alg</tt> values) supported by the Token
     * Endpoint for the signature on the JWT <a href="#JWT">[JWT] (Jones, M., Bradley, J., and N. Sakimura, “JSON Web
     * Token (JWT),” July&nbsp;2014.)</a> used to authenticate the Client at the Token Endpoint for the
     * <tt>private_key_jwt</tt> and <tt>client_secret_jwt</tt> authentication methods. Servers SHOULD support
     * <tt>RS256</tt>. The value <tt>none</tt> MUST NOT be used.
     */
    @JsonProperty("token_endpoint_auth_signing_alg_values_supported")
    public final Set<String> tokenEndpointAuthSigningAlgValuesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the <tt>display</tt> parameter values that the OpenID Provider
     * supports. These values are described in Section 3.1.2.1 of <a href="#OpenID.Core">OpenID Connect Core 1.0
     * (Sakimura, N., Bradley, J., Jones, M., de Medeiros, B., and C. Mortimore, “OpenID Connect Core 1.0,”
     * November&nbsp;2014.)</a> [OpenID.Core].
     */
    @JsonProperty("display_values_supported")
    public final Set<Display> displayValuesSupported;

    /**
     * OPTIONAL. JSON array containing a list of the Claim Types that the OpenID Provider supports. These Claim Types
     * are described in Section 5.6 of <a href="#OpenID.Core">OpenID Connect Core 1.0 (Sakimura, N., Bradley, J., Jones,
     * M., de Medeiros, B., and C. Mortimore, “OpenID Connect Core 1.0,” November&nbsp;2014.)</a> [OpenID.Core]. Values
     * defined by this specification are <tt>normal</tt>, <tt>aggregated</tt>, and <tt>distributed</tt>. If omitted, the
     * implementation supports only <tt>normal</tt> Claims.
     */
    @JsonProperty("claim_types_supported")
    public final Set<String> claimTypesSupported;

    /**
     * RECOMMENDED. JSON array containing a list of the Claim Names of the Claims that the OpenID Provider MAY be able
     * to supply values for. Note that for privacy or other reasons, this might not be an exhaustive list.
     */
    @JsonProperty("claims_supported")
    public final Set<String> claimsSupported;

    /**
     * OPTIONAL. URI of a page containing human-readable information that developers might want or need to know when
     * using the OpenID Provider. In particular, if the OpenID Provider does not support Dynamic Client Registration,
     * then information on how to register Clients needs to be provided in this documentation.
     */
    @JsonProperty("service_documentation")
    public final URI serviceDocumentation;

    /**
     * OPTIONAL. Languages and scripts supported for values in Claims being returned, represented as a JSON array of
     * <a href="#RFC5646">BCP47 (Phillips, A. and M. Davis, “Tags for Identifying Languages,” September&nbsp;2009.)</a>
     * [RFC5646] language tag values. Not all languages and scripts are necessarily supported for all Claim values.
     */
    @JsonProperty("claims_locales_supported")
    public final Set<Locale> claimsLocalesSupported;

    /**
     * OPTIONAL. Languages and scripts supported for the user interface, represented as a JSON array of
     * <a href="#RFC5646">BCP47 (Phillips, A. and M. Davis, “Tags for Identifying Languages,” September&nbsp;2009.)</a>
     * [RFC5646] language tag values.
     */
    @JsonProperty("ui_locales_supported")
    public final Set<Locale> uiLocalesSupported;

    /**
     * OPTIONAL. Boolean value specifying whether the OP supports use of the <tt>claims</tt> parameter, with
     * <tt>true</tt> indicating support. If omitted, the default value is <tt>false</tt>.
     */
    @JsonProperty("claims_parameter_supported")
    public final Boolean claimsParameterSupported;

    /**
     * OPTIONAL. Boolean value specifying whether the OP supports use of the <tt>request</tt> parameter, with
     * <tt>true</tt> indicating support. If omitted, the default value is <tt>false</tt>.
     */
    @JsonProperty("request_parameter_supported")
    public final Boolean requestParameterSupported;

    /**
     * OPTIONAL. Boolean value specifying whether the OP supports use of the <tt>request_uri</tt> parameter, with
     * <tt>true</tt> indicating support. If omitted, the default value is <tt>true</tt>.
     */
    @JsonProperty("request_uri_parameter_supported")
    public final Boolean requestUriParameterSupported;

    /**
     * OPTIONAL. Boolean value specifying whether the OP requires any <tt>request_uri</tt> values used to be
     * pre-registered using the <tt>request_uris</tt> registration parameter. Pre-registration is REQUIRED when the
     * value is <tt>true</tt>. If omitted, the default value is <tt>false</tt>.
     */
    @JsonProperty("require_request_uri_registration")
    public final Boolean requireRequestUriRegistration;

    /**
     * OPTIONAL. URI that the OpenID Provider provides to the person registering the Client to read about the OP's
     * requirements on how the Relying Party can use the data provided by the OP. The registration process SHOULD
     * display this URI to the person registering the Client if it is given.
     */
    @JsonProperty("op_policy_uri")
    public final URI opPolicyUri;

    /**
     * OPTIONAL. URI that the OpenID Provider provides to the person registering the Client to read about OpenID
     * Provider's terms of service. The registration process SHOULD display this URI to the person registering the
     * Client if it is given.
     */
    @JsonProperty("op_tos_uri")
    public final URI opTosUri;

    public OpenIDProviderMetadata(URI authorizationEndpoint, URI userInfoEndpoint, URI jwksUri, JWKSet idTokenJWKInfo) {
        super();
        this.issuer = URI.create(ConfigurationService.get(Configuration.ISS));
        this.authorizationEndpoint = authorizationEndpoint;
        this.tokenEndpoint = null; // no token endpoint yet
        this.userinfoEndpoint = userInfoEndpoint;
        this.jwksUri = jwksUri;
        this.registrationEndpoint = null; // no registration yet
        this.scopesSupported = new HashSet<>();
        this.scopesSupported.add("openid");
        this.responseTypesSupported = EnumSet.of(ResponseType.ID_TOKEN);
        this.responseModesSupported = EnumSet.of(ResponseMode.FRAGMENT, ResponseMode.QUERY, ResponseMode.FORM_POST);
        this.grantTypesSupported = Collections.singleton("implicit");
        this.acrValuesSupported = null;
        this.subjectTypesSupported = EnumSet.of(SubjectType.PAIRWISE);
        this.idTokenSigningAlgValuesSupported = signingAlgValues(idTokenJWKInfo);
        this.idTokenEncryptionAlgValuesSupported = encryptionAlgValues(idTokenJWKInfo);
        this.idTokenEncryptionEncValuesSupported = encryptionEncValues(idTokenJWKInfo);
        this.userinfoSigningAlgValuesSupported = signingAlgValues(idTokenJWKInfo);
        this.userinfoEncryptionAlgValuesSupported = encryptionAlgValues(idTokenJWKInfo);
        this.userinfoEncryptionEncValuesSupported = encryptionEncValues(idTokenJWKInfo);
        this.requestObjectSigningAlgValuesSupported = null;
        this.requestObjectEncryptionAlgValuesSupported = null;
        this.requestObjectEncryptionEncValuesSupported = null;
        this.tokenEndpointAuthMethodsSupported = null;
        this.tokenEndpointAuthSigningAlgValuesSupported = null;
        this.displayValuesSupported = null; // nothing done with 'em yet?
        this.claimTypesSupported = null;
        this.claimsSupported = null;
        this.serviceDocumentation = null;
        this.claimsLocalesSupported = null;
        this.uiLocalesSupported = null;
        this.claimsParameterSupported = null;
        this.requestParameterSupported = null;
        this.requestUriParameterSupported = null; // defaults to true
        this.requireRequestUriRegistration = null;
        this.opPolicyUri = null;
        this.opTosUri = null;
    }

    private static Set<String> encryptionEncValues(JWKSet idTokenJWKInfo) {
        // TODO: implement
        return null;
    }

    private static Set<String> encryptionAlgValues(JWKSet idTokenJWKInfo) {
        // @formatter:off
        return idTokenJWKInfo
            .getKeys()
            .stream()
            .filter(KeyUsePredicate.ENC)
            .map(k -> k.getAlgorithm() == null ? null : k.getAlgorithm().toString())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        // @formatter:on
    }

    private static Set<String> signingAlgValues(JWKSet idTokenJWKInfo) {
        // @formatter:off
        return idTokenJWKInfo
            .getKeys()
            .stream()
            .filter(KeyUsePredicate.SIG)
            .map(k -> k.getAlgorithm() == null ? null : k.getAlgorithm().toString())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        // @formatter:on
    }

}

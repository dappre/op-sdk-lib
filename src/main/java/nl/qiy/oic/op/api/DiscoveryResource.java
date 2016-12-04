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

import java.lang.reflect.Method;
import java.net.URI;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import net.minidev.json.JSONObject;
import nl.qiy.oic.op.service.ConfigurationService;
import nl.qiy.oic.op.service.SecretService;
import nl.qiy.oic.op.service.spi.Configuration;

/**
 * Helper that should be extended to implement discovery, for now, just do parts
 *
 * @author Friso Vrolijken
 * @since 17 mei 2016
 */
@Path(".well-known")
public class DiscoveryResource {
    private static URI authEndpointUri;
    private static URI userInfoUri;
    private static URI jwksUri;

    @Path("openid-configuration")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("ucd")
    public static OpenIDProviderMetadata getOpenIdConfiguration() {
        return new OpenIDProviderMetadata(getAuthEndpoinURI(), getUserInfoEndpointURI(), getJwksURI(),
                SecretService.getJWKSet("idToken"));
    }

    private static URI getUserInfoEndpointURI() {
        if (userInfoUri == null) {
            String baseUri = ConfigurationService.get(Configuration.BASE_URI);

            try (Stream<Method> methods = Stream.of(AuthenticationResource.class.getDeclaredMethods())) {
                // @formatter:off
                Method method = methods
                        .filter(m -> "getUserInfo".equals(m.getName()))
                        .findFirst()
                        .orElseThrow(UnsupportedOperationException::new);
                // @formatter:on
                UriBuilder builder = UriBuilder.fromUri(baseUri).path(AuthenticationResource.class).path(method);
                URI result = builder.build();
                userInfoUri = result;
            }

        }
        return userInfoUri;
    }

    private static URI getJwksURI() {
        if (jwksUri == null) {
            String baseUri = ConfigurationService.get(Configuration.BASE_URI);

            try (Stream<Method> methods = Stream.of(DiscoveryResource.class.getDeclaredMethods())) {
                // @formatter:off
                Method method = methods
                        .filter(m -> "getKeySet".equals(m.getName()))
                        .findFirst()
                        .orElseThrow(UnsupportedOperationException::new);
                // @formatter:on
                UriBuilder builder = UriBuilder.fromUri(baseUri).path(DiscoveryResource.class).path(method);
                URI result = builder.build();
                jwksUri = result;
            }

        }
        return jwksUri;

    }

    private static URI getAuthEndpoinURI() {
        if (authEndpointUri == null) {
            String baseUri = ConfigurationService.get(Configuration.BASE_URI);
            UriBuilder builder = UriBuilder.fromUri(baseUri);
            // builder path (authrequest.class) and path (method) but those are both be empty
            URI result = builder.build();
            authEndpointUri = result;
        }
        return authEndpointUri;
    }

    @Path("jwksUri")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("ucd")
    public static JSONObject getKeySet() {
        return SecretService.getJWKSet("idToken").toJSONObject(true);
    }

    @Override
    public String toString() {
        return "only exists to satisfy SonarLint";
    }

}

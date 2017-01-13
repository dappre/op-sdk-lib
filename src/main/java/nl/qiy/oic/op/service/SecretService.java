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

package nl.qiy.oic.op.service;

import com.nimbusds.jose.jwk.JWKSet;

import nl.qiy.oic.op.service.spi.SecretStore;

/**
 * ;-) handling secret information
 *
 * @author Friso Vrolijken
 * @since 2 mei 2016
 */
public enum SecretService implements LoadingService {
    INSTANCE;

    private final FCFSServiceLoaderWrapper<SecretStore> loader = new FCFSServiceLoaderWrapper<>(SecretStore.class);

    /**
     * @param type
     *            one of idToken, userInfo or requestObject
     * @return the JSON for the JWKSet (both public and private). These are the keys that are needed to communicate with
     *         the other OAuth parties
     */
    public static JWKSet getJWKSet(String type) {
        // @formatter:off
        return INSTANCE
                .loader
                .get(s -> s.getJWKSet(type))
                .orElseThrow(IllegalStateException::new);
        // @formatter:on
    }
}

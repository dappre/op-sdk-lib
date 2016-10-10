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

package nl.qiy.oic.op.service.spi;

import java.security.PrivateKey;

import com.nimbusds.jose.jwk.JWKSet;

/**
 * ServiceProviderInterface for the SecretService
 *
 * @author Friso Vrolijken
 * @since 3 mei 2016
 */
public interface SecretStore extends ServiceProviderInterface {

    /**
     * Returns the secret that is needed to initiate connections from the Node. If the implementation doesn't know the
     * private key, it will return null to pass control to the next implementation.
     * 
     * @return see description
     */
    String getNodePassword();

    /**
     * Returns the private key for access to the node. If the implementation doesn't know the private key, it will
     * return null to pass control to the next implementation.
     * 
     * @return see description
     */
    PrivateKey getNodePrivateKey();

    /**
     * returns a complete JWK description (both public and private)
     * 
     * @param type
     *            one of idToken, userInfo or requestObject
     * @return see description
     */
    JWKSet getJWKSet(String type);

}

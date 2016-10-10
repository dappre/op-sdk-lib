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

import nl.qiy.oic.op.domain.OAuthClient;

/**
 * Manages the clients that are known to this provider. Implementations MAY dynamically find clients or MAY have a
 * static list
 *
 * @author Friso Vrolijken
 * @since 3 mei 2016
 */
public interface ClientStore extends ServiceProviderInterface {
    /**
     * @param clientId
     *            an identifier for a client. Per OAuth spec no assumptions are made
     * @return a client, if found, null otherwise
     */
    OAuthClient getById(String clientId);

    /**
     * Should only return Boolean.TRUE if there is a client known to exist with that will send the given Origin header
     * in a (ajax) request. Should only return Boolean.FALSE if such a client is know NOT to exist. Should return null
     * if the answer is unknown.
     * 
     * @param origin
     *            the "Origin" header that a client may sent
     * @return see description
     */
    Boolean existstOrigin(String origin);
}

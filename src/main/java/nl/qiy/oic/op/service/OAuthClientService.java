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

import java.util.Optional;

import nl.qiy.oic.op.domain.OAuthClient;
import nl.qiy.oic.op.service.spi.ClientStore;

/**
 * Enum singleton with convenience methods to make the calls static. Uses the service loader to load services that might
 * provide {@link OAuthClient}s. Other managment of clients might be added.
 *
 * @author Friso Vrolijken
 * @since 29 apr. 2016
 */
public enum OAuthClientService implements LoadingService {
    INSTANCE;
    private final FCFSServiceLoaderWrapper<ClientStore> loader = new FCFSServiceLoaderWrapper<>(ClientStore.class);

    public static Optional<OAuthClient> getById(String clientId) {
        return INSTANCE.loader.get(clientDao -> clientDao.getById(clientId));
    }

    /**
     * Return TRUE iff there exists a client store that knows there to be a client that will enter with the given Origin
     * header
     * 
     * @param origin
     *            value for the HTTP header "Origin"
     * @return see description
     */
    public static Boolean existsOrigin(String origin) {
        return INSTANCE.loader.get(clientDao -> clientDao.existstOrigin(origin)).orElse(Boolean.FALSE);
    }
}

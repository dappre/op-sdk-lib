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

import javax.ws.rs.client.Client;

import nl.qiy.oic.op.service.spi.JaxrsClient;

/**
 * Any instance of an implementation will be different. This class will help get a client that can and will be used to
 * execute client requests.
 *
 * @author Rudy Bruns
 * @since 2016-06-29
 */
public enum JaxrsClientService implements LoadingService {
    INSTANCE;
    private final FCFSServiceLoaderWrapper<JaxrsClient> loader = new FCFSServiceLoaderWrapper<>(JaxrsClient.class);

    /**
     * Gets the {@link Client} that is configured. Will throw an exception if no client is configured.
     * 
     * @return the configured {@link Client}.
     */
    public static Client getClient() {
        Optional<Client> result = INSTANCE.loader.get(conf -> conf.getClient());
        return result.orElseThrow(() -> new IllegalStateException("No client configured"));
    }

}
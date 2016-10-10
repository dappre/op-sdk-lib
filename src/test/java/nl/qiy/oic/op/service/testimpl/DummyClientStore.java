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

package nl.qiy.oic.op.service.testimpl;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.domain.OAuthClient;
import nl.qiy.oic.op.service.spi.ClientStore;

/**
 * ClientStore implementation to get through the test cases. As the service loader needs to know the class' name, a
 * Spock Mock or Stub won't suffice
 *
 * @author Friso Vrolijken
 * @since 10 mei 2016
 */
public class DummyClientStore implements ClientStore {
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyClientStore.class);

    @Override
    public OAuthClient getById(String clientId) {
        LOGGER.debug("returning dummy client");
        return new OAuthClient() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean ownsURI(URI redirectUri) {
                return true;
            }

        };
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public Boolean existstOrigin(String origin) {
        return null;
    }
}

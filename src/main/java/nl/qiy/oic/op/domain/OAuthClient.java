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
import java.net.URI;

/**
 * Implementation of a client in the OAuth sense, with only the operations we need
 * 
 * @author Friso Vrolijken
 * @since 29 apr. 2016
 */
public interface OAuthClient extends Serializable {

    /**
     * Per spec we should check if the redirectURI belongs to the given client
     * 
     * @param redirectUri
     *            a URI that should be known to belong to the client
     * @return true iff this client is known to control the redirectURI
     */
    boolean ownsURI(URI redirectUri);
}

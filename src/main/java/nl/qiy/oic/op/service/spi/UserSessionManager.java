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

import javax.servlet.http.HttpSession;

import nl.qiy.oic.op.domain.OAuthUser;

/**
 * Service provider for OAuth-users. Gets them if they're logged in, logs them in or out.
 *
 * @author Friso Vrolijken
 * @since 29 apr. 2016
 */
public interface UserSessionManager extends ServiceProviderInterface {
    /**
     * Tries to find a logged in user
     * 
     * @param session
     *            the HttpSession of the current user
     * 
     * @return a logged in user or null if there was no logged in user for these requests
     */
    public OAuthUser getLoggedIn(HttpSession session);

    /**
     * Logs out the user defined by the inputs and request, if any
     * 
     * @param session
     *            the HttpSession of the current user
     */
    public void logout(HttpSession session);

    /**
     * Tries to log in the user with the given input. Returns a user if that succeeds, returns null if it doesn't
     * 
     * @param template
     *            a dummy user object that should have all the property the specific implementation needs
     * @param session
     *            the session belonging to the HTTP request that initiated this call
     * @return see description
     */
    public OAuthUser login(OAuthUser template, HttpSession session);
}

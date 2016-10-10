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

import javax.servlet.http.HttpSession;

import nl.qiy.oic.op.domain.OAuthUser;
import nl.qiy.oic.op.service.spi.UserSessionManager;

/**
 * Handles the {@link OAuthUser}s
 *
 * @author Friso Vrolijken
 * @since 29 apr. 2016
 */
public enum OAuthUserService implements LoadingService {
    INSTANCE;
    private final FCFSServiceLoaderWrapper<UserSessionManager> loader = new FCFSServiceLoaderWrapper<>(
            UserSessionManager.class);

    public static Optional<OAuthUser> getLoggedIn(HttpSession session) {
        return INSTANCE.loader.get(userDao -> userDao.getLoggedIn(session));
    }

    /**
     * Logs out the user identified by either the inputs or the request (or both) with all UserDAOs
     * 
     * @param session
     *            the HttpSession of the current user
     */
    public static void logout(HttpSession session) {
        for (UserSessionManager userDao : INSTANCE.loader) {
            userDao.logout(session);
        }
    }

    /**
     * Tries to log in the user in with all {@link UserSessionManager}. Stops at the first one that succeeds and returns
     * the user which was the result of the login attempt. Will return null if no {@link UserSessionManager} could log
     * the user in
     * 
     * @param template
     *            a dummy user that holds enough information to uniquely identify her within a given
     *            {@link UserSessionManager}
     * @param session
     *            the request that initiated this login
     * @return see description
     */
    public static OAuthUser login(OAuthUser template, HttpSession session) {
        return INSTANCE.loader.get(usrSrv -> usrSrv.login(template, session)).orElse(null);
    }

}

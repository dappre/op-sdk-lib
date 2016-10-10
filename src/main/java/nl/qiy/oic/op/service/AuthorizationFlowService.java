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

import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import nl.qiy.oic.op.api.AuthenticationRequest;
import nl.qiy.oic.op.service.spi.AuthorizationFlow;

/**
 * A generic way to handle the Authorization flow. While this project was started to handle the Qiy authorization flow,
 * it can be used to accomodate any other
 *
 * @author Friso Vrolijken
 * @since 12 mei 2016
 */
public enum AuthorizationFlowService implements LoadingService {
    INSTANCE;
    private final DecoratingServiceLoaderWrapper<AuthorizationFlow> loader = new DecoratingServiceLoaderWrapper<>(
            AuthorizationFlow.class);

    /**
     * Will be invoked by the AuthenticationResource when it has determined that it is time to do so.
     * 
     * @param inputs
     *            the user's inputs
     * @param session
     *            the session belonging to the request that started this call
     * @return the actual non-null Response that will be sent to the client
     */
    public static Response startFlow(AuthenticationRequest inputs, HttpSession session) {
        // @formatter:off
        Response response = INSTANCE.loader
                .get(
                    flow -> flow.startFlow(inputs, session),
                    flow -> flow.startFlowCombiner()
                );
        // @formatter:on
        if (response == null) {
            throw new IllegalStateException("Unable to get a response to start the AuthFlow " + inputs);
        }
        return response;
    }
}

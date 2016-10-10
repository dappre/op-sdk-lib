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

import java.util.function.BinaryOperator;

import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import nl.qiy.oic.op.api.AuthenticationRequest;

/**
 * Creates the UI part of the login
 *
 * @author Friso Vrolijken
 * @since 12 mei 2016
 */
public interface AuthorizationFlow extends ServiceProviderInterface {

    /**
     * This is called when the user needs to authenticate. The caller should have checked that.
     * 
     * @param inputs
     *            the user's input, will be used to construct the callback URI
     * @param session
     *            the session belonging to the request that started this call
     * @return a HTML page with the response, or a redirect or ...
     */
    Response startFlow(AuthenticationRequest inputs, HttpSession session);

    /**
     * As there may be more than one Authorization flow configured, we may need to combine them. This returns a binary
     * operator to achieve that.
     * 
     * @return see description
     */
    BinaryOperator<Response> startFlowCombiner();
}

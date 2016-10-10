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

package nl.qiy.oic.op.api;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To make sure that the AuthRequest is available regardless of where we are in the processing, it is stored in the
 * thread. This filter clears that ThreadLocal storage.
 *
 * @author Friso Vrolijken
 * @since 10 mei 2016
 */
public class InputResetFilter implements Filter {
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(InputResetFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no action needed

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        LOGGER.debug("resetting Auth Req Form Storage");
        AuthenticationRequest.resetStorage();
        chain.doFilter(request, response);
        LOGGER.debug("resetting Auth Req Form Storage again");
        AuthenticationRequest.resetStorage();
    }

    @Override
    public void destroy() {
        // no action needed

    }
}

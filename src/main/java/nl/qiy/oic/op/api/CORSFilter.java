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
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.service.OAuthClientService;

/**
 * If you want to allow access to the OP from other sites using ajax, enable this filter. It will ask the ClientStores
 * if the origin header is supposed to be allowed
 *
 * @author Friso Vrolijken
 * @since 23 mei 2016
 */
public class CORSFilter implements Filter {
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CORSFilter.class);

    /**
     * Headers that always have the same values, initialisation is done in {@link #init(FilterConfig)}
     */
    private Map<String, String> headers = new HashMap<>(3);

    private boolean allowAll;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        headers.put("Access-Control-Allow-Headers",
                "Cache-Control, Pragma, Origin, Authorization, Content-Type, X-Requested-With, Set-Cookie, Cookie, Accept");
        headers.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS, HEAD");
        headers.put("Access-Control-Allow-Credentials", "false");

        allowAll = Boolean.parseBoolean(filterConfig.getInitParameter("allowAll"));
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) resp;
        HttpServletRequest request = (HttpServletRequest) req;

        String origin;
        if (allowAll) {
            origin = "*";
            LOGGER.debug("allowing all CORS requests");
        } else {
            origin = URI.create(request.getHeader("Origin")).toString();
            if (origin != null && !OAuthClientService.existsOrigin(origin)) {
                LOGGER.debug("Resetting origin {}, since it is not accepted by the client service", origin);
                origin = null;
            }
        }

        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            // copy the headers from the map to the response
            headers.forEach(response::setHeader);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // no action needed
    }

}

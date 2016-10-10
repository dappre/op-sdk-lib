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

package nl.qiy.oic.op;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.service.AuthorizationFlowService;
import nl.qiy.oic.op.service.ConfigurationService;
import nl.qiy.oic.op.service.JaxrsClientService;
import nl.qiy.oic.op.service.OAuthClientService;
import nl.qiy.oic.op.service.OAuthUserService;
import nl.qiy.oic.op.service.SecretService;

/**
 * Checks at the start if all services are happy, giving them context to allow themselves to init
 *
 * @author Friso Vrolijken
 * @since 3 mei 2016
 */
@WebListener
public class ContextListener implements ServletContextListener {
    /**
     * Standard SLF4J Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextListener.class);

    public static boolean ok = false;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // just call 'name()' on all to make sure they're initialized
        ConfigurationService.INSTANCE.name();
        OAuthUserService.INSTANCE.name();
        OAuthClientService.INSTANCE.name();
        SecretService.INSTANCE.name();
        AuthorizationFlowService.INSTANCE.name();
        JaxrsClientService.INSTANCE.name();
        LOGGER.debug("init services success");
        ok = true;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // nothing to do here
    }
}

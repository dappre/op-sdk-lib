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

import nl.qiy.oic.op.service.spi.Configuration;

/**
 * Any instance of an implementation will be different. This class will help get values that are generally needed.
 *
 * @author Friso Vrolijken
 * @since 2 mei 2016
 */
public enum ConfigurationService implements LoadingService {
    INSTANCE;
    private final FCFSServiceLoaderWrapper<Configuration> loader = new FCFSServiceLoaderWrapper<>(Configuration.class);

    /**
     * Gets the value of a key. Will throw an exception if no item was found.
     * 
     * @param key
     *            one of the static values in {@link Configuration}
     * @param <T>
     *            the result value type
     * @return the configured value
     */
    public static <T> T get(String key) {
        Optional<T> result = INSTANCE.loader.get(conf -> conf.get(key));
        return result.orElseThrow(() -> new IllegalStateException("No configuration found for " + key));
    }

}

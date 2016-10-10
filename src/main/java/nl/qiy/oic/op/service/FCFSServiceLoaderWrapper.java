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

import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.service.spi.ServiceProviderInterface;

/**
 * First Come First Serve ServiceLoaderWrapper. Since the {@link ServiceLoader} will have the same patterns, regardless
 * of what service is invoked, abstract that away in this little class. This class will return the result of the first
 * SPI that is given.
 * <p>
 * Implements Iterable to be able to invoke all knows service providers on start and stop (not sure yet if that's a good
 * thing [FV 12-05-2016])
 *
 * @author Friso Vrolijken
 * @since 29 apr. 2016
 */
final class FCFSServiceLoaderWrapper<T extends ServiceProviderInterface> implements Iterable<T> {
    /**
     * Standard SLF4J Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FCFSServiceLoaderWrapper.class);

    /**
     * The actual {@link ServiceLoader}, which is not threadsafe, so wrapping it in a {@link ThreadLocal}
     */
    private final ThreadLocal<ServiceLoader<T>> loader;
    /**
     * Since actual implementations are likely to have one service provider that actually works, cache that one and use
     * it first on subsequent calls
     */
    private T lastSuccesfull = null;

    /**
     * Constructor for FCFSServiceLoaderWrapper
     * 
     * @param serviceInterface
     *            the class that is loaded by the {@link ServiceLoader}
     */
    FCFSServiceLoaderWrapper(Class<T> serviceInterface) {
        super();
        this.loader = ThreadLocal.withInitial(() -> ServiceLoader.load(serviceInterface));
        Iterator<T> iter = this.loader.get().iterator();
        if (!iter.hasNext()) {
            String name = serviceInterface.getName();
            throw new IllegalStateException(
                    "No service found that implements " + name + ". Please check that file META-INF/services/" + name
                            + " exists, is readable and has at least one implementing class in it's body");
        }
        while (iter.hasNext()) {
            T item = iter.next();
            if (!item.isHealthy()) {
                LOGGER.error("{} is reporting it's unhealthy", item.getClass());
            }
        }
    }


    /**
     * Will iterate over the loader and for each item call the function. If that function yields a non-null result it is
     * wrapped in an {@link Optional} and returned. If no item was able to provide a result {@link Optional#empty()} is
     * returned.
     * 
     * @param func
     *            a function that takes an element of the loader and returns something which will define the return type
     *            of this function
     * @return the first result any element of the loader could provide, or {@link Optional#empty()}
     */
    protected <R> Optional<R> get(Function<T, R> func) {
        // copy the pointer to make this threadsafe
        T tryMeFirst = lastSuccesfull;

        // Try a shortcut. The rationale behind this is that normally there will be two implementations active: the
        // default implementation and (maybe) the one that is used by the actual implementation of this project. The
        // second one will normaly have all the results.
        if (tryMeFirst != null) {
            R result = func.apply(tryMeFirst);
            if (result != null) {
                return Optional.of(result);
            }
        }

        // if that failed, do it all over again
        for (T t : loader.get()) {
            if (tryMeFirst != null && t.getClass().equals(tryMeFirst.getClass())) {
                // already tried that
                continue;
            }
            R result = func.apply(t);
            if (result != null) {
                LOGGER.debug("result found in {}", t);
                lastSuccesfull = t;
                return Optional.of(result);
            }
        }
        // if we get here nothing was found
        LOGGER.debug("no result found");
        return Optional.empty();
    }

    @Override
    public Iterator<T> iterator() {
        return loader.get().iterator();
    }

}

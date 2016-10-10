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
import java.util.ServiceLoader;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.service.spi.ServiceProviderInterface;

/**
 * Since the service loader will have the same patterns, regardless of what service is invoked, abstract that away in
 * this little class. This class will invoke all SPIs that are found, where each is given the result of the previous.
 *
 * @author Friso Vrolijken
 * @since 12 mei 2016
 */
@SuppressWarnings("ucd")
public class DecoratingServiceLoaderWrapper<T extends ServiceProviderInterface> implements Iterable<T> {
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DecoratingServiceLoaderWrapper.class);
    /**
     * The actual {@link ServiceLoader}, which is not threadsafe, so wrapping it in a {@link ThreadLocal}
     */
    private final ThreadLocal<ServiceLoader<T>> loader;

    /**
     * Constructor for DecoratingServiceLoaderWrapper
     * 
     * @param serviceInterface
     *            the interface that is loaded
     */
    DecoratingServiceLoaderWrapper(Class<T> serviceInterface) {
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

    @Override
    public Iterator<T> iterator() {
        return loader.get().iterator();
    }

    /**
     * iterates over all the service providers and applies the function on them. The result thereof if combined with the
     * others (starting with null), using the combinator.
     * 
     * @param func
     *            a function that takes the serviceProvider as it's argument and returns a result of type R (possibly
     *            null)
     * @param combinator
     *            combines the result thus far (possibly null) and the result from a service provider (possibly null)
     *            and combines them into a new result (possibly null)
     * @param <R>
     *            the return value type
     * @return the result (possibly null) from the last service provider in the row
     */
    @SuppressWarnings("ucd")
    public <R> R get(Function<T, R> func, Function<T, BinaryOperator<R>> combinator) {
        R result = null;
        for (T serviceProvider : loader.get()) {
            R newResult = func.apply(serviceProvider);
            result = combinator.apply(serviceProvider).apply(newResult, result);
        }
        LOGGER.debug("{} result found", result == null ? "no" : "a");
        return result;
    }
}

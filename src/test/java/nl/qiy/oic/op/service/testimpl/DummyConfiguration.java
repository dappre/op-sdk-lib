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

package nl.qiy.oic.op.service.testimpl;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.qiy.oic.op.service.spi.Configuration;

/**
 * Configuration implementation to get through the test cases. As the service loader needs to know the class' name, a
 * Spock Mock or Stub won't suffice
 *
 * @author Friso Vrolijken
 * @since 2 mei 2016
 */
public class DummyConfiguration implements Configuration {
    private Map<String, Object> delegate = new HashMap<>();

    /**
     * Default constructor for DummyConfiguration
     */
    public DummyConfiguration() {
        super();
        StringReader reader = new StringReader(
                "{\"env\": \"test\", \"obj\": {\"key\": \"value\"}, \"nr\":1, \"bool\": true}");
        try {
            delegate = new ObjectMapper().readerFor(Map.class).readValue(reader);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key) {
        return (T) delegate.get(key);
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

}

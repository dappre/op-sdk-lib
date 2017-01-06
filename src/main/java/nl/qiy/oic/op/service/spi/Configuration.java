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

/**
 * General configuration
 *
 * @author Friso Vrolijken
 * @since 2 mei 2016
 */
public interface Configuration extends ServiceProviderInterface {
    static final String NODE_ID = "node-id";
    static final String NODE_ENDPOINT = "node-endpoint";
    static final String SIGNATURE = "signature";
    static final String REGISTER_CALLBACK_URI = "register-callback-uri";
    static final String ISS = "iss";
    static final String BASE_URI = "base-uri";

    <T> T get(String key);

}

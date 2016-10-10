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

/**
 * Defines the OAuth error codes as defined in <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">RFC6749</a>
 *
 * @author Friso Vrolijken
 * @since 28 apr. 2016
 */
public enum ErrorCode {
    INVALID_REQUEST, /* UNAUTHORIZED_CLIENT, ACCESS_DENIED, */UNSUPPORTED_RESPONSE_TYPE, INVALID_SCOPE, SERVER_ERROR, /* TEMPORARILY_UNAVAILABLE */;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

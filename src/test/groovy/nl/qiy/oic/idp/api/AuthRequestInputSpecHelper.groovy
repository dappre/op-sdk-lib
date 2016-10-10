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

package nl.qiy.oic.idp.api

import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap

import org.apache.commons.lang3.RandomStringUtils

class AuthRequestInputsSpecHelper {
    def Random RANDOM = new Random();
    def MultivaluedMap<String, String> toMvMap(String scope, String responseType, String clientId, String redirectUri, String state, String nonce, String display, String prompt, String maxAge, String uiLocales, String idTokenHint, String loginHint, String acrValues) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.add("scope", scope);
        result.add("response_type", responseType);
        result.add("client_id", clientId);
        result.add("redirect_uri", redirectUri);
        result.add("state", state);
        result.add("nonce", nonce);
        result.add("display", display);
        result.add("prompt", prompt);
        result.add("max_age", maxAge);
        result.add("ui_locales", uiLocales);
        result.add("id_token_hint", idTokenHint);
        result.add("login_hint", loginHint);
        result.add("acr_values", acrValues);
        return result;
    }

    def String randomString() {
        return RandomStringUtils.random(RANDOM.nextInt(63) + 1);
    }
}
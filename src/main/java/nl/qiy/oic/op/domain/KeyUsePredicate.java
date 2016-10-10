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

package nl.qiy.oic.op.domain;

import java.util.function.Predicate;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;

/**
 * The key use types for {@link JWK JSON web key}. And a test that can be used to e.g. filter a stream of JWKs.
 * 
 * <pre>
 * ... 
 *     .stream()
 *     .filter(KeyUsePredicate.SIG)
 *     . ...
 * </pre>
 *
 * @author Friso Vrolijken
 * @since 31 mei 2016
 */
public enum KeyUsePredicate implements Predicate<JWK> {
    SIG(KeyUse.SIGNATURE), ENC(KeyUse.ENCRYPTION);
    private final KeyUse arg;

    private KeyUsePredicate(KeyUse filterMe) {
        this.arg = filterMe;
    }

    @Override
    public boolean test(JWK jwk) {
        return jwk.getKeyUse() == null || jwk.getKeyUse() == arg;
    }
}

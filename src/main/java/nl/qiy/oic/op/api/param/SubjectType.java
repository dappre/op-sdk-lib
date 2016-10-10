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

package nl.qiy.oic.op.api.param;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import nl.qiy.oic.op.api.ErrorCode;
import nl.qiy.oic.op.api.InputException;

/**
 * Values defined by the OAuth spec metadata subject_types_supported
 *
 * @author Friso Vrolijken
 * @since 27 mei 2016
 */
public enum SubjectType {
    PAIRWISE("pairwise"), PUBLIC("public");

    private final String type;

    private SubjectType(String type) {
        this.type = type;
    }

    @JsonCreator
    @SuppressWarnings("ucd")
    public static SubjectType get(String value) {
        for (SubjectType item : values()) {
            if (item.type.equals(value)) {
                return item;
            }
        }
        throw new InputException(ErrorCode.INVALID_REQUEST, "%s is not a valid value for subject_type", value);
    }

    @JsonValue
    @Override
    public String toString() {
        return type;
    }

}

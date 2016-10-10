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
 * Exception that is thrown to indicate that the user's input was incorrect
 *
 * @author Friso Vrolijken
 * @since 28 apr. 2016
 */
public final class InputException extends RuntimeException {
    /**
     * generated
     */
    private static final long serialVersionUID = 1L;

    private final ErrorCode error;
    private final String errorDescription;

    public InputException(ErrorCode code, String description, Object... args) {
        super(String.format(description, args));
        this.error = code;
        this.errorDescription = this.getMessage();
    }

    InputException(ErrorCode code, String description) {
        super(description);
        this.error = code;
        this.errorDescription = description;
    }

    /**
     * Simple getter
     * 
     * @return the error
     */
    public ErrorCode getError() {
        return error;
    }

    /**
     * Simple getter
     * 
     * @return the errorDescription
     */
    public String getErrorDescription() {
        return errorDescription;
    }
}

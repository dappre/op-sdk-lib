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

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple data class for an AddressClaim, as defined by
 * http://openid.net/specs/openid-connect-core-1_0.html#AddressClaim
 *
 * @author Friso Vrolijken
 * @since 14 jun. 2016
 */
public class AddressClaim implements Serializable {
    /**
     * generated
     */
    private static final long serialVersionUID = 1L;

    /**
     * Full mailing address, formatted for display or use on a mailing label. This field MAY contain multiple lines,
     * separated by newlines. Newlines can be represented either as a carriage return/line feed pair ("\r\n") or as a
     * single line feed character ("\n").
     */
    public final String formatted;
    /**
     * Full street address component, which MAY include house number, street name, Post Office Box, and multi-line
     * extended street address information. This field MAY contain multiple lines, separated by newlines. Newlines can
     * be represented either as a carriage return/line feed pair ("\r\n") or as a single line feed character ("\n").
     */
    @JsonProperty("street_address")
    public final String streetAddress;
    /**
     * City or locality component.
     */
    public final String locality;
    /**
     * State, province, prefecture, or region component.
     */
    public final String region;
    /**
     * Zip code or postal code component.
     */
    @JsonProperty("postal_code")
    public final String postalCode;
    /**
     * Country name component.
     */
    public final String country;

    @JsonCreator
    public AddressClaim(@JsonProperty("formatted") String formatted,
            @JsonProperty("street_address") String streetAddress, @JsonProperty("locality") String locality,
            @JsonProperty("region") String region, @JsonProperty("postal_code") String postalCode,
            @JsonProperty("country") String country) {
        super();
        this.formatted = formatted;
        this.streetAddress = streetAddress;
        this.locality = locality;
        this.region = region;
        this.postalCode = postalCode;
        this.country = country;
    }

}

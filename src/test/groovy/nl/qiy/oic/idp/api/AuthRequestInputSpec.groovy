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

package nl.qiy.oic.idp.api;
import javax.validation.Validation

import nl.qiy.oic.op.api.AuthenticationRequest
import nl.qiy.oic.op.api.InputException
import nl.qiy.oic.op.api.param.Display
import nl.qiy.oic.op.api.param.Prompt
import nl.qiy.oic.op.api.param.ResponseType
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * BDD testing of AuthRequestInput
 *
 * @author Friso Vrolijken
 * @since 26 apr. 2016
 */
class AuthRequestInputSpec extends Specification {
    @Shared validator = Validation.buildDefaultValidatorFactory().getValidator();
    @Shared helper = new AuthRequestInputsSpecHelper();

    @Unroll
    def "throw an error when illegal user input [#value] is given for [#key]"() {
        given: "valid user input"
        def input = helper.toMvMap("openid", "code", "A", "http://a.nl", null, null, null, null, null, null, null, null, null);

        and: "an illegal value is used to set or replace a value"
        input.putSingle(key, value);

        when: "the input object is created"
        new AuthenticationRequest(input, validator);

        then: "an InputException is thrown"
        def ex = thrown(InputException);
        ex.errorDescription.toLowerCase().contains(key.replaceAll("_", ""));

        // small chance on false positives with the random strings, but I'll take that chance
        where:
        key             | value
        "redirect_uri"  | null
        "redirect_uri"  | ""
        "redirect_uri"  | "ftp://a.com"
        "client_id"     | null
        "client_id"     | ""
        "scope"         | null
        "scope"         | ""
        "scope"         | "A"
        "scope"         | "bopenid"
        "scope"         | "openidb"
        "scope"         | "openid\\email"
        "scope"         | "\"openid\""
        "response_type" | null
        "response_type" | ""
        "response_type" | helper.randomString()
        "display"       | helper.randomString()
        "prompt"        | helper.randomString()
        "prompt"		| "none login"
        "max_age"       | "a"
        "max_age"       | helper.randomString()
        "ui_locales"    | "98797"
    }

    @Unroll
    def "accept valid user input [#value] for [#key]"() {
        given: "valid user input"
        def input = helper.toMvMap("openid", "code", "A", "http://a.nl", null, null, null, null, null, null, null, null, null);

        and: "a valid value is used to set or replace a value"
        input.putSingle(key, value);

        when: "the input object is created"
        def form = new AuthenticationRequest(input, validator);

        then: "no exception is thrown"
        notThrown(IllegalArgumentException);

        and: "the result matches the expectation"
        def fieldname = key.replaceAll( "(_)([A-Za-z0-9])", { Object[] it ->
            it[2].toUpperCase()
        } )
        form."$fieldname" == expectation

        where:
        key             | value                        | expectation
        "scope"         | "openid profile email phone" | new HashSet(["openid", "profile", "email", "phone"])
        "scope"         | "openid email phone"		   | new HashSet(["openid", "email", "phone"])
        "scope"         | "openid profile phone"	   | new HashSet(["openid", "profile", "phone"])
        "scope"         | "openid profile email"	   | new HashSet(["openid", "profile", "email"])
        "response_type" | "code"                       | EnumSet.of(ResponseType.CODE)
        "response_type" | "id_token"                   | EnumSet.of(ResponseType.ID_TOKEN)
        "response_type" | "id_token token"             | EnumSet.of(ResponseType.ID_TOKEN, ResponseType.TOKEN)
        "response_type" | "code id_token"              | EnumSet.of(ResponseType.CODE, ResponseType.ID_TOKEN)
        "response_type" | "code token"                 | EnumSet.of(ResponseType.CODE, ResponseType.TOKEN)
        "response_type" | "code id_token token"        | EnumSet.of(ResponseType.CODE, ResponseType.ID_TOKEN, ResponseType.TOKEN)
        "client_id"     | "B"                          | "B"
        "redirect_uri"  | "http://a.nl"                | new URI("http://a.nl")
        "redirect_uri"  | "https://a.nl"               | new URI("https://a.nl");
        //		"state"         | helper.randomString()        | value
        //		"nonce"         | helper.randomString()        | _
        "display"       | null                         | Display.PAGE
        "display"       | ""                           | Display.PAGE
        "display"       | "   "                        | Display.PAGE
        "display"       | "page"                       | Display.PAGE
        "display"       | "popup"                      | Display.POPUP
        "display"       | "touch"                      | Display.TOUCH
        "display"       | "wap"                        | Display.WAP
        "prompt"        | "none"                       | EnumSet.of(Prompt.NONE)
        "prompt"        | "login"                      | EnumSet.of(Prompt.LOGIN)
        "prompt"        | "consent"                    | EnumSet.of(Prompt.CONSENT)
        "prompt"        | "select_account"             | EnumSet.of(Prompt.SELECT_ACCOUNT)
        "prompt"        | "login consent"              | EnumSet.of(Prompt.LOGIN, Prompt.CONSENT)
        "max_age"       | "99" + Integer.MAX_VALUE     | new BigInteger("99" + Integer.MAX_VALUE).intValue()
        "max_age"       | "99"                         | 99
        "ui_locales"    | "fr-CA fr en"                | [Locale.forLanguageTag("fr-CA"), Locale.forLanguageTag("fr"), Locale.forLanguageTag("en")]
    }
}
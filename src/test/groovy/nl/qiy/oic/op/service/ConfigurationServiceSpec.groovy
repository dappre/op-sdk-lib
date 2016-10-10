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

package nl.qiy.oic.op.service

import java.util.function.Function

import nl.qiy.oic.op.service.spi.Configuration
import spock.lang.Specification

class ConfigurationServiceSpec extends Specification {

    // doesn't seem to work
    //	def "aaa first test"() {
    //		given:
    //		def mLogger = Mock(Logger)
    //		and:
    //		ConfigurationImpl.logger = mLogger
    //		when:
    //		ConfigurationService.get("env")
    //		then:
    //		3 * mLogger.warn(_)
    //	}

    def "call service loader wrapper once"() {
        given:
        def loader = new FCFSServiceLoaderWrapper<Configuration>(Configuration.class);
        and:
        def func = Mock(Function);
        when:
        loader.get(func);
        then:
        1 * func.apply(_);
    }

    def "return 'test' when getting 'env'"() {
        expect:
        'test' == ConfigurationService.get("env");
    }

    def "return 'value' when getting 'obj.key'"() {
        given:
        Map<String, String> obj = ConfigurationService.get("obj");

        expect:
        'value' == obj.get("key");
    }

    def "return 'obj' as Map"() {
        expect:
        ConfigurationService.get("obj") instanceof Map;
    }

    def "return 'nr' as Integer"() {
        expect:
        ConfigurationService.get("nr") instanceof Integer;
    }

    def "return 'bool' as Boolean"() {
        expect:
        ConfigurationService.get("bool") instanceof Boolean;
    }

    def "throw an exception when 'bool' is cast to Integer" () {
        when:
        Integer i = ConfigurationService.get("bool")

        then:
        thrown (ClassCastException)
    }

    def "throw an exception when getting 'nothere'"() {
        when:
        ConfigurationService.get("nothere")

        then:
        thrown(IllegalStateException)
    }

    //	def "not throw an exception when getting 'nothere' optional" () {
    //		when:
    //		ConfigurationService.getOptional("nothere")
    //
    //		then:
    //		notThrown(IllegalStateException);
    //	}
}
/*
 * Copyright (c) 2020 Georgios Voulgarakis
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

package ch.voulgarakis.fix.example.client.web;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import quickfix.SessionID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = WebEndpointsTestContext.class)
@ActiveProfiles("web-test")
class ActuatorEndpointsTest {

    @LocalServerPort
    private int port;
    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    private TestRestTemplate testRestTemplate;

    @BeforeEach
    void init() {
        testRestTemplate = new TestRestTemplate(
                restTemplateBuilder
                        .rootUri("http://localhost:" + port),
                null, null, //I don't use basic auth, if you do you can set user, pass here
                TestRestTemplate.HttpClientOption.ENABLE_COOKIES); // I needed cookie support in this particular test, you may not have this need
    }

    @Test
    void testEndpoint() {
        ResponseEntity<Map<SessionID, Properties>> response = testRestTemplate
                .exchange("/actuator/quickfixj", HttpMethod.GET, null,
                        new ParameterizedTypeReference<Map<SessionID, Properties>>() {
                        });

        assertEquals(HttpStatus.OK, response.getStatusCode());

        SessionID sessionID = new SessionID("FIX.4.0:SCompID/SSubID/SLocID->TCompID/TSubID/TLocID");
        Properties properties = new Properties();
        properties.put("StartTime", "00:00:00");
        properties.put("ConnectionType", "initiator");
        properties.put("TargetSubID ", "TSubID");
        properties.put("EndTime", "00:00:00");
        properties.put(" BeginString ", "FIX.4.0");
        properties.put("TargetLocationID", " TLocID");
        properties.put(" SenderSubID", " SSubID");
        properties.put("ReconnectInterval", "5");
        properties.put(" SessionQualifier", "Qualifier");
        properties.put(" TargetCompID", " TCompID");
        properties.put(" SocketConnectHost", " localhost");
        properties.put(" SenderCompID", " SCompID");
        properties.put(" HeartBtInt", "30");
        properties.put("Qualifier"," Qualifier");
        properties.put(" SenderLocationID"," SLocID");
        properties.put("SocketConnectPort","9876");

        assertEquals(Maps.newHashMap(sessionID,properties),response.getBody());
    }

    @Test
    void testHealth() {
        ResponseEntity<String> response = testRestTemplate
                .getForEntity("/actuator/health/quickfixj", String.class);
        System.out.println(response);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
/*-
 * ---license-start
 * EU Digital COVID Certificate Business Rule Service / dgca-businessrule-service
 * ---
 * Copyright (C) 2021 - 2022 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package eu.europa.ec.dgc.businessrule.utils.btp;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Configuration
@Profile("btp")
public class CredentialStoreConfig {

    @Value("${sap.btp.credstore.username}")
    private String username;

    @Value("${sap.btp.credstore.password}")
    private String password;

    @Value("${sap.btp.credstore.namespace}")
    private String namespace;

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder.build();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("Authorization", "Basic " + getAuthToken());
            request.getHeaders().set("sapcp-credstore-namespace", namespace);
            return execution.execute(request, body);
        });

        return restTemplate;
    }

    private String getAuthToken() {
        String authHeader = username + ":" + password;
        return Base64.getEncoder().encodeToString(authHeader.getBytes(StandardCharsets.UTF_8));
    }

}

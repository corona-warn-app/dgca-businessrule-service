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

package eu.europa.ec.dgc.businessrule.restapi.controller;

import eu.europa.ec.dgc.businessrule.service.SigningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/publickey")
@Slf4j
@RequiredArgsConstructor
public class SigningController {
    private final Optional<SigningService> signingService;

    /**
     * Http Method for getting the business rules list.
     */
    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Gets the signing public key (der base64 encoded)",
            description = "Gets the signing public key (der base64 encoded)",
            tags = {"Business Rules"},
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "public key"),
                @ApiResponse(
                        responseCode = "404",
                        description = "signing not supported"),
            }
    )
    public ResponseEntity<String> getPublicKey() {
        if (signingService.isPresent()) {
            return ResponseEntity.ok(signingService.get().getPublicKey());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}

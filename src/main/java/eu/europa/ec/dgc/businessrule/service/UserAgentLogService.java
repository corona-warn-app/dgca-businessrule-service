/*-
 * ---license-start
 * eu-digital-green-certificates / dgca-businessrule-service
 * ---
 * Copyright (C) 2022 T-Systems International GmbH and all other contributors
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

package eu.europa.ec.dgc.businessrule.service;

import eu.europa.ec.dgc.businessrule.config.DgcConfigProperties;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty("dgc.userAgentLogging.enabled")
public class UserAgentLogService {

    private static final String ENCRYPTION_ALGORITHM = "RSA";

    private final DgcConfigProperties dgcConfigProperties;

    private final UserAgentLogPersistenceService persistenceService;

    private PublicKey publicKey;

    private Cipher cipher;

    private ConcurrentHashMap<UserAgentRequestStringPair, Long> userAgentMap = new ConcurrentHashMap<>();

    /**
     * Load the PublicKey used to encrypt the gathered data.
     */
    @PostConstruct
    public void loadPublicKey() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        String publicKeyString = dgcConfigProperties.getUserAgentLogging().getEncryptionPublicKey();

        if (!isUserAgentLoggingEnabled()) {
            return;
        }

        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
        KeyFactory keyFactory = KeyFactory.getInstance(ENCRYPTION_ALGORITHM);
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        try {
            publicKey = keyFactory.generatePublic(publicKeySpec);
        } catch (InvalidKeySpecException e) {
            log.error("Could not read RSA PublicKey from Configuration: {}", e.getMessage());
        }

        cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
    }

    /**
     * Scheduler to persist the gathered data in the database.
     */
    @Scheduled(fixedRateString = "${dgc.userAgentLogging.interval}", timeUnit = TimeUnit.SECONDS)
    public void persistData() {
        if (!isUserAgentLoggingEnabled()) {
            log.debug("UserAgent Logging is disabled, ignoring persistData() scheduler");
            return;
        }

        ConcurrentHashMap<UserAgentRequestStringPair, Long> mapToSave = userAgentMap;
        userAgentMap = new ConcurrentHashMap<>();

        ZonedDateTime timestamp = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        String salt = String.valueOf(timestamp.toEpochSecond());

        mapToSave.forEach((data, count) -> {
            String encryptedUserAgent = encryptData(data.getUserAgent(), salt);
            String encryptedRequestString = encryptData(data.getRequestString(), salt);

            if (encryptedRequestString == null || encryptedUserAgent == null) {
                log.error("Encryption failed. Skipping persisting");
                return;
            }

            persistenceService.increaseCount(timestamp, encryptedUserAgent, encryptedRequestString, count);
        });
    }

    /**
     * Cleanup Job for old entities.
     */
    @Scheduled(fixedDelayString = "${dgc.userAgentLogging.cleanupInterval}", timeUnit = TimeUnit.SECONDS)
    @SchedulerLock(name = "UserAgentLogCleanup", lockAtMostFor = "PT30S")
    public void cleanup() {
        log.info("Deleting old UserAgent Log Entities");
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(dgcConfigProperties.getUserAgentLogging().getMaxAge());
        int count = persistenceService.cleanup(threshold);

        log.info("Deleted {} UserAgent Log Entities", count);
    }

    /**
     * Register the usage of UserAgent and RequestString in cache.
     *
     * @param requestString Request String describing the performed request (e.g. POST /a/b/c)
     * @param userAgent     UserAgent sent with the request
     */
    public void registerUserAgent(String requestString, String userAgent) {
        if (isUserAgentLoggingEnabled()) {
            userAgentMap.compute(new UserAgentRequestStringPair(userAgent, requestString),
                (pair, oldValue) -> oldValue == null ? 1L : oldValue + 1);
        } else {
            log.debug("UserAgent Logging is disabled, ignoring registerUserAgent() call");
        }
    }

    private boolean isUserAgentLoggingEnabled() {
        return dgcConfigProperties.getUserAgentLogging().getEnabled() != null
            && dgcConfigProperties.getUserAgentLogging().getEnabled()
            && dgcConfigProperties.getUserAgentLogging().getInterval() != null
            && dgcConfigProperties.getUserAgentLogging().getInterval() != -1
            && dgcConfigProperties.getUserAgentLogging().getEncryptionPublicKey() != null
            && !dgcConfigProperties.getUserAgentLogging().getEncryptionPublicKey().isEmpty();
    }

    private String encryptData(String data, String salt) {
        byte[] dataToEncrypt = (salt + ";" + data).getBytes(StandardCharsets.UTF_8);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(dataToEncrypt));
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            log.error("Failed to encrypt data for UserAgentLog: {}", e.getMessage(), e);
            return null;
        }
    }

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode
    private static class UserAgentRequestStringPair {
        private final String userAgent;
        private final String requestString;
    }
}

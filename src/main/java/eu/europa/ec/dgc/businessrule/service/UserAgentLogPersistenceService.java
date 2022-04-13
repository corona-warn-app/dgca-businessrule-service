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

import eu.europa.ec.dgc.businessrule.entity.UserAgentLogEntity;
import eu.europa.ec.dgc.businessrule.repository.UserAgentLogRepository;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAgentLogPersistenceService {

    private final UserAgentLogRepository userAgentLogRepository;

    /**
     * Increase the count for a combination of Timestamp, UserAgent and Request String.
     *
     * @param timestamp     ZonedDateTime with a timestamp of the request (should be truncated to minutes)
     * @param userAgent     UserAgent String (should be encrypted)
     * @param requestString String describing the performed request (should be encrypted)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    @Retryable(value = RuntimeException.class, maxAttempts = 10,
        backoff = @Backoff(delay = 150, maxDelay = 5000, random = true))
    public void increaseCount(ZonedDateTime timestamp, String userAgent, String requestString, Long amount) {
        Optional<UserAgentLogEntity> existingEntityOptional = userAgentLogRepository
            .getFirstByTimestampAndUserAgentAndAndRequestString(timestamp, userAgent, requestString);

        if (existingEntityOptional.isPresent()) {
            log.debug("Entity for K/V Pair already exists, increasing count.");
            UserAgentLogEntity existingEntity = existingEntityOptional.get();
            userAgentLogRepository.updateCount(existingEntity.getId(), existingEntity.getCount() + amount);
        } else {
            log.debug("Entity for K/V Pair does not exist, creating new one.");
            userAgentLogRepository.save(new UserAgentLogEntity(null, timestamp, userAgent, requestString, amount));
        }
    }

    /**
     * Delete entities older than provided threshold.
     *
     * @param threshold Threshold timestamp
     * @return Number of deleted entities
     */
    @Transactional
    @Retryable(value = RuntimeException.class, maxAttempts = 10, backoff = @Backoff(delay = 2000, maxDelay = 10000))
    public int cleanup(ZonedDateTime threshold) {
        return userAgentLogRepository.cleanup(threshold);
    }
}

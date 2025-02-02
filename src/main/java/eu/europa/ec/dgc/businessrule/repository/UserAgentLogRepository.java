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

package eu.europa.ec.dgc.businessrule.repository;

import eu.europa.ec.dgc.businessrule.entity.UserAgentLogEntity;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAgentLogRepository extends JpaRepository<UserAgentLogEntity, Long> {

    Optional<UserAgentLogEntity> getFirstByTimestampAndUserAgentAndAndRequestString(
        ZonedDateTime timestamp, String userAgent, String requestString);

    @Query("UPDATE UserAgentLogEntity e SET e.count = :count WHERE e.id = :id")
    @Modifying
    int updateCount(@Param("id") Long id, @Param("count") Long count);

    @Query("DELETE FROM UserAgentLogEntity e WHERE e.timestamp < :threshold")
    @Modifying
    int cleanup(@Param("threshold") ZonedDateTime threshold);

}

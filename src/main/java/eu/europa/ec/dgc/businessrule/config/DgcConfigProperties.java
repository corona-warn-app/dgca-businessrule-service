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

package eu.europa.ec.dgc.businessrule.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("dgc")
public class DgcConfigProperties {

    private final DownloadSetting businessRulesDownload = new DownloadSetting();

    private final DownloadSetting valueSetsDownload = new DownloadSetting();

    private final DownloadSetting countryListDownload = new DownloadSetting();

    private final DownloadSetting boosterNotificationRulesDownload = new DownloadSetting();

    private final DownloadSetting cclRulesDownload = new DownloadSetting();

    private final DownloadSetting domesticRulesDownload = new DownloadSetting();

    private String allowedCorsUrls;

    private UserAgentLogging userAgentLogging = new UserAgentLogging();

    @Getter
    @Setter
    public static class UserAgentLogging {
        private Integer interval;
        private Integer cleanupInterval;
        private Integer maxAge;
        private String encryptionPublicKey;
    }

    @Getter
    @Setter
    public static class DownloadSetting {
        private Integer timeInterval;
        private Integer lockLimit;
    }


}


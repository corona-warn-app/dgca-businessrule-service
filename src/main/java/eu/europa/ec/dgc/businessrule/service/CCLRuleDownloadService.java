/*-
 * ---license-start
 * eu-digital-green-certificates / dgca-businessrule-service
 * ---
 * Copyright (C) 2021 T-Systems International GmbH and all other contributors
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

import eu.europa.ec.dgc.businessrule.exception.BoosterNotificationRuleParseException;
import eu.europa.ec.dgc.businessrule.exception.CCLRuleParseException;
import eu.europa.ec.dgc.businessrule.model.BoosterNotificationRuleItem;
import eu.europa.ec.dgc.businessrule.model.CCLRuleItem;
import eu.europa.ec.dgc.businessrule.utils.BusinessRulesUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultTemplate;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * A service to download the cclrules from the vault.
 */
@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty("dgc.cclRulesDownload.enabled")
public class CCLRuleDownloadService {

    private final String identifierKeyName = "identifier";
    private final String versionKeyName = "version";
    private final String rawDataKeyName = "raw_data";
    private final List<String> expectedKeys = Arrays.asList(
        identifierKeyName,
        versionKeyName,
        rawDataKeyName);

    @Value("${dgc.cclRulesDownload.key-store}")
    private String keyStoreName;

    @Value("${dgc.cclRulesDownload.base-path}")
    private String rulesBasePath;

    private final VaultTemplate vaultTemplate;
    private final BusinessRulesUtils businessRulesUtils;
    private final CCLRuleService cclRuleService;

    /**
     * A service to download the ccl rules from a vault key value store.
     */
    @Scheduled(fixedDelayString = "${dgc.cclRulesDownload.timeInterval}")
    public void downloadRules() {

        List<CCLRuleItem> ruleItems = new ArrayList<>();

        log.info("CCL rules download started");

        VaultKeyValueOperations kv = vaultTemplate.opsForKeyValue(
            keyStoreName,
            VaultKeyValueOperationsSupport.KeyValueBackend.KV_2);

        List<String> ruleKeys = kv.list(rulesBasePath);


        for (String ruleKey : ruleKeys) {
            CCLRuleItem ruleItem;

            try {
                ruleItem = getRuleFromVaultData(kv, ruleKey);
                ruleItems.add(ruleItem);
            } catch (NoSuchAlgorithmException e) {
                log.error("Failed to hash ccl rules on download.", e);
                return;
            } catch (CCLRuleParseException e) {
                log.error("Failed to parse rule with rule key: " + ruleKey);
            }
        }

        if (!ruleItems.isEmpty()) {
            cclRuleService.updateRules(ruleItems);
        } else {
            log.warn("The download of the CCL rules seems to fail, as the download connector "
                + "returns an empty rules list.-> No data was changed.");
        }


        log.info("CCL rules download finished");
    }

    private CCLRuleItem getRuleFromVaultData(VaultKeyValueOperations kv, String ruleKey)
        throws NoSuchAlgorithmException, CCLRuleParseException {
        CCLRuleItem ruleItem = new CCLRuleItem();
        Map<String, Object> ruleRawData = kv.get(ruleKey).getData();

        if (!ruleRawData.keySet().containsAll(expectedKeys)) {
            log.info(String.format(
                "Not all expected keys value pairs present. Expected: %s , Received: %s",
                expectedKeys,
                ruleRawData.keySet()));
            throw new CCLRuleParseException();
        }

        ruleItem.setIdentifier(ruleRawData.get(identifierKeyName).toString());
        ruleItem.setVersion(ruleRawData.get(versionKeyName).toString());
        ruleItem.setRawData(ruleRawData.get(rawDataKeyName).toString());
        ruleItem.setHash(businessRulesUtils.calculateHash(ruleItem.getRawData()));

        return ruleItem;
    }

}



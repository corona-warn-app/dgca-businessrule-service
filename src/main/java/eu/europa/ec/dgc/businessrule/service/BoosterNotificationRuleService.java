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

import eu.europa.ec.dgc.businessrule.entity.ListType;
import eu.europa.ec.dgc.businessrule.entity.SignedListEntity;
import eu.europa.ec.dgc.businessrule.model.BoosterNotificationRuleItem;
import eu.europa.ec.dgc.businessrule.repository.SignedListRepository;
import eu.europa.ec.dgc.businessrule.restapi.dto.BoosterNotificationRuleListItemDto;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@RequiredArgsConstructor
@Service
public class BoosterNotificationRuleService {

    private final Map<String, BoosterNotificationRuleItem> boosterNotificationRuleMap = new HashMap<>();
    private final ListSigningService listSigningService;
    private final Optional<SigningService> signingService;
    private final SignedListRepository signedListRepository;

    /**
     * Creates the signature for the empty rules list after start up.
     */
    @PostConstruct
    @Transactional
    public void boosterNotificationRuleServiceInit() {
        listSigningService.updateSignedList(getRulesList(), ListType.BnRules);
    }


    /**
     * Gets list of all rules ids and hashes.
     */
    public List<BoosterNotificationRuleListItemDto> getRulesList() {

        return boosterNotificationRuleMap.values().stream()
            .sorted(Comparator.comparing(BoosterNotificationRuleItem::getIdentifier))
            .map(rule -> new BoosterNotificationRuleListItemDto(
                rule.getIdentifier(),
                rule.getVersion(),
                rule.getHash()
            )).collect(Collectors.toList());
    }

    public Optional<SignedListEntity> getRulesSignedList() {
        return signedListRepository.findById(ListType.BnRules);
    }


    /**
     * Gets  a rule by hash.
     */
    @Transactional
    public BoosterNotificationRuleItem getRuleByHash(String hash) {
        return boosterNotificationRuleMap.get(hash);
    }

    /**
     * Updates the list of rules.
     *
     * @param rules list of actual value sets
     */
    @Transactional
    public void updateRules(List<BoosterNotificationRuleItem> rules) {
        boosterNotificationRuleMap.clear();

        for (BoosterNotificationRuleItem rule : rules) {
            saveRule(rule);
        }

        listSigningService.updateSignedList(getRulesList(), ListType.BnRules);
    }

    /**
     * Saves a rule.
     *
     * @param rule The rule to be saved.
     */
    @Transactional
    public void saveRule(BoosterNotificationRuleItem rule) {
        signingService.ifPresent(service -> rule.setSignature(service.computeSignature(rule.getHash())));
        boosterNotificationRuleMap.put(rule.getHash(), rule);
    }

}

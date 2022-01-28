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

package eu.europa.ec.dgc.businessrule.service;

import eu.europa.ec.dgc.businessrule.entity.CountryListEntity;
import eu.europa.ec.dgc.businessrule.repository.CountryListRepository;
import eu.europa.ec.dgc.gateway.connector.DgcGatewayCountryListDownloadConnector;
import eu.europa.ec.dgc.gateway.connector.DgcGatewayValidationRuleDownloadConnector;
import eu.europa.ec.dgc.gateway.connector.DgcGatewayValueSetDownloadConnector;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;


@SpringBootTest
@AutoConfigureMockMvc
class CountryListServiceTest {

    @MockBean
    DgcGatewayValidationRuleDownloadConnector dgcGatewayValidationRuleDownloadConnector;

    @MockBean
    DgcGatewayValueSetDownloadConnector dgcGatewayValueSetDownloadConnector;

    @MockBean
    DgcGatewayCountryListDownloadConnector dgcGatewayCountryListDownloadConnector;

    @Autowired
    CountryListService countryListService;

    @Autowired
    CountryListRepository countryListRepository;

    @BeforeEach
    void clearRepositoryData() {
        countryListRepository.deleteAll();
    }


    @Test
    void updateCountryList()  {
        String countryList_1 = "[\"BE\", \"EL\", \"LT\", \"PT\", \"BG\"]";
        String countryList_2 = "[\"SE\", \"DE\", \"EU\", \"CZ\", \"DK\"]";

        countryListService.updateCountryList(countryList_1);

        List<CountryListEntity> cl = countryListRepository.findAll();
        Assertions.assertEquals(1, cl.size());
        CountryListEntity cle = cl.get(0);
        Assertions.assertEquals(countryList_1, cle.getRawData());

        countryListService.updateCountryList(countryList_2);

        cl = countryListRepository.findAll();
        Assertions.assertEquals(1, cl.size());
        cle = cl.get(0);
        Assertions.assertEquals(countryList_2, cle.getRawData());

        countryListService.updateCountryList(countryList_2);

        cl = countryListRepository.findAll();
        Assertions.assertEquals(1, cl.size());
        cle = cl.get(0);
        Assertions.assertEquals(countryList_2, cle.getRawData());

        countryListService.updateCountryList(countryList_1);

        cl = countryListRepository.findAll();
        Assertions.assertEquals(1, cl.size());
        cle = cl.get(0);
        Assertions.assertEquals(countryList_1, cle.getRawData());
    }

}

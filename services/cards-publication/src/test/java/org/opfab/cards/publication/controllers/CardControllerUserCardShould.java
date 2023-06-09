/* Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

package org.opfab.cards.publication.controllers;


import org.assertj.core.api.Assertions;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opfab.cards.publication.application.UnitTestApplication;
import org.opfab.cards.publication.model.CardPublicationData;
import org.opfab.cards.publication.model.PublisherTypeEnum;
import org.opfab.cards.publication.repositories.CardRepositoryForTest;
import org.opfab.springtools.configuration.test.WithMockOpFabUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.List;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = UnitTestApplication.class)
@ActiveProfiles("test")
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithMockOpFabUser(login = "someUser")
class CardControllerUserCardShould extends CardControllerShouldBase {

	String cardUid;
	int cardNumber = 2;


    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CardRepositoryForTest cardRepository;

    @BeforeAll
    void setup() {
        this.mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
	    EasyRandom randomGenerator = instantiateEasyRandom();
		List<CardPublicationData> cardsInRepository = instantiateCardPublicationData(randomGenerator, cardNumber);
		cardUid = cardsInRepository.get(0).getUid();
		cardRepository.saveAll(cardsInRepository);
    }


	@AfterAll
	void clean() {
		cardRepository.deleteAll();
	}

	@Test
	void deleteNonExistingUserCard() throws Exception {
		String cardId = "NotExistingCardId";
		Optional <CardPublicationData> card = cardRepository.findById(cardId);
		Assertions.assertThat(card).isNotPresent();
		mockMvc.perform(delete("/cards/userCard/" + cardId)).andExpect(status().isNotFound());
	}

	@Test
	void deleteUserCardWithForbiddenError() throws Exception {
		CardPublicationData card = cardRepository.findByUid(cardUid).get();
		Assertions.assertThat(card.getPublisherType()).isNotEqualTo(PublisherTypeEnum.ENTITY);
		mockMvc.perform(delete("/cards/userCard/" + card.getId())).andExpect(status().isForbidden());
	}
}

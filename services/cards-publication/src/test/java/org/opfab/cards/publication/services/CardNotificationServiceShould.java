/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */



package org.opfab.cards.publication.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opfab.cards.model.CardOperationTypeEnum;
import org.opfab.cards.model.SeverityEnum;
import org.opfab.cards.publication.configuration.TestCardReceiver;
import org.opfab.cards.publication.model.CardOperationData;
import org.opfab.cards.publication.model.CardPublicationData;
import org.opfab.cards.publication.model.I18nPublicationData;
import org.opfab.cards.publication.application.UnitTestApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = UnitTestApplication.class)
@ActiveProfiles(profiles = {"native","test"})
class CardNotificationServiceShould {

    @Autowired
    CardNotificationService cardNotificationService;

    @Autowired
    TestCardReceiver testCardReceiver;

    @BeforeEach
    @AfterEach
    public void clearData(){
        testCardReceiver.clear();
    }

    @Test
    void transmitCards(){
        Instant start = Instant.now().plusSeconds(3600);
        CardPublicationData newCard = CardPublicationData.builder()
           .publisher("PUBLISHER_1")
           .processVersion("0.0.1")
           .processInstanceId("PROCESS_1")
           .severity(SeverityEnum.ALARM)
           .startDate(start)
           .title(I18nPublicationData.builder().key("title").build())
           .summary(I18nPublicationData.builder().key("summary").parameter("arg1","value1").build())
           .lttd(start.minusSeconds(600))
           .userRecipients(Arrays.asList("graham", "eric"))
           .groupRecipients(Arrays.asList("mytso", "admin"))
           .build();

        cardNotificationService.notifyOneCard(newCard,CardOperationTypeEnum.ADD);
        await().pollDelay(1, TimeUnit.SECONDS).until(()->true);
        assertThat(testCardReceiver.getCardQueue()).hasSize(1);

        CardOperationData cardOperationData = testCardReceiver.getCardQueue().element();
        List<String> groupRecipientsIds = cardOperationData.getGroupRecipientsIds();
        assertThat(groupRecipientsIds)
            .hasSize(2)
            .contains("mytso")
            .contains("admin");

        List<String> userRecipientsIds = cardOperationData.getUserRecipientsIds();
        assertThat(userRecipientsIds)
            .hasSize(2)
            .contains("graham")
            .contains("eric");
    }
}

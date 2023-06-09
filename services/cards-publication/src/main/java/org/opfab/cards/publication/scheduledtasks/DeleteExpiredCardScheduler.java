/* Copyright (c) 2022, Alliander (http://www.alliander.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

package org.opfab.cards.publication.scheduledtasks;

import lombok.extern.slf4j.Slf4j;
import org.opfab.cards.publication.services.CardProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@EnableScheduling
public class DeleteExpiredCardScheduler {

    @Autowired
    CardProcessingService cardProcessingService;

    @Value("${delayForDeleteExpiredCardsScheduling:60000}") String delayForDeleteExpiredCardsScheduling;

    @Scheduled(fixedDelayString = "${delayForDeleteExpiredCardsScheduling:60000}")
    public void deleteExpiredCards() {
        cardProcessingService.deleteCardsByExpirationDate(Instant.now());
    }
}

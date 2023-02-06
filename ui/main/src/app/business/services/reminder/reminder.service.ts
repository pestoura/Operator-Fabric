/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {Injectable} from '@angular/core';
import {CardService} from '@ofServices/card.service';
import {ReminderList} from './reminderList';
import {AcknowledgeService} from 'app/business/services/acknowledge.service';
import {LightCardsStoreService} from '@ofServices/lightcards/lightcards-store.service';
import {SoundNotificationService} from '@ofServices/sound-notification.service';
import {LogOption, OpfabLoggerService} from 'app/business/services/logs/opfab-logger.service';
import {ServerResponseStatus} from 'app/business/server/serverResponse';

@Injectable({
    providedIn: 'root'
})
export class ReminderService {
    private reminderList: ReminderList;

    constructor(
        private cardService: CardService,
        private acknowledgeService: AcknowledgeService,
        private lightCardsStoreService: LightCardsStoreService,
        private soundNotificationService: SoundNotificationService,
        private logger: OpfabLoggerService
    ) {}

    public startService(userLogin: string) {
        this.logger.info('Reminder : starting service');
        this.reminderList = new ReminderList(userLogin);
        this.listenForCardsToAddInReminder();
        this.checkForCardToRemind();
    }

    private checkForCardToRemind() {
        const cardsIdToRemind = this.reminderList.getCardIdsToRemindNow();
        cardsIdToRemind.forEach((cardId) => this.remindCard(cardId));
        setTimeout(() => this.checkForCardToRemind(), 5000);
    }

    private listenForCardsToAddInReminder() {
        this.lightCardsStoreService.getNewLightCards().subscribe((card) => {
            if (!!card) this.reminderList.addAReminder(card);
        });
    }

    private remindCard(cardId) {
        this.logger.info('Reminder : will remind card = ' + cardId, LogOption.LOCAL_AND_REMOTE);
        const lightCard = this.lightCardsStoreService.getLightCard(cardId);

        if (!!lightCard) {
            this.reminderList.setCardHasBeenRemind(lightCard);
            this.acknowledgeService.deleteUserAcknowledgement(lightCard.uid).subscribe((resp) => {
                if (resp.status !== ServerResponseStatus.OK)
                    this.logger.error(
                        'Reminder : the remote acknowledgement endpoint returned an error status' + resp.status
                    );
            });
            this.cardService.deleteUserCardRead(lightCard.uid).subscribe((resp) => {
                if (!(resp.status === 200 || resp.status === 204))
                    this.logger.error('Reminder : the remote read endpoint returned an error status' + resp.status);
            });
            this.lightCardsStoreService.setLightCardAcknowledgment(cardId, false);
            this.lightCardsStoreService.setLightCardRead(cardId, false);
            this.soundNotificationService.handleRemindCard(lightCard);
        } else {
            // the card has been deleted in this case
            this.reminderList.removeAReminder(cardId);
        }
    }
}
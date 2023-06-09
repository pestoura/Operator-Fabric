/* Copyright (c) 2018-2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {AfterViewChecked, Component, Input, OnInit} from '@angular/core';
import {LightCard} from '@ofModel/light-card.model';
import {Observable} from 'rxjs';
import {NgbModalRef} from '@ng-bootstrap/ng-bootstrap/modal/modal-ref';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ConfigService} from 'app/business/services/config.service';
import {MessageLevel} from '@ofModel/message.model';
import {ProcessesService} from 'app/business/services/processes.service';
import {AcknowledgeService} from 'app/business/services/acknowledge.service';
import {UserService} from 'app/business/services/user.service';
import {UserWithPerimeters} from '@ofModel/userWithPerimeters.model';
import {EntitiesService} from 'app/business/services/entities.service';
import {GroupedCardsService} from 'app/business/services/grouped-cards.service';
import {AlertMessageService} from 'app/business/services/alert-message.service';
import {Router} from '@angular/router';

@Component({
    selector: 'of-card-list',
    templateUrl: './card-list.component.html',
    styleUrls: ['./card-list.component.scss']
})
export class CardListComponent implements AfterViewChecked, OnInit {
    @Input() public lightCards: LightCard[];
    @Input() public selection: Observable<string>;
    @Input() public totalNumberOfLightsCards: number;

    modalRef: NgbModalRef;
    hideAckAllCardsFeature: boolean;
    ackAllCardsDemandTimestamp: number;
    currentUserWithPerimeters: UserWithPerimeters;

    domCardListElement;

    constructor(
        private modalService: NgbModal,
        private configService: ConfigService,
        private processesService: ProcessesService,
        private acknowledgeService: AcknowledgeService,
        private userService: UserService,
        private entitiesService: EntitiesService,
        private groupedCardsService: GroupedCardsService,
        private alertMessageService: AlertMessageService,
        private router: Router
    ) {
        this.currentUserWithPerimeters = this.userService.getCurrentUserWithPerimeters();
    }

    ngOnInit(): void {
        this.domCardListElement = document.getElementById('opfab-card-list');
        this.hideAckAllCardsFeature = this.configService.getConfigValue('feed.card.hideAckAllCardsFeature', true);
    }

    ngAfterViewChecked() {
        this.adaptFrameHeight();
    }

    adaptFrameHeight() {
        const rect = this.domCardListElement.getBoundingClientRect();
        let height = window.innerHeight - rect.top - 30;
        if (this.hideAckAllCardsFeature) height = window.innerHeight - rect.top - 10;
        this.domCardListElement.style.maxHeight = `${height}px`;
    }

    acknowledgeAllVisibleCardsInTheFeed() {
        this.lightCards.forEach((lightCard) => {
            this.acknowledgeVisibleCardInTheFeed(lightCard);
            this.groupedCardsService
                .getChildCardsByTags(lightCard.tags)
                .forEach((groupedCard) => this.acknowledgeVisibleCardInTheFeed(groupedCard));
        });
    }

    private acknowledgeVisibleCardInTheFeed(lightCard: LightCard): void {
        const processDefinition = this.processesService.getProcess(lightCard.process);
        if (
            !lightCard.hasBeenAcknowledged &&
            this.isCardPublishedBeforeAckDemand(lightCard) &&
            this.acknowledgeService.isAcknowledgmentAllowed(
                this.currentUserWithPerimeters,
                lightCard,
                processDefinition
            )
        ) {
            try {
                const entitiesAcks = [];
                const entities = this.entitiesService.getEntitiesFromIds(
                    this.currentUserWithPerimeters.userData.entities
                );
                entities.forEach((entity) => {
                    if (entity.entityAllowedToSendCard)
                        // this avoids to display entities used only for grouping
                        entitiesAcks.push(entity.id);
                });
                this.acknowledgeService.acknowledgeCard(lightCard, entitiesAcks);
            } catch (err) {
                console.error(err);
                this.displayMessage('response.error.ack', null, MessageLevel.ERROR);
            }
        }
    }

    isCardPublishedBeforeAckDemand(lightCard: LightCard): boolean {
        return lightCard.publishDate < this.ackAllCardsDemandTimestamp;
    }

    private displayMessage(i18nKey: string, msg: string, severity: MessageLevel = MessageLevel.ERROR) {
        this.alertMessageService.sendAlertMessage({message: msg, level: severity, i18n: {key: i18nKey}});
    }

    open(content) {
        this.ackAllCardsDemandTimestamp = Date.now();
        this.modalRef = this.modalService.open(content, {centered: true});
    }

    confirmAckAllCards() {
        this.modalRef.close();
        this.acknowledgeAllVisibleCardsInTheFeed();
        this.router.navigate(['/feed']);
    }

    declineAckAllCards(): void {
        this.modalRef.dismiss();
    }

    isCardInGroup(selected: string, id: string) {
        return this.groupedCardsService.isCardInGroup(selected, id);
    }
}

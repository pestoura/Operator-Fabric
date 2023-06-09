/* Copyright (c) 2018-2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges, ViewEncapsulation} from '@angular/core';
import {Card} from '@ofModel/card.model';
import {ProcessesService} from 'app/business/services/processes.service';
import {SafeHtml} from '@angular/platform-browser';
import {AcknowledgmentAllowedEnum, State} from '@ofModel/processes.model';
import {map, takeUntil} from 'rxjs/operators';
import {Subject} from 'rxjs';
import {User} from '@ofModel/user.model';
import {UserService} from 'app/business/services/user.service';
import {EntitiesService} from 'app/business/services/entities.service';
import {NgbModalRef} from '@ng-bootstrap/ng-bootstrap';
import {UserPermissionsService} from 'app/business/services/user-permissions.service';
import {DisplayContext} from '@ofModel/templateGateway.model';
import {LightCardsStoreService} from 'app/business/services/lightcards/lightcards-store.service';
import {CardComponent} from '../../card.component';
import {OpfabLoggerService} from 'app/business/services/logs/opfab-logger.service';
import {UserWithPerimeters} from "@ofModel/userWithPerimeters.model";
import {SelectedCardService} from 'app/business/services/card/selectedCard.service';
import {CardService} from 'app/business/services/card.service';
import {RouterStore,PageType} from 'app/business/store/router.store';
import {Router} from '@angular/router';

declare const templateGateway: any;

@Component({
    selector: 'of-card-body',
    templateUrl: './card-body.component.html',
    styleUrls: ['./card-body.component.scss'],
    encapsulation: ViewEncapsulation.None
})
export class CardBodyComponent implements OnChanges, OnInit, OnDestroy {
    @Input() cardState: State;
    @Input() card: Card;
    @Input() childCards: Card[];

    @Input() parentModalRef: NgbModalRef;
    @Input() screenSize: string;
    @Input() parentComponent: CardComponent;

    @Output() closeCardDetail: EventEmitter<boolean> = new EventEmitter<boolean>();

    public displayContext: DisplayContext = DisplayContext.REALTIME;
    public isUserEnabledToRespond = false;
    public lttdExpiredIsTrue: boolean;
    private regularlyLttdCheckActive = false;

    public isResponseLocked = false;
    public fullscreen = false;
    public showMaxAndReduceButton = false;
    public showDetailCardHeader = false;
    public htmlTemplateContent: SafeHtml;
    public templateOffset = 15;

    private lastCardSetToReadButNotYetOnFeed;
    private entityIdsAllowedOrRequiredToRespondAndAllowedToSendCards = [];
    public userEntityIdsPossibleForResponse = [];
    private userEntityIdToUseForResponse = '';
    private userMemberOfAnEntityRequiredToRespondAndAllowedToSendCards = false;
    private unsubscribe$: Subject<void> = new Subject<void>();

    public user: User;
    private userWithPerimeters: UserWithPerimeters;

    constructor(
        private businessconfigService: ProcessesService,
        private routerStore: RouterStore,
        private cardService: CardService,
        private router: Router,
        private userService: UserService,
        private entitiesService: EntitiesService,
        private userPermissionsService: UserPermissionsService,
        private lightCardsStoreService: LightCardsStoreService,
        private selectedCardService: SelectedCardService,
        private logger: OpfabLoggerService
    ) {
        this.userWithPerimeters = this.userService.getCurrentUserWithPerimeters();
        if (!!this.userWithPerimeters) {
            this.user = this.userWithPerimeters.userData;
        }
    }

    ngOnInit() {
        this.integrateChildCardsInRealTime();
        const pageType = this.routerStore.getCurrentPageType();
        if (pageType === PageType.MONITORING || pageType === PageType.CALENDAR)
            this.templateOffset = 35;
        if (pageType !== PageType.CALENDAR && pageType !== PageType.MONITORING)
            this.showMaxAndReduceButton = true;
    }

    private integrateChildCardsInRealTime() {
        this.lightCardsStoreService
            .getNewLightChildCards()
            .pipe(
                takeUntil(this.unsubscribe$),
                map((lastCardLoaded) => {
                    if (!!lastCardLoaded) {
                        if (
                            lastCardLoaded.parentCardId === this.card.id &&
                            !this.childCards.map((childCard) => childCard.uid).includes(lastCardLoaded.uid)
                        ) {
                            this.integrateOneChildCard(lastCardLoaded);
                        }
                    }
                })
            )
            .subscribe();

        this.lightCardsStoreService
            .getDeletedChildCardsIds()
            .pipe(
                takeUntil(this.unsubscribe$),
                map((lastCardDeleted) => {
                    if (
                        !!lastCardDeleted &&
                        lastCardDeleted.parentCardId === this.card.id &&
                        this.childCards.map((childCard) => childCard.id).includes(lastCardDeleted.cardId)
                    ) {
                        this.removeChildCard(lastCardDeleted.cardId);
                    }
                })
            )
            .subscribe();
    }

    private integrateOneChildCard(newChildCard: Card) {
        this.cardService.loadCard(newChildCard.id).subscribe((cardData) => {
            const newChildArray = this.childCards.filter((childCard) => childCard.id !== cardData.card.id);
            newChildArray.push(cardData.card);
            this.childCards = newChildArray;
            templateGateway.childCards = this.childCards;
            templateGateway.applyChildCards();
            this.lockResponseIfOneUserEntityHasAlreadyRespond();
            if (this.isResponseLocked) templateGateway.lockAnswer();
        });
    }

    private lockResponseIfOneUserEntityHasAlreadyRespond() {
        this.isResponseLocked = false;
        for (const e of this.childCards.map((c) => c.publisher)) {
            if (this.user.entities.includes(e)) {
                this.isResponseLocked = true;
                break;
            }
        }
    }

    private removeChildCard(deletedChildCardId: string) {
        const newChildArray = this.childCards.filter((childCard) => childCard.id !== deletedChildCardId);
        this.childCards = newChildArray;
        this.lockResponseIfOneUserEntityHasAlreadyRespond();
        templateGateway.isLocked = this.isResponseLocked;
        if (!this.isResponseLocked) templateGateway.unlockAnswer();
        templateGateway.childCards = this.childCards;
        templateGateway.applyChildCards();
    }

    public unlockAnswer() {
        this.isResponseLocked = false;
        templateGateway.unlockAnswer();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (!!changes.card || !!changes.cardState) {
            if (this.cardState.response != null && this.cardState.response !== undefined) {
                this.computeEntityIdsAllowedOrRequiredToRespondAndAllowedToSendCards();
                this.computeUserEntityIdsPossibleForResponse();
                this.computeUserMemberOfAnEntityRequiredToRespondAndAllowedToSendCards();
                this.isUserEnabledToRespond = this.userPermissionsService.isUserEnabledToRespond(
                    this.userService.getCurrentUserWithPerimeters(),
                    this.card,
                    this.businessconfigService.getProcess(this.card.process)
                );
            }
            this.computeShowDetailCardHeader();
            this.lockResponseIfOneUserEntityHasAlreadyRespond();
            this.markAsReadIfNecessary();
        }
    }

    private computeEntityIdsAllowedOrRequiredToRespondAndAllowedToSendCards() {
        let entityIdsAllowedOrRequiredToRespond = [];
        if (this.card.entitiesAllowedToRespond)
            entityIdsAllowedOrRequiredToRespond = entityIdsAllowedOrRequiredToRespond.concat(
                this.card.entitiesAllowedToRespond
            );
        if (this.card.entitiesRequiredToRespond)
            entityIdsAllowedOrRequiredToRespond = entityIdsAllowedOrRequiredToRespond.concat(
                this.card.entitiesRequiredToRespond
            );

        const entitiesAllowedOrRequiredToRespond = this.entitiesService.getEntitiesFromIds(
            entityIdsAllowedOrRequiredToRespond
        );
        this.entityIdsAllowedOrRequiredToRespondAndAllowedToSendCards = this.entitiesService
            .resolveEntitiesAllowedToSendCards(entitiesAllowedOrRequiredToRespond)
            .map((entity) => entity.id);

        this.logger.debug(
            `Detail card - entities allowed to respond = ${this.entityIdsAllowedOrRequiredToRespondAndAllowedToSendCards}`
        );
    }

    private computeUserEntityIdsPossibleForResponse() {
        this.userEntityIdsPossibleForResponse = this.entityIdsAllowedOrRequiredToRespondAndAllowedToSendCards.filter(
            (entityId) => this.user.entities.includes(entityId)
        );
        this.logger.debug(`Detail card - user entities allowed to respond = ${this.userEntityIdsPossibleForResponse}`);
        if (this.userEntityIdsPossibleForResponse.length === 1)
            this.userEntityIdToUseForResponse = this.userEntityIdsPossibleForResponse[0];
    }

    private computeUserMemberOfAnEntityRequiredToRespondAndAllowedToSendCards() {
        if (!this.card.entitiesRequiredToRespond) {
            this.userMemberOfAnEntityRequiredToRespondAndAllowedToSendCards = false;
            return;
        }

        const entitiesRequiredToRespond = this.entitiesService.getEntitiesFromIds(this.card.entitiesRequiredToRespond);

        const entityIdsRequiredToRespondAndAllowedToSendCards = this.entitiesService
            .resolveEntitiesAllowedToSendCards(entitiesRequiredToRespond)
            .map((entity) => entity.id);

        const userEntitiesRequiredToRespondAndAllowedToSendCards =
            entityIdsRequiredToRespondAndAllowedToSendCards.filter((entityId) => this.user.entities.includes(entityId));
        this.userMemberOfAnEntityRequiredToRespondAndAllowedToSendCards =
            userEntitiesRequiredToRespondAndAllowedToSendCards.length > 0;
    }

    public computeShowDetailCardHeader() {
        this.showDetailCardHeader =
            (!this.cardState.showDetailCardHeader || this.cardState.showDetailCardHeader === true) &&
            this.cardState.response != null &&
            this.cardState.response !== undefined;
    }

    private markAsReadIfNecessary() {
        if (this.card.hasBeenRead === false) {
            // we do not set now the card as read in the store, as we want to keep
            // the card as unread in the feed
            // we will set it read in the feed when
            //  - we close the card
            //  - we exit the feed (i.e destroy the card)
            //  - we change card

            if (this.lastCardSetToReadButNotYetOnFeed) {
                // if the user has changed selected card in feed, set the previous read card as read in the feed
                if (this.card.id !== this.lastCardSetToReadButNotYetOnFeed.id)
                    this.updateLastReadCardStatusOnFeedIfNeeded();
            }
            this.lastCardSetToReadButNotYetOnFeed = this.card;
            this.cardService.postUserCardRead(this.card.uid).subscribe();
        } else this.updateLastReadCardStatusOnFeedIfNeeded();
    }

    private updateLastReadCardStatusOnFeedIfNeeded() {
        if (this.lastCardSetToReadButNotYetOnFeed) {
            this.lightCardsStoreService.setLightCardRead(this.lastCardSetToReadButNotYetOnFeed.id, true);
            this.lastCardSetToReadButNotYetOnFeed = null;
        }
    }

    public displayCardAcknowledgedFooter(): boolean {
        return (
            this.cardState.acknowledgmentAllowed !== AcknowledgmentAllowedEnum.NEVER &&
            !!this.card.entityRecipients &&
            this.card.entityRecipients.length > 0 &&
            this.userPermissionsService.isUserAuthorizedToSeeAcknowledgmentFooter(this.userWithPerimeters, this.card)
        );
    }

    public beforeTemplateRendering() {
        this.setTemplateGatewayVariables();
        this.stopRegularlyCheckLttd();
    }

    private setTemplateGatewayVariables() {
        templateGateway.childCards = this.childCards;
        templateGateway.isLocked = this.isResponseLocked;
        templateGateway.userAllowedToRespond = this.isUserEnabledToRespond;
        templateGateway.entitiesAllowedToRespond = this.entityIdsAllowedOrRequiredToRespondAndAllowedToSendCards;
        templateGateway.userMemberOfAnEntityRequiredToRespond =
            this.userMemberOfAnEntityRequiredToRespondAndAllowedToSendCards;
        templateGateway.entityUsedForUserResponse = this.userEntityIdToUseForResponse;
    }

    private stopRegularlyCheckLttd() {
        this.regularlyLttdCheckActive = false;
        this.lttdExpiredIsTrue = false;
    }

    public afterTemplateRendering() {
        if (this.isResponseLocked) templateGateway.lockAnswer();
        this.startRegularlyCheckLttd();
        
    }

    private startRegularlyCheckLttd() {
        this.regularlyLttdCheckActive = true;
        this.regularlyCheckLttd();
    }

    private regularlyCheckLttd() {
        if (this.card.lttd && !this.lttdExpiredIsTrue && this.regularlyLttdCheckActive) {
            if (this.isLttdExpired()) {
                this.lttdExpiredIsTrue = true;
                templateGateway.setLttdExpired(true);
            } else setTimeout(() => this.regularlyCheckLttd(), 500);
        }
    }

    private isLttdExpired(): boolean {
        return this.card.lttd != null && this.card.lttd - new Date().getTime() <= 0;
    }

    public isSmallscreen() {
        return window.innerWidth < 1000;
    }

    public setFullScreen(active) {
        this.fullscreen = active;
        if (!!this.parentComponent) this.parentComponent.screenSize = active ? 'lg' : 'md';
    }

    public closeDetails() {
        this.closeCardDetail.next(true);
        this.updateLastReadCardStatusOnFeedIfNeeded();
        if (this.parentModalRef) {
            this.parentModalRef.close();
            this.selectedCardService.clearSelectedCardId();
        } else {
            this.selectedCardService.clearSelectedCardId();
            this.router.navigate(['/' + this.routerStore.getCurrentRoute()]);
        }
    }

    ngOnDestroy() {
        this.updateLastReadCardStatusOnFeedIfNeeded();
        this.unsubscribe$.next();
        this.unsubscribe$.complete();
    }
}

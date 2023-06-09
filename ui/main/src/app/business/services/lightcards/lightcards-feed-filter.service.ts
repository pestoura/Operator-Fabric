/* Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {Injectable} from '@angular/core';
import {debounceTime, map} from 'rxjs/operators';
import {combineLatest, Observable, ReplaySubject, Subject} from 'rxjs';
import {LightCard} from '@ofModel/light-card.model';
import {LightCardsStoreService} from './lightcards-store.service';
import {FilterService} from './filter.service';
import {SortService} from './sort.service';
import {GroupedCardsService} from 'app/business/services/grouped-cards.service';
import {ConfigService} from 'app/business/services/config.service';
import {LogOption, OpfabLoggerService} from 'app/business/services/logs/opfab-logger.service';
import {SearchService} from './search-service';

@Injectable({
    providedIn: 'root'
})
export class LightCardsFeedFilterService {
    private filteredAndSortedLightCards = new Subject();
    private filteredLightCards = new Subject();
    private filteredAndSearchedLightCards = new ReplaySubject(1);
    private filteredLightCardsForTimeLine = new Subject();
    private onlyBusinessFilterForTimeLine = new Subject();

    constructor(
        private lightCardsStoreService: LightCardsStoreService,
        private filterService: FilterService,
        private sortService: SortService,
        private searchService: SearchService,
        private groupedCardsService: GroupedCardsService,
        private configService: ConfigService,
        private logger : OpfabLoggerService
    ) {
        this.computeFilteredAndSortedLightCards();
        this.computeFilteredAndSearchedLightCards();
        this.computeFilteredLightCards();
        this.onlyBusinessFilterForTimeLine.next(false);
    }

    private computeFilteredAndSortedLightCards() {
        combineLatest([this.sortService.getSortFunctionChanges(), this.getFilteredAndSearchedLightCards()])
            .pipe(
                map((results) => {
                    results[1] = results[1].sort(results[0]);
                    if (this.isGroupedCardsEnabled()) {
                        this.groupedCardsService.computeGroupedCards(results[1]);
                        results[1] = this.groupedCardsService.filterGroupedChilds(results[1]);
                    }
                    return results[1];
                })
            )
            .subscribe((lightCards) => this.filteredAndSortedLightCards.next(lightCards));
    }

    public getFilteredLightCards(): Observable<any> {
        return this.filteredLightCards.asObservable();
    }

    public getFilteredLightCardsForTimeLine(): Observable<any> {
        return this.filteredLightCardsForTimeLine.asObservable();
    }

    public getFilteredAndSearchedLightCards() : Observable<any> {
        return this.filteredAndSearchedLightCards.asObservable();
    }

    private computeFilteredLightCards() {
        combineLatest([
            this.filterService.getFiltersChanges(),
            this.lightCardsStoreService.getLightCards(),
            this.onlyBusinessFilterForTimeLine.asObservable()
        ])
            .pipe(
                debounceTime(50), // When resetting components it can happen that we have more than one filter change
                // with debounceTime, we avoid processing intermediate states
                map((results) => {
                    const lightCards = results[1];
                    const onlyBusinessFitlerForTimeLine = results[2];

                    this.logger.debug('Number of cards in memory : ' +  results[1].length ,LogOption.LOCAL_AND_REMOTE);

                    if (onlyBusinessFitlerForTimeLine) {
                        const cardFilteredByBusinessDate =
                            this.filterService.filterLightCardsOnlyByBusinessDate(lightCards);
                        this.filteredLightCardsForTimeLine.next(cardFilteredByBusinessDate);
                        return this.filterService.filterLightCardsWithoutBusinessDate(cardFilteredByBusinessDate);
                    }

                    const cardFilter = this.filterService.filterLightCards(lightCards);
                    this.filteredLightCardsForTimeLine.next(cardFilter);
                    return cardFilter;
                })
            )
            .subscribe((lightCards) => {
                this.filteredLightCards.next(lightCards);
            });
    }

    private computeFilteredAndSearchedLightCards() {
        combineLatest([this.searchService.getSearchChanges(), this.getFilteredLightCards()])
            .pipe(map((results) => {
                return this.searchService.searchLightCards(results[1]);
            }))
            .subscribe((lightCards) => {
                this.logger.debug('Number of cards visible after filtering and searching : ' +  lightCards.length, LogOption.LOCAL_AND_REMOTE);
                this.filteredAndSearchedLightCards.next(lightCards);
            });
    }

    private isGroupedCardsEnabled(): boolean {
        return this.configService.getConfigValue('feed.enableGroupedCards', false);
    }

    public isCardVisibleInFeed(card: LightCard) {
        return this.searchService.searchLightCards(this.filterService.filterLightCards([card])).length > 0;
    }

    public getFilteredAndSortedLightCards(): Observable<any> {
        return this.filteredAndSortedLightCards.asObservable();
    }

    public setOnlyBusinessFilterForTimeLine(onlyBusinessFilterForTimeLine: boolean) {
        this.onlyBusinessFilterForTimeLine.next(onlyBusinessFilterForTimeLine);
    }
}

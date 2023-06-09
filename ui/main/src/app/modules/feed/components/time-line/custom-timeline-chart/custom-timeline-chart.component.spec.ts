/* Copyright (c) 2018-2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';
import {CustomTimelineChartComponent} from './custom-timeline-chart.component';
import {APP_BASE_HREF, CommonModule} from '@angular/common';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {FormsModule} from '@angular/forms';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import * as moment from 'moment';
import {MouseWheelDirective} from '../directives/mouse-wheel.directive';
import {RouterTestingModule} from '@angular/router/testing';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {LightCardsFeedFilterService} from 'app/business/services/lightcards/lightcards-feed-filter.service';
import {LightCardsServiceMock} from '@tests/mocks/lightcards.service.mock';
import {DateTimeFormatterService} from 'app/business/services/date-time-formatter.service';
import {RemoteLoggerService} from 'app/business/services/logs/remote-logger.service';
import {RemoteLoggerServiceMockAngular} from '@tests/mocks/remote-logger.service-angular.mock';

describe('CustomTimelineChartComponent', () => {
    let component: CustomTimelineChartComponent;
    let fixture: ComponentFixture<CustomTimelineChartComponent>;

    const startDate = moment().startOf('year');
    const endDate = moment().startOf('year').add(1, 'year');

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                CommonModule,
                BrowserAnimationsModule,
                FormsModule,
                RouterTestingModule,
                NgxChartsModule,
                NgbModule,
                HttpClientTestingModule
            ],
            declarations: [CustomTimelineChartComponent, MouseWheelDirective],
            providers: [
                {provide: APP_BASE_HREF, useValue: '/'},
                {provide: DateTimeFormatterService, useClass: DateTimeFormatterService},
                {provide: LightCardsFeedFilterService, useClass: LightCardsServiceMock},
                {provide: RemoteLoggerService, useClass: RemoteLoggerServiceMockAngular}
            ],
            schemas: [NO_ERRORS_SCHEMA]
        }).compileComponents();
        fixture = TestBed.createComponent(CustomTimelineChartComponent);
        component = fixture.componentInstance;
        component.xDomain = [moment().valueOf, moment().add(1, 'year').valueOf];
    }));

    it('should call update() and create chart by calling updateXAxisWidth function', () => {
        fixture.detectChanges();
        component.domainId = 'W';
        component.updateXAxisHeight({height: 1024});
        expect(component).toBeTruthy();
    });

    it('set no circle if no card ', () => {
        fixture.detectChanges();
        component.domainId = 'Y';
        component.xDomainForTimeLineGridDisplay = [startDate, endDate];
        component.setXTicksValue();
        component.cardsData = [];
        component.createCircles();
        expect(component.circles.length).toEqual(0);
    });

    it('set one circle with count=1 if one card ', () => {
        fixture.detectChanges();
        component.domainId = 'Y';
        component.xDomainForTimeLineGridDisplay = [startDate, endDate];
        component.setXTicksValue();

        // Test 1
        const card1 = {
            date: moment(startDate).add(5, 'days').add(1, 'hours'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        component.cardsData = [card1];
        component.createCircles();

        expect(component.circles.length).toEqual(1);
        expect(component.circles[0].count).toEqual(1);
        expect(component.circles[0].circleYPosition).toEqual(4);
    });

    it('set no circle if one card is after time domain ', () => {
        fixture.detectChanges();
        component.domainId = 'Y';
        component.xDomainForTimeLineGridDisplay = [startDate, endDate];
        component.setXTicksValue();
        // Test 1
        const card1 = {
            date: moment(endDate).add(1, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        component.cardsData = [card1];
        component.createCircles();

        expect(component.circles.length).toEqual(0);
    });

    it('set one circle with count=2 if two card in the same interval and same severity ', () => {
        fixture.detectChanges();
        component.domainId = 'Y';
        component.xDomainForTimeLineGridDisplay = [startDate, endDate];
        component.setXTicksValue();

        const card1 = {
            date: moment(startDate).add(2, 'day').add(1, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        const card2 = {
            date: moment(startDate).add(4, 'day').add(2, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        component.cardsData = [card1, card2];
        component.createCircles();

        expect(component.circles.length).toEqual(1);
        expect(component.circles[0].count).toEqual(2);
    });

    it('set two circles with count=1 if two card are not in the same interval but same severity ', () => {
        fixture.detectChanges();
        component.domainId = 'Y';
        component.xDomainForTimeLineGridDisplay = [startDate, endDate];
        component.setXTicksValue();

        const card1 = {
            date: moment(startDate).add(2, 'day').add(1, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        const card2 = {
            date: moment(startDate).add(2, 'month').add(1, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        component.cardsData = [card1, card2];
        component.createCircles();

        expect(component.circles.length).toEqual(2);
        expect(component.circles[0].count).toEqual(1);
        expect(component.circles[1].count).toEqual(1);
    });

    it('set two circles with count=1 if two cards are in the same interval but not same severity ', () => {
        fixture.detectChanges();
        component.domainId = 'Y';
        component.xDomainForTimeLineGridDisplay = [startDate, endDate];
        component.setXTicksValue();

        const card1 = {
            date: moment(startDate).add(2, 'day').add(1, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        const card2 = {
            date: moment(startDate).add(4, 'day').add(1, 'hour'),
            severity: 'INFORMATION',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        component.cardsData = [card1, card2];
        component.createCircles();

        expect(component.circles.length).toEqual(2);
        expect(component.circles[0].count).toEqual(1);
        expect(component.circles[1].count).toEqual(1);
    });

    it('set two circles with one count=1 and one count= 2 if three cards of same severity  and 2 in the same interval ', () => {
        fixture.detectChanges();
        component.domainId = 'Y';
        component.xDomainForTimeLineGridDisplay = [startDate, endDate];
        component.setXTicksValue();

        const card1 = {
            date: moment(startDate).add(2, 'day').add(1, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        const card2 = {
            date: moment(startDate).add(4, 'day').add(1, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        const card3 = {
            date: moment(startDate).add(6, 'month').add(1, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        component.cardsData = [card1, card2, card3];
        component.createCircles();

        expect(component.circles.length).toEqual(2);
        expect(component.circles[0].count).toEqual(2);
        expect(component.circles[1].count).toEqual(1);
    });

    it('set three card in the same interval and same severity , the end date should be the max date of the cards ', () => {
        fixture.detectChanges();
        component.domainId = 'Y';
        component.xDomainForTimeLineGridDisplay = [startDate, endDate];
        component.setXTicksValue();

        const card1 = {
            date: moment(startDate).add(2, 'day').add(1, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        const card2 = {
            date: moment(startDate).add(5, 'day').add(1, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        const card3 = {
            date: moment(startDate).add(3, 'day').add(1, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        component.cardsData = [card1, card2, card3];
        component.createCircles();

        expect(component.circles.length).toEqual(1);
        expect(component.circles[0].end).toEqual(card2.date);
    });

    it('set three cards in the same interval and same severity , there shoud be one circle with 3 summary  ', () => {
        fixture.detectChanges();
        component.domainId = 'Y';
        component.xDomainForTimeLineGridDisplay = [startDate, endDate];
        component.setXTicksValue();

        const card1 = {
            date: moment(startDate).add(5, 'day').add(1, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        const card2 = {
            date: moment(startDate).add(6, 'day').add(3, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        const card3 = {
            date: moment(startDate).add(8, 'day').add(8, 'hour'),
            severity: 'ALARM',
            summary: {parameters: 'param', key: 'process'},
            publisher: 'TEST',
            processVersion: '1'
        };

        component.cardsData = [card1, card2, card3];
        component.createCircles();

        expect(component.circles.length).toEqual(1);
        expect(component.circles[0].summary.length).toEqual(3);
    });
});

/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {navigationRoutes} from '../../app-routing.module';
import {Store} from '@ngrx/store';
import {TryToLogOutAction} from '@ofActions/authentication.actions';
import {AppState} from '@ofStore/index';
import {selectCurrentUrl} from '@ofSelectors/router.selectors';
import {LoadMenuAction} from '@ofActions/menu.actions';
import {selectMenuStateMenu} from '@ofSelectors/menu.selectors';
import {BehaviorSubject, Observable} from 'rxjs';
import {CoreMenuConfig, Menu} from '@ofModel/menu.model';
import {map, tap} from 'rxjs/operators';
import * as _ from 'lodash-es';
import {GlobalStyleService} from '@ofServices/global-style.service';
import {Route} from '@angular/router';
import {ConfigService} from '@ofServices/config.service';
import {UserPreferencesService} from '@ofServices/user-preference.service';
import {QueryAllEntitiesAction} from '@ofActions/user.actions';
import {UserService} from '@ofServices/user.service';
import {NgbModal, NgbModalOptions, NgbModalRef} from '@ng-bootstrap/ng-bootstrap';
import {AppService} from '@ofServices/app.service';
import {SwPush} from '@angular/service-worker';
import {PushNotificationService} from '@ofServices/push-notification.service';

@Component({
    selector: 'of-navbar',
    templateUrl: './navbar.component.html',
    styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit {
    private static nightMode: BehaviorSubject<boolean>;

    navbarCollapsed = true;
    navigationRoutes: Route[];
    currentPath: string[];
    private _businessconfigMenus: Observable<Menu[]>;
    expandedMenu: boolean[] = [];

    modalRef: NgbModalRef;
    @ViewChild('userCard') userCardTemplate: ElementRef;

    @ViewChild('about') aboutTemplate: ElementRef;

    customLogo: string;
    height: number;
    width: number;
    limitSize: boolean;
    displayAdmin: boolean;
    displayActivityArea: boolean;
    displayFeedConfiguration: boolean;
    displayRealTimeUsers: boolean;
    displayExternalDevicesConfiguration: boolean;
    displayCreateUserCard: boolean;
    displayCalendar: boolean;
    displaySettings: boolean;
    displayAbout: boolean;
    displayLogOut: boolean;
    displayChangePassword: boolean;
    displayEnvironmentName = false;
    environmentName: string;
    environmentColor: string;
    nightDayMode = false;
    logoutInProgress = false;

    vapIdPublicKey: string;
    pushSubscriptionActive: boolean;
    pushSubscription: PushSubscription;

    constructor(
        private store: Store<AppState>,
        private globalStyleService: GlobalStyleService,
        private configService: ConfigService,
        private userService: UserService,
        private modalService: NgbModal,
        private appService: AppService,
        private userPreferences: UserPreferencesService,
        private swPush: SwPush,
        private pushNotificationService: PushNotificationService
    ) {
        this.currentPath = ['']; // Initializing currentPath to avoid 'undefined' errors when it is used to determine 'active' look in template
    }

    ngOnInit() {
        this.store.select(selectCurrentUrl).subscribe((url) => {
            if (url) {
                this.currentPath = url.split('/');
            }
        });
        this._businessconfigMenus = this.store.select(selectMenuStateMenu).pipe(
            map((menus) => this.getCurrentUserCustomMenus(menus)),
            tap((menus) => {
                this.expandedMenu = new Array<boolean>(menus.length);
                _.fill(this.expandedMenu, false);
            })
        );
        this.store.dispatch(new LoadMenuAction());
        this.store.dispatch(new QueryAllEntitiesAction());

        const logo = this.configService.getConfigValue('logo.base64');
        if (!!logo) {
            this.customLogo = `data:image/svg+xml;base64,${logo}`;
        }
        const logo_height = this.configService.getConfigValue('logo.height');
        if (!!logo_height) {
            this.height = logo_height;
        }

        const logo_width = this.configService.getConfigValue('logo.width');
        if (!!logo_width) {
            this.width = logo_width;
        }

        const logo_limitSize = this.configService.getConfigValue('logo.limitSize');
        this.limitSize = logo_limitSize === true;

        const visibleCoreMenus = this.computeVisibleCoreMenusForCurrentUser();
        const settings = this.configService.getConfigValue('settings');

        this.navigationRoutes = navigationRoutes.filter((route) => visibleCoreMenus.includes(route.path));
        this.displayAdmin = visibleCoreMenus.includes('admin');
        this.displayActivityArea = visibleCoreMenus.includes('activityarea');
        this.displayFeedConfiguration = visibleCoreMenus.includes('feedconfiguration');
        this.displayRealTimeUsers = visibleCoreMenus.includes('realtimeusers');
        this.displayExternalDevicesConfiguration = visibleCoreMenus.includes('externaldevicesconfiguration');
        this.displayCreateUserCard = visibleCoreMenus.includes('usercard');
        this.displayCalendar = visibleCoreMenus.includes('calendar');
        this.displaySettings = visibleCoreMenus.includes('settings');
        this.displayAbout = visibleCoreMenus.includes('about');
        this.displayLogOut = visibleCoreMenus.includes('logout');
        this.displayChangePassword = visibleCoreMenus.includes('changepassword');
        this.nightDayMode = visibleCoreMenus.includes('nightdaymode');

        this.environmentName = this.configService.getConfigValue('environmentName');
        this.environmentColor = this.configService.getConfigValue('environmentColor', 'blue');
        if (!!this.environmentName) this.displayEnvironmentName = true;

        if (!this.nightDayMode) {
            if (settings && settings.styleWhenNightDayModeDesactivated) {
                this.globalStyleService.setStyle(settings.styleWhenNightDayModeDesactivated);
            }
        } else {
            this.loadNightModeFromUserPreferences();
        }
        this.vapIdPublicKey = this.configService.getConfigValue('webPush.publicKey');
        console.log("Push notification enabled : " + this.swPush.isEnabled);
        this.swPush.subscription.subscribe( sub =>  {
            this.pushSubscriptionActive = (sub != null);
            this.pushSubscription = sub;
            console.log("Subscribed to push notifications = " + this.pushSubscriptionActive);
        });
    }

    private getCurrentUserCustomMenus(menus: Menu[]): Menu[] {
        const filteredMenus = [];
        menus.forEach((m) => {
            const entries = m.entries.filter(
                (e) => !e.showOnlyForGroups || this.userService.isCurrentUserInAnyGroup(e.showOnlyForGroups)
            );
            if (entries.length > 0) {
                filteredMenus.push(new Menu(m.id, m.label, entries));
            }
        });
        return filteredMenus;
    }

    private computeVisibleCoreMenusForCurrentUser(): string[] {
        const coreMenuConfiguration = this.configService.getCoreMenuConfiguration();

        if (coreMenuConfiguration) {
            return coreMenuConfiguration
                .filter((coreMenuConfig: CoreMenuConfig) => {
                    return (
                        coreMenuConfig.visible &&
                        (!coreMenuConfig.showOnlyForGroups ||
                            coreMenuConfig.showOnlyForGroups.length == 0 ||
                            this.userService.isCurrentUserInAnyGroup(coreMenuConfig.showOnlyForGroups))
                    );
                })
                .map((coreMenuConfig: CoreMenuConfig) => coreMenuConfig.id);
        } else {
            console.log('No coreMenusConfiguration property set in ui-menu.json');
            return [];
        }
    }

    logOut() {
        this.logoutInProgress = true;
        this.store.dispatch(new TryToLogOutAction());
    }

    get businessconfigMenus() {
        return this._businessconfigMenus;
    }

    toggleMenu(index: number) {
        this.expandedMenu[index] = !this.expandedMenu[index];
        if (this.expandedMenu[index]) {
            setTimeout(() => (this.expandedMenu[index] = false), 5000);
        }
    }

    private loadNightModeFromUserPreferences() {
        NavbarComponent.nightMode = new BehaviorSubject<boolean>(true);
        const nightMode = this.userPreferences.getPreference('opfab.nightMode');
        if (nightMode !== null && nightMode === 'false') {
            NavbarComponent.nightMode.next(false);
            this.globalStyleService.setStyle('DAY');
        } else {
            this.globalStyleService.setStyle('NIGHT');
        }
    }

    switchToNightMode() {
        this.globalStyleService.setStyle('NIGHT');
        NavbarComponent.nightMode.next(true);
        this.userPreferences.setPreference('opfab.nightMode', 'true');
    }

    switchToDayMode() {
        this.globalStyleService.setStyle('DAY');
        NavbarComponent.nightMode.next(false);
        this.userPreferences.setPreference('opfab.nightMode', 'false');
    }

    getNightMode(): Observable<boolean> {
        return NavbarComponent.nightMode.asObservable();
    }

    openCardCreation() {
        /**
     We can not have in the same time a card open in the feed and a preview of user card, so
     we close the card if one is open in the feed

     This lead to a BUG :

     In case the user was watching in the feed a card with response activated
     he may not be able to see child cards after closing the usercard form

     REASONS :

     The card template in the preview  may redefine method templateGateway.applyChild
     This will override method templateGateway.applyChild  form the card on the feed
     As a consequence, the card on the feed will never receive new (or updated) child cards

     Furthermore, having the same template open twice in the application may cause unwanted behavior as
     we could have duplicated element html ids in the html document.

     */
        if (this.currentPath[1] === 'feed') this.appService.closeDetails();

        const options: NgbModalOptions = {
            size: 'usercard',
            backdrop: 'static'
        };
        this.modalRef = this.modalService.open(this.userCardTemplate, options);
    }

    showAbout() {
        this.modalService.open(this.aboutTemplate, {centered: true});
    }

    subscribe() {
        console.log("Subscribe to push notifications");
        this.swPush.requestSubscription({
            serverPublicKey: this.vapIdPublicKey
        })
        .then(sub => this.handleSubscription(sub))
        .catch(err => console.error("Could not subscribe to notifications", err));
    }

    handleSubscription(sub: PushSubscription) {
        console.log("Save push subscriprion: " + JSON.stringify(sub.toJSON()));
        this.pushSubscription = sub;
        this.pushNotificationService.sendSubscriptionToServer(sub).subscribe();
    }

    unsubscribe() {
        console.log("UnSubscribe from push notification " + this.pushSubscription.endpoint);
        this.pushNotificationService.deleteSubscription(this.pushSubscription).subscribe();

        this.swPush.unsubscribe()
        .then(sub => {
            console.log("Unsubscribed");
        })
        .catch(err => console.error("Could not unsubscribe from push notifications", err));
    }
}

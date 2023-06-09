/* Copyright (c) 2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {Injectable} from '@angular/core';
import * as moment from 'moment';
import {TranslateService} from '@ngx-translate/core';
import {Observable} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';
import {ConfigService} from 'app/business/services/config.service';
import {Utilities} from 'app/business/common/utilities';
import {ConfigServer} from '../server/config.server';
import {ServerResponseStatus} from '../server/serverResponse';

declare const opfab: any;

@Injectable({
    providedIn: 'root'
})
export class I18nService {
    private static localUrl = '/assets/i18n/';
    private _locale: string;

    constructor(
        private configServer: ConfigServer,
        private translate: TranslateService,
        private configService: ConfigService
    ) {
    }


    public initLocale() {
        this.configService
            .getConfigValueAsObservable('settings.locale', 'en')
            .subscribe((locale) => this.changeLocale(locale));
    }

    public changeLocale(locale: string) {
        if (locale) {
            this._locale = locale;
        } else {
            this._locale = 'en';
        }
        moment.locale(this._locale);
        this.translate.use(this._locale);
        this.setTranslationForMultiSelectUsedInTemplates();
    }

    public setTranslationForMultiSelectUsedInTemplates() {
        this.translate
            .get('multiSelect.searchPlaceholderText')
            .subscribe((translated) => (opfab.multiSelect.searchPlaceholderText = translated));
        this.translate
            .get('multiSelect.clearButtonText')
            .subscribe((translated) => (opfab.multiSelect.clearButtonText = translated));
        this.translate
            .get('multiSelect.noOptionsText')
            .subscribe((translated) => (opfab.multiSelect.noOptionsText = translated));
        this.translate
            .get('multiSelect.noSearchResultsText')
            .subscribe((translated) => (opfab.multiSelect.noSearchResultsText = translated));
    }

    public get locale() {
        return this._locale;
    }

    public loadLocale(locale: string): Observable<any> {
        return this.configServer.getLocale(locale).pipe(
            tap({
                next: (serverResponse) => {
                    if (serverResponse.status === ServerResponseStatus.OK) {
                     this.translate.setTranslation(locale, serverResponse.data, true)
                    }
                    else {
                        console.log(
                            new Date().toISOString(),
                            `Error : impossible to load locale ${I18nService.localUrl}${locale}.json`
                        )
                    }},
            })
        );
    }

    public loadGlobalTranslations(locales: Array<string>): Observable<any[]> {
        if (!!locales) {
            const localeRequests$ = [];
            locales.forEach((locale) => localeRequests$.push(this.loadLocale(locale)));
            return Utilities.subscribeAndWaitForAllObservablesToEmitAnEvent(localeRequests$);
        }
    }

    public loadTranslationForMenu(): void {
        this.configService.fetchMenuTranslations().subscribe((locales) => {
            locales.forEach((locale) => {
                this.translate.setTranslation(locale.language, locale.i18n, true);
            });
        });

        catchError((err, caught) => {
            console.error('Impossible to load configuration file ui-menu.json', err);
            return caught;
        });
    }
}

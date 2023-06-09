/* Copyright (c) 2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {HttpClient, HttpHeaders} from '@angular/common/http';
import {environment} from '@env/environment';
import {I18n} from '@ofModel/i18n.model';
import {Message, MessageLevel} from '@ofModel/message.model';
import {ConfigService} from 'app/business/services/config.service';
import {OpfabLoggerService} from 'app/business/services/logs/opfab-logger.service';
import {AuthenticatedUser} from './auth.model';
import {Guid} from 'guid-typescript';
import jwt_decode from 'jwt-decode';
import {Observable, of, Subject} from 'rxjs';

export const ONE_SECOND = 1000;
export const MILLIS_TO_WAIT_BETWEEN_TOKEN_EXPIRATION_DATE_CONTROLS = 5000;
export abstract class AuthHandler {
    protected checkTokenUrl = `${environment.urls.auth}/check_token`;
    protected askTokenUrl = `${environment.urls.auth}/token`;

    protected sessionExpired = new Subject<boolean>();
    protected userAuthenticated = new Subject<AuthenticatedUser>();
    protected rejectAuthentication = new Subject<Message>();

    protected secondsToCloseSession: number;
    protected clientId: string;
    protected delegateUrl: string;
    protected loginClaim: string;
    protected expireClaim: string;

    constructor(configService: ConfigService, protected httpClient: HttpClient, protected logger: OpfabLoggerService) {
        this.secondsToCloseSession = configService.getConfigValue('secondsToCloseSession', 60);
        this.clientId = configService.getConfigValue('security.oauth2.client-id', null);
        this.delegateUrl = configService.getConfigValue('security.oauth2.flow.delegate-url', null);
        this.loginClaim = configService.getConfigValue('security.jwt.login-claim', 'sub');
        this.expireClaim = configService.getConfigValue('security.jwt.expire-claim', 'exp');
    }

    abstract initializeAuthentication();

    public tryToLogin(username: string, password: string) {
        // Not implemented by default
    }
    public logout() {
        // not implemented by default
    }

    public getSessionExpired(): Observable<boolean> {
        return this.sessionExpired.asObservable();
    }

    public getUserAuthenticated(): Observable<AuthenticatedUser> {
        return this.userAuthenticated.asObservable();
    }

    public getRejectAuthentication(): Observable<Message> {
        return this.rejectAuthentication.asObservable();
    }

    protected checkAuthentication(): Observable<any> {
        const token = localStorage.getItem('token');
        if (!!token) {
            const postData = new URLSearchParams();
            postData.append('token', token);
            const headers = new HttpHeaders({'Content-type': 'application/x-www-form-urlencoded; charset=utf-8'});
            return this.httpClient.post(this.checkTokenUrl, postData.toString(), {headers: headers});
        }
        return of(null);
    }

    protected getUserFromAuthInfo(authInfo: HttpAuthInfo): AuthenticatedUser {
        let expirationDate;
        const jwt = this.decodeToken(authInfo.access_token);
        if (!!authInfo.expires_in) {
            expirationDate = Date.now() + ONE_SECOND * authInfo.expires_in;
        } else if (!!this.expireClaim) {
            expirationDate = jwt[this.expireClaim];
        } else {
            expirationDate = 0;
        }
        const user = new AuthenticatedUser();
        user.login = jwt[this.loginClaim];
        user.token = authInfo.access_token;
        user.expirationDate = new Date(expirationDate);
        return user;
    }

    private decodeToken(token: string): any {
        try {
            return jwt_decode(token);
        } catch (Error) {
            return null;
        }
    }

    protected handleErrorOnTokenGeneration(errorResponse, category: string) {
        let message, key;
        const params = new Map<string, string>();
        switch (errorResponse.status) {
            case 401:
                message = 'Unable to authenticate the user';
                key = `login.error.${category}`;
                break;
            case 0:
            case 500:
                message = 'Authentication service currently unavailable';
                key = 'login.error.unavailable';
                break;
            default:
                message = 'Unexpected error';
                key = 'login.error.unexpected';
                params['error'] = errorResponse.message;
        }
        this.logger.error(message + ' - ' + JSON.stringify(errorResponse));
        this.rejectAuthentication.next(new Message(message, MessageLevel.ERROR, new I18n(key, params)));
    }

    public regularCheckTokenValidity() {
        if (this.verifyExpirationDate()) {
            setTimeout(() => {
                this.regularCheckTokenValidity();
            }, MILLIS_TO_WAIT_BETWEEN_TOKEN_EXPIRATION_DATE_CONTROLS);
        } else {
            // Will send Logout if token is expired
            this.sessionExpired.next(true);
        }
    }
    private verifyExpirationDate(): boolean {
        // + to convert the stored number as a string back to number
        const expirationDate = +localStorage.getItem('expirationDate');
        const isNotANumber = isNaN(expirationDate);
        const stillValid = expirationDate - this.secondsToCloseSession * 1000 > Date.now();
        return !isNotANumber && stillValid;
    }

    protected isExpirationDateOver(): boolean {
        return !this.verifyExpirationDate();
    }
}

export class HttpAuthInfo {
    constructor(
        public access_token: string,
        public expires_in: number,
        public clientId: Guid,
        public identifier?: string
    ) {}
}

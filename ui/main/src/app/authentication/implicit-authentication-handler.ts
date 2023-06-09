/* Copyright (c) 2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {AuthConfig, EventType as OAuthType, JwksValidationHandler, OAuthEvent, OAuthService} from 'angular-oauth2-oidc';
import {AuthenticatedUser} from './auth.model';
import {ConfigService} from 'app/business/services/config.service';
import {environment} from '@env/environment';
import {AuthHandler} from './auth-handler';
import {HttpClient} from '@angular/common/http';
import {OpfabLoggerService} from 'app/business/services/logs/opfab-logger.service';

export class ImplicitAuthenticationHandler extends AuthHandler {
    constructor(
        configService: ConfigService,
        httpClient: HttpClient,
        logger: OpfabLoggerService,
        private oauthService: OAuthService
    ) {
        super(configService, httpClient, logger);
    }

    public async initializeAuthentication() {
        const authConfig: AuthConfig = {
            issuer: this.delegateUrl,
            redirectUri: window.location.origin + environment.urls.authentication,
            silentRefreshRedirectUri: window.location.origin + this.getPathEnd() + '/silent-refresh.html',
            clientId: this.clientId,
            scope: 'openid profile email',
            showDebugInformation: false,
            sessionChecksEnabled: false,
            clearHashAfterLogin: false,
            requireHttps: false
        };
        this.oauthService.configure(authConfig);
        this.oauthService.setupAutomaticSilentRefresh();
        this.oauthService.tokenValidationHandler = new JwksValidationHandler();
        await this.oauthService.loadDiscoveryDocument().then(() => {
            this.login();
        });
        this.oauthService.events.subscribe((e) => {
            this.dispatchOAuth2Events(e);
        });
    }

    private getPathEnd(): string {
        return location.pathname.length > 1 ? location.pathname : '';
    }

    private async login() {
        await this.oauthService.tryLogin().then(() => {
            if (this.oauthService.hasValidAccessToken()) {
                this.setUserAuthenticated();
            } else {
                sessionStorage.setItem('flow', 'implicit');
                this.oauthService.initImplicitFlow();
            }
        });
    }

    private setUserAuthenticated() {
        const user = new AuthenticatedUser();
        const identityClaims = this.oauthService.getIdentityClaims();
        user.login = identityClaims['sub'];
        user.token = this.oauthService.getAccessToken();
        user.expirationDate = new Date(this.oauthService.getAccessTokenExpiration());
        this.userAuthenticated.next(user);
    }

    private dispatchOAuth2Events(event: OAuthEvent) {
        const eventType: OAuthType = event.type;
        switch (eventType) {
            // We can have a token_error or token_refresh_error when it is not possible to refresh token
            // This case arise for example when using a SSO and the session is not valid anymore (session timeout)
            case 'token_error':
            case 'token_refresh_error':
                this.sessionExpired.next(true);
                break;
            case 'logout': {
                console.log('Logout from implicit flow');
                break;
            }
        }
    }

    public regularCheckTokenValidity() {
        // Override because there is no regularly check in implicit mode
        // it is done via the oauthService
    }

    public logout() {
        this.oauthService.logOut();
    }
}

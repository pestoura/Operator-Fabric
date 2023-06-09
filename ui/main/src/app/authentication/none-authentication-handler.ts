/* Copyright (c) 2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {AuthenticatedUser} from './auth.model';
import {AuthHandler} from './auth-handler';
import {UserService} from 'app/business/services/user.service';
import {Message, MessageLevel} from '@ofModel/message.model';
import {ConfigService} from 'app/business/services/config.service';
import {HttpClient} from '@angular/common/http';
import {OpfabLoggerService} from 'app/business/services/logs/opfab-logger.service';

export class NoneAuthenticationHandler extends AuthHandler {
    constructor(
        configService: ConfigService,
        httpClient: HttpClient,
        logger: OpfabLoggerService,
        private userService: UserService
    ) {
        super(configService, httpClient, logger);
    }

    initializeAuthentication() {
        this.userService.currentUserWithPerimeters().subscribe((foundUser) => {
            if (foundUser != null) {
                this.logger.info('None auth mode - User (' + foundUser.userData.login + ') found');
                const user = new AuthenticatedUser();
                user.login = foundUser.userData.login;
                this.userAuthenticated.next(user);
            } else {
                this.logger.error('None auth mode - Unable to authenticate the user');
                this.rejectAuthentication.next(new Message('Unable to authenticate user', MessageLevel.ERROR));
            }
        });
    }

    regularCheckTokenValidity() {
        // Override because there is no regularly check in none mode
    }
}

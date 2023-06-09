/* Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {Component, OnInit, TemplateRef, ViewChild, ViewEncapsulation} from '@angular/core';
import {NgbModal, NgbModalRef} from '@ng-bootstrap/ng-bootstrap';
import {LogOption, OpfabLoggerService} from 'app/business/services/logs/opfab-logger.service';
import {SoundNotificationService} from 'app/business/services/sound-notification.service';
import {OpfabEventStreamService} from 'app/business/services/opfabEventStream.service';
import {AuthService} from 'app/authentication/auth.service';
import {CurrentUserStore} from 'app/business/store/current-user.store';

@Component({
    selector: 'of-session-end',
    styleUrls: ['./session-end.component.scss'],
    encapsulation: ViewEncapsulation.None,
    templateUrl: './session-end.component.html'
})
export class SessionEndComponent implements OnInit {
    public isDisconnectedByNewUser = false;
    private modalRef: NgbModalRef;
    @ViewChild('sessionEnd') sessionEndPopupRef: TemplateRef<any>;

    constructor(
        private opfabEventStreamService : OpfabEventStreamService,
        private soundNotificationService: SoundNotificationService,
        private modalService: NgbModal,
        private authService: AuthService,
        private logger: OpfabLoggerService,
        private currentUserStore: CurrentUserStore
    ) {}

    ngOnInit(): void {
        this.subscribeToSessionEnd();
        this.subscribeToSessionClosedByNewUser();
    }

    private subscribeToSessionEnd() {
        this.currentUserStore.getSessionExpired().subscribe(() => {
            this.logger.info('Session expire ', LogOption.REMOTE);
            this.soundNotificationService.handleSessionEnd();
            this.opfabEventStreamService.closeEventStream();
            this.modalRef = this.modalService.open(this.sessionEndPopupRef, {
                centered: true,
                backdrop: 'static',
                backdropClass: 'opfab-session-end-modal',
                windowClass: 'opfab-session-end-modal'
            });
        });
    }

    private subscribeToSessionClosedByNewUser() {
        this.opfabEventStreamService.getReceivedDisconnectUser().subscribe((isDisconnected) => {
            this.isDisconnectedByNewUser = isDisconnected;

            if (isDisconnected) {
                this.soundNotificationService.clearOutstandingNotifications();
            }
        });
    }

    public logout() {
        this.logger.info('Logout ', LogOption.REMOTE);
        this.modalRef.close();
        this.authService.logout();
    }
}

/* Copyright (c) 2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {Injectable} from '@angular/core';
import {OpfabLoggerService} from '@ofServices/logs/opfab-logger.service';
import {TemplateCssService} from '@ofServices/template-css.service';
import {HandlebarsService} from 'app/modules/card/services/handlebars.service';
import {debounce, timer, map, catchError} from 'rxjs';
import {OpfabEventStreamService} from './opfabEventStream.service';
import {ProcessesService} from './processes.service';

@Injectable({
    providedIn: 'root'
})
export class ApplicationUpdateService {
 

    constructor(
        private opfabEventStreamService : OpfabEventStreamService,
        private processService: ProcessesService,
        private handlebarsService: HandlebarsService,
        private templateCssService: TemplateCssService,
        private logger : OpfabLoggerService
    ) {}


    init() {
        this.listenForBusinessConfigUpdate();
    }

    private listenForBusinessConfigUpdate() {
        this.opfabEventStreamService.getBusinessConfigChange().pipe(
                debounce(() => timer(5000 + Math.floor(Math.random() * 5000))), // use a random  part to avoid all UI to access at the same time the server
                map(() => {
                    this.logger.info("Update business config");
                    this.handlebarsService.clearCache();
                    this.templateCssService.clearCache();
                    this.processService.loadAllProcesses().subscribe();
                    this.processService.loadProcessGroups().subscribe();
                }),
                catchError((error, caught) => {
                    console.error('ProcessesEffects - Error in update business config ', error);
                    return caught;
                })
            ).subscribe();
    }

}
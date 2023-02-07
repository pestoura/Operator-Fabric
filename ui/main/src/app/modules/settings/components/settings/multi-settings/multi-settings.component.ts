/* Copyright (c) 2018-2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {Component, OnDestroy, OnInit} from '@angular/core';
import {BaseSettingDirective} from '../base-setting/base-setting.directive';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ConfigService} from 'app/business/services/config.service';
import {SettingsService} from 'app/business/services/settings.service';
import {OpfabLoggerService} from 'app/business/services/logs/opfab-logger.service';
import {CurrentUserStore} from 'app/business/store/current-user.store';

@Component({
    selector: 'of-multi-settings',
    templateUrl: './multi-settings.component.html'
})
export class MultiSettingsComponent extends BaseSettingDirective implements OnInit, OnDestroy {
    constructor(
        protected configService: ConfigService,
        protected settingsService: SettingsService,
        protected currentUserStore: CurrentUserStore,
        protected logger: OpfabLoggerService
    ) {
        super(configService, settingsService,currentUserStore,logger);
    }

    initFormGroup() {
        const validators = this.computeMultiValidators();
        return new FormGroup(
            {
                setting: new FormControl([], validators)
            },
            {updateOn: 'change'}
        );
    }

    protected computeMultiValidators() {
        const validators = [];
        if (this.requiredField) {
            validators.push(Validators.required);
        }
        return validators;
    }

    updateValue(value) {
        this.form.get('setting').setValue(value, {emitEvent: false});
    }
}

/* Copyright (c) 2021, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {TranslateModule} from '@ngx-translate/core';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {TimelineButtonsComponent} from './timeline-buttons.component';

@NgModule({
    declarations: [TimelineButtonsComponent],
    imports: [CommonModule, FormsModule, ReactiveFormsModule, TranslateModule, NgbModule],
    exports: [TimelineButtonsComponent]
})
export class TimelineButtonsModule {}

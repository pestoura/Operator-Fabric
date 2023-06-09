/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CardComponent} from './card.component';
import {CardBodyComponent} from './components/card-body/card-body.component';
import {TranslateModule} from '@ngx-translate/core';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {CountDownModule} from '../share/countdown/countdown.module';
import {UserCardModule} from '../usercard/usercard.module';
import {ReactiveFormsModule} from '@angular/forms';
import {MultiSelectModule} from '../share/multi-select/multi-select.module';
import {SpinnerModule} from '../share/spinner/spinner.module';
import {TemplateRenderingModule} from '../share/template-rendering/template-rendering.module';
import {PipesModule} from '../share/pipes/pipes.module';
import {CardActionsComponent} from './components/card-actions/card-actions.component';
import {CardAcksFooterComponent} from './components/card-acks-footer/card-acks-footer.component';
import {CardHeaderComponent} from './components/card-header/card-header.component';
import {CardFooterTextComponent} from './components/card-footer-text/card-footer-text.component';
import {CardAckComponent} from './components/card-ack/card-ack.component';
import {CardResponseComponent} from './components/card-reponse/card-response.component';

@NgModule({
    declarations: [
        CardComponent,
        CardBodyComponent,
        CardActionsComponent,
        CardAcksFooterComponent,
        CardHeaderComponent,
        CardFooterTextComponent,
        CardAckComponent,
        CardResponseComponent
    ],
    imports: [
        CommonModule,
        CountDownModule,
        TranslateModule,
        NgbModule,
        UserCardModule,
        ReactiveFormsModule,
        MultiSelectModule,
        SpinnerModule,
        TemplateRenderingModule,
        PipesModule
    ],
    exports: [CardComponent, CardBodyComponent]
})
export class CardModule {}

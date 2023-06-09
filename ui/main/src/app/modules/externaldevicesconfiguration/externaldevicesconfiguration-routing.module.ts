/* Copyright (c) 2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {ExternaldevicesconfigurationComponent} from './externaldevicesconfiguration.component';
import {DevicesTableComponent} from './table/devices.table.component';
import {UsersTableComponent} from './table/users.table.component';


const defaultPath = 'devices';


const routes: Routes = [
    {
        path: '',
        component: ExternaldevicesconfigurationComponent,

        children: [
            {
                path: 'devices',
                component: DevicesTableComponent
            },
            {
                path: 'users',
                component: UsersTableComponent
            },

            {path: '**', redirectTo: defaultPath}
        ]
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class ExternaldevicesconfigurationRoutingModule {}

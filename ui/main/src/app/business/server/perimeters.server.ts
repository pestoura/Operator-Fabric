/* Copyright (c) 2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {Perimeter} from '@ofModel/perimeter.model';
import {Observable} from 'rxjs';
import {ServerResponse} from './serverResponse';

export abstract class PerimetersServer {
    abstract deleteById(id: string): Observable<ServerResponse<any>>; 
    abstract queryAllPerimeters(): Observable<ServerResponse<Perimeter[]>>;
    abstract createPerimeter(perimeterData: Perimeter): Observable<ServerResponse<Perimeter>>;
    abstract updatePerimeter(perimeterData: Perimeter): Observable<ServerResponse<Perimeter>>;
}
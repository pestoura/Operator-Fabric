/* Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

import {Injectable, OnDestroy} from '@angular/core';
import {EntitiesService} from 'app/business/services/entities.service';
import {GroupsService} from 'app/business/services/groups.service';
import {UserService} from 'app/business/services/user.service';
import {CrudService} from 'app/business/services/crud-service';
import {CachedCrudService} from 'app/business/services/cached-crud-service';
import {Observable, ReplaySubject, Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {PerimetersService} from 'app/business/services/perimeters.service';
import {AdminProcessesService} from 'app/business/services/adminprocess.service';

/** The aim of this service is to provide the services that need to be shared between components of the admin screen. For example, a single
 * instance of `EntitiesService` should be used across all components so a update to the cache is visible from all components.
 * This is normally doable by defining the providers for Angular's DI in the correct place, but then I couldn't get Angular to provide the
 * appropriate subclass of `CrudService` depending on the context (I tried making it generic, to no avail).
 * */

@Injectable()
export class SharingService implements OnDestroy {
    private readonly _paginationPageSize$: ReplaySubject<number>;
    private unsubscribe$: Subject<void> = new Subject<void>();

    constructor(
        private entitiesService: EntitiesService,
        private groupsService: GroupsService,
        private userService: UserService,
        private perimetersService: PerimetersService,
        private adminprocessesService: AdminProcessesService
    ) {
        this._paginationPageSize$ = new ReplaySubject<number>();

        // Initialization necessary for perimeters selection dropdown in modals and to display names instead of codes
        // As it is only necessary for admin purposes, it's done here rather than in the app initialization code
        this.perimetersService.loadAllPerimetersData().pipe(takeUntil(this.unsubscribe$)).subscribe();
    }

    /** This is a factory method returning the appropriate `CrudService` depending on the type passed as parameter.
     * */
    public resolveCrudServiceDependingOnType(adminItemType: AdminItemType): CrudService {
        switch (adminItemType) {
            case AdminItemType.ENTITY:
                return this.entitiesService;
            case AdminItemType.GROUP:
                return this.groupsService;
            case AdminItemType.USER:
                return this.userService;
            case AdminItemType.PERIMETER:
                return this.perimetersService;
            case AdminItemType.PROCESS:
                return this.adminprocessesService;
            default:
                throw Error('No CrudService associated with ' + adminItemType);
        }
    }

    /** This is a factory method returning the appropriate `CachedCrudService` depending on the type passed as parameter.
     * */
    public resolveCachedCrudServiceDependingOnType(adminItemType: AdminItemType): CachedCrudService {
        switch (adminItemType) {
            case AdminItemType.ENTITY:
                return this.entitiesService;
            case AdminItemType.GROUP:
                return this.groupsService;
            case AdminItemType.PERIMETER:
                return this.perimetersService;
            case AdminItemType.PROCESS:
                return this.adminprocessesService;
            default:
                throw Error('No CachedCrudService associated with ' + adminItemType);
        }
    }

    get paginationPageSize$(): Observable<number> {
        return this._paginationPageSize$;
    }

    changePaginationPageSize(value: number) {
        this._paginationPageSize$.next(value);
    }

    ngOnDestroy() {
        this.unsubscribe$.next();
        this.unsubscribe$.complete();
    }
}

/** This enum defines the type of the data managed by an admin table or renderer. When adding a type to this list, please make sure
 * to also add the corresponding handling to the methods above.
 * */
export enum AdminItemType {
    USER = 'user',
    ENTITY = 'entity',
    GROUP = 'group',
    PERIMETER = 'perimeter',
    PROCESS = 'process'
}

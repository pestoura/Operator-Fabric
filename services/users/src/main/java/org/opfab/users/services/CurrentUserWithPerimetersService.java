/* Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */
package org.opfab.users.services;

import org.opfab.users.model.*;
import org.opfab.users.repositories.EntityRepository;
import org.opfab.users.model.CurrentUserWithPerimetersData;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CurrentUserWithPerimetersService {


    private UsersService usersService;
    private UserSettingsService userSettingsService;
    private EntityRepository entityRepository;

    public CurrentUserWithPerimetersService(UsersService usersService, UserSettingsService userSettingsService,
             EntityRepository entityRepository) {
        this.usersService = usersService;
        this.userSettingsService = userSettingsService;
        this.entityRepository = entityRepository;
    }

    public CurrentUserWithPerimeters fetchCurrentUserWithPerimeters(User user) {
        CurrentUserWithPerimetersData currentUserWithPerimetersData = new CurrentUserWithPerimetersData();
        if (user != null) {
            currentUserWithPerimetersData.setUserData(user);
            handleGroups(user, currentUserWithPerimetersData);
            handleEntities(user);
        }
        return currentUserWithPerimetersData;
    }

    protected void handleGroups(User userData, CurrentUserWithPerimetersData userWithPerimeterData) {
        List<String> userGroups = userData.getGroups(); // First, we recover the groups to which the user belongs

        // We recover the user_settings to have the process/state filters defined by the
        // user, for his feed
        OperationResult<UserSettings> operationResult = userSettingsService.fetchUserSettings(userData.getLogin());
        if (operationResult.isSuccess())
            userWithPerimeterData
                    .setProcessesStatesNotNotified(operationResult.getResult().getProcessesStatesNotNotified());

        if ((userGroups != null) && (!userGroups.isEmpty())) { // Then, we recover the groups data
            List<Group> groups = usersService.retrieveGroups(userGroups);

            if ((groups != null) && (!groups.isEmpty())) {
                Set<Perimeter> perimetersData = new HashSet<>(); // We use a set because we don't want to have a
                                                                 // duplicate
                groups.forEach( // For each group, we recover its perimeters
                        groupData -> {
                            List<Perimeter> list = usersService.retrievePerimeters(groupData.getPerimeters());
                            if (list != null)
                                perimetersData.addAll(list);
                        });
                userWithPerimeterData.computePerimeters(perimetersData);
            }
        }
    }

    /*
     * by convention there can not be cycles within Entity relationship
     * this function adds transitive entity references to the declared user entity
     * list
     */
    protected void handleEntities(User userData) {
        List<String> userEntityList = userData.getEntities();

        // we retrieve entitiesDisconnected of the user
        OperationResult<UserSettings> operationResult = userSettingsService.fetchUserSettings(userData.getLogin());
        if (operationResult.isSuccess()) {
            List<String> entitiesDisconnected = operationResult.getResult().getEntitiesDisconnected();
            // we remove entitiesDisconnected from the entities list of the user
            if (entitiesDisconnected != null) {
                userEntityList = userEntityList.stream().filter(
                        entityId -> !entitiesDisconnected.contains(entityId)).toList();
            }
        }

        Set<String> userEntityNames = userEntityList.stream().collect(Collectors.toSet());
        List<Entity> systemEntities = entityRepository.findAll();
        Map<String, Entity> systemEntityDictionary = systemEntities.stream()
                .collect(Collectors.toMap(Entity::getId, Function.identity()));
        userEntityList.stream()
                .forEach(entityName -> this.manageParentsRef(entityName, systemEntityDictionary, userEntityNames));
        userData.setEntities(userEntityNames.stream().toList());
    }

    // recursive because by convention there are no cycle in entity relationship (cf
    // above)
    protected void manageParentsRef(String entity, Map<String, Entity> dictionary, Set<String> records) {
        Entity entityRef = dictionary.get(entity);
        if (entityRef != null) {
            entityRef.getParents().stream().forEach(parentName -> {
                if (!records.contains(parentName)) {
                    this.manageParentsRef(parentName, dictionary, records);
                    records.add(parentName);
                }
            });
        }
    }
}
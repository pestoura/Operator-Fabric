/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

package org.opfab.users.mongo.repositories;

import java.util.List;

import org.opfab.users.model.EntityData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Mongo {@link EntityData} repository
 */
@Repository
public interface MongoEntityRepository extends MongoRepository<EntityData, String> {

    Page<EntityData> findAll(Pageable pageable);

    List<EntityData> findByParentsContaining(String entityId);

}

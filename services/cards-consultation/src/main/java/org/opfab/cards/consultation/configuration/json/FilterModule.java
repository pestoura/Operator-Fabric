/* Copyright (c) 2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */
package org.opfab.cards.consultation.configuration.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.opfab.cards.consultation.model.ArchivedCardsFilter;
import org.opfab.cards.consultation.model.ArchivedCardsFilterData;
import org.opfab.cards.consultation.model.FilterModel;
import org.opfab.cards.consultation.model.FilterModelData;

public class FilterModule extends SimpleModule {

    public FilterModule() {
        addAbstractTypeMapping(ArchivedCardsFilter.class, ArchivedCardsFilterData.class);
        addAbstractTypeMapping(FilterModel.class, FilterModelData.class);
    }
}
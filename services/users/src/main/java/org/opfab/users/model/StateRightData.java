/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */



package org.opfab.users.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StateRight Model, documented at {@link StateRight}
 *
 * {@inheritDoc}
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StateRightData implements StateRight {
    private String state;
    private RightsEnum right;
    @Builder.Default
    private Boolean filteringNotificationAllowed = true;

    public StateRightData(StateRight stateRight) {
        this.state = stateRight.getState();
        this.right = stateRight.getRight();
        this.filteringNotificationAllowed = stateRight.getFilteringNotificationAllowed();
    }
}

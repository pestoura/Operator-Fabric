/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

package org.opfab.cards.consultation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opfab.cards.model.CardOperationTypeEnum;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardOperationConsultationData implements CardOperation {

    private CardOperationTypeEnum type;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String cardId;
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private LightCard card;
}

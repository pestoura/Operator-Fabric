/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.lfenergy.operatorfabric.cards.consultation.model;

import lombok.*;
import org.lfenergy.operatorfabric.cards.model.TitlePositionEnum;

import java.util.List;

/**
 * Please use builder to instantiate outside delinearization
 *
 * @Author David Binder
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetailConsultationData implements Detail {
    private TitlePositionEnum titlePosition;
    private I18n title;
    private String titleStyle;

    private String templateName;
    @Singular
    private List<String> styles;
}

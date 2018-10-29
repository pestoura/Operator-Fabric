/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.lfenergy.operatorfabric.cards.consultation.model;

import lombok.*;

import java.util.Map;

/**
 * Please use builder to instantiate outside delinearization
 *
 * @Author David Binder
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class I18nConsultationData implements I18n {
    private String key;
    @Singular
    private Map<String,String> parameters;
}

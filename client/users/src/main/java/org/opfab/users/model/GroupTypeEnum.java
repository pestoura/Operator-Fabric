/* Copyright (c) 2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */


package org.opfab.users.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Group type 
 *
 * <dl>
 *     <dt>ROLE</dt><dd> Used to define user role or profile </dd>
 *     <dt>PERMISSION</dt><dd> Used to define user permissions on processes </dd>
 * </dl>
 * Note : This enum is created by hand because Swagger can't handle enums. It should match the corresponding enum definition in the Users API.
 *
 */
public enum GroupTypeEnum {
    ROLE("ROLE"),
    PERMISSION("PERMISSION");

    private String value;

    GroupTypeEnum(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static GroupTypeEnum fromValue(String text) {
        for (GroupTypeEnum b : GroupTypeEnum.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}
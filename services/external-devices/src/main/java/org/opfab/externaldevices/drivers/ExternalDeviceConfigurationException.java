/* Copyright (c) 2021, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

package org.opfab.externaldevices.drivers;

/** This exception is thrown whenever there is an issue with the resolution of the configuration for a given incoming
 * signal: userConfiguration/deviceConfiguration/signalMapping not found, or signal key that is not supported in the
 * signalMapping.
 * */
public class ExternalDeviceConfigurationException extends Exception {

    public ExternalDeviceConfigurationException(String message) {
        super(message);
    }
}

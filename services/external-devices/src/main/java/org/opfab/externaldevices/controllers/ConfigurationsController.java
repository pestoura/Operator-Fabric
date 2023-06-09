/* Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

package org.opfab.externaldevices.controllers;


import lombok.extern.slf4j.Slf4j;
import org.opfab.externaldevices.drivers.ExternalDeviceConfigurationException;
import org.opfab.externaldevices.drivers.ExternalDeviceDriverException;
import org.opfab.externaldevices.drivers.UnknownExternalDeviceException;
import org.opfab.externaldevices.model.DeviceConfiguration;
import org.opfab.externaldevices.model.SignalMapping;
import org.opfab.externaldevices.model.UserConfiguration;
import org.opfab.externaldevices.services.ConfigService;
import org.opfab.externaldevices.services.DevicesService;
import org.opfab.springtools.configuration.oauth.OpFabJwtAuthenticationToken;
import org.opfab.springtools.error.model.ApiError;
import org.opfab.springtools.error.model.ApiErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

/**
 * DevicesController, documented at {@link ConfigurationsApi}
 *
 */
@RestController
@Slf4j
public class ConfigurationsController implements ConfigurationsApi {

    public static final String CREATED_LOG = "{} {} was created.";
    public static final String DELETED_LOG = "{} {} was deleted.";
    public static final String NOT_FOUND_MESSAGE = "%1$s %2$s was not found.";
    public static final String LOCATION_HEADER_NAME = "Location";
    public static final String DEVICE_CONFIGURATION_NAME = "DeviceConfiguration";
    public static final String SIGNAL_MAPPING_NAME = "SignalMapping";
    public static final String USER_CONFIGURATION_NAME = "UserConfiguration";

    private final ConfigService configService;
    private final DevicesService devicesService;

    @Autowired
    public ConfigurationsController(ConfigService configService,DevicesService devicesService) {
        this.configService = configService;
        this.devicesService = devicesService;
    }

    @Override
    public Void createDeviceConfiguration(HttpServletRequest request, HttpServletResponse response, DeviceConfiguration deviceConfiguration) {

        String id = deviceConfiguration.getId();
        configService.insertDeviceConfiguration(deviceConfiguration);
        if (Boolean.TRUE.equals(deviceConfiguration.getIsEnabled())) {
            try {
                devicesService.enableDevice(id);
            } catch (Exception exc) {
                log.warn("Impossible to enable driver {} ", id, exc);
            }
        }
        response.addHeader(LOCATION_HEADER_NAME, request.getContextPath() + "/configurations/devices/" + id);
        response.setStatus(201);
        log.info(CREATED_LOG, DEVICE_CONFIGURATION_NAME, id);
        return null;

    }

    @Override
    public List<DeviceConfiguration> getDeviceConfigurations(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(200);
        return this.configService.getDeviceConfigurations();
    }

    @Override
    public DeviceConfiguration getDeviceConfiguration(HttpServletRequest request, HttpServletResponse response, String deviceId) {
        try {
            DeviceConfiguration deviceConfiguration = this.configService.retrieveDeviceConfiguration(deviceId);
            response.setStatus(200);
            return deviceConfiguration;
        } catch (ExternalDeviceConfigurationException | UnknownExternalDeviceException e) {
            throw buildApiNotFoundException(e, String.format(NOT_FOUND_MESSAGE, DEVICE_CONFIGURATION_NAME, deviceId));
        }
    }

    private ApiErrorException buildApiNotFoundException(Exception e, String errorMessage)  {
        return new ApiErrorException(ApiError.builder()
                   .status(HttpStatus.NOT_FOUND)
                   .message(errorMessage)
                   .build(), e);
    }

    @Override
    public Void deleteDeviceConfiguration(HttpServletRequest request, HttpServletResponse response, String deviceId) throws ExternalDeviceDriverException{

        try {
            devicesService.disableDevice(deviceId);
            configService.deleteDeviceConfiguration(deviceId);
        } catch (ExternalDeviceConfigurationException | UnknownExternalDeviceException e) {
            throw buildApiNotFoundException(e, String.format(NOT_FOUND_MESSAGE, DEVICE_CONFIGURATION_NAME, deviceId));
        }
        response.setStatus(200);
        log.info(DELETED_LOG, DEVICE_CONFIGURATION_NAME, deviceId);
        return null;

    }

    @Override
    public Void createSignalMapping(HttpServletRequest request, HttpServletResponse response, SignalMapping signalMapping) {

        String id = signalMapping.getId();
        configService.insertSignalMapping(signalMapping);
        response.addHeader(LOCATION_HEADER_NAME, request.getContextPath() + "/configurations/signals/" + id);
        response.setStatus(201);
        log.info(CREATED_LOG, SIGNAL_MAPPING_NAME, id);
        return null;
    }

    @Override
    public List<SignalMapping> getSignalMappings(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(200);
        return this.configService.getSignalMappings();
    }

    @Override
    public SignalMapping getSignalMapping(HttpServletRequest request, HttpServletResponse response, String signalMappingId) {
        try {
            SignalMapping signalMapping = this.configService.retrieveSignalMapping(signalMappingId);
            response.setStatus(200);
            return signalMapping;
        } catch (ExternalDeviceConfigurationException e) {
            throw buildApiNotFoundException(e, String.format(NOT_FOUND_MESSAGE, SIGNAL_MAPPING_NAME, signalMappingId));
        }
    }

    @Override
    public Void deleteSignalMapping(HttpServletRequest request, HttpServletResponse response, String signalMappingId) {

        try {
            configService.deleteSignalMapping(signalMappingId);
        } catch (ExternalDeviceConfigurationException e) {
            throw buildApiNotFoundException(e, String.format(NOT_FOUND_MESSAGE, SIGNAL_MAPPING_NAME, signalMappingId));
        }
        response.setStatus(200);
        log.debug(DELETED_LOG, SIGNAL_MAPPING_NAME, signalMappingId);
        return null;
    }

    @Override
    public Void createUserConfiguration(HttpServletRequest request, HttpServletResponse response, UserConfiguration userConfiguration) {
        String id = userConfiguration.getUserLogin();
        configService.saveUserConfiguration(userConfiguration);
        response.addHeader(LOCATION_HEADER_NAME, request.getContextPath() + "/configurations/signals/" + id);
        response.setStatus(201);
        log.info(CREATED_LOG, SIGNAL_MAPPING_NAME, id);
        return null;
    }

    @Override
    public List<UserConfiguration> getUserConfigurations(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(200);
        return this.configService.getUserConfigurations();
    }

    @Override
    public UserConfiguration getUserConfiguration(HttpServletRequest request, HttpServletResponse response, String userLogin) {
        try {
            UserConfiguration userConfiguration = this.configService.retrieveUserConfiguration(userLogin);
            response.setStatus(200);
            return userConfiguration;
        } catch (ExternalDeviceConfigurationException e) {
            throw buildApiNotFoundException(e, String.format(NOT_FOUND_MESSAGE, USER_CONFIGURATION_NAME, userLogin));
        }
    }

    @Override
    public Void deleteUserConfiguration(HttpServletRequest request, HttpServletResponse response, String userLogin) {
        try {
            OpFabJwtAuthenticationToken jwtPrincipal = (OpFabJwtAuthenticationToken) request.getUserPrincipal();
            Jwt token = null;
            if (jwtPrincipal!=null) {
                token = jwtPrincipal.getToken();
            }

            configService.deleteUserConfiguration(userLogin, Optional.ofNullable(token));
        } catch (ExternalDeviceConfigurationException e) {
            throw buildApiNotFoundException(e, String.format(NOT_FOUND_MESSAGE, USER_CONFIGURATION_NAME, userLogin));
        }
        response.setStatus(200);
        log.info(DELETED_LOG, USER_CONFIGURATION_NAME, userLogin);
        return null;
    }

}

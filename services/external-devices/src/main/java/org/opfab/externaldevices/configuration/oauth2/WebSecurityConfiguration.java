/* Copyright (c) 2021, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

package org.opfab.externaldevices.configuration.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.opfab.springtools.configuration.oauth.WebSecurityChecks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * OAuth 2 http authentication configuration and access rules
 *
 *
 */
@Configuration
@Slf4j
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    public static final String PROMETHEUS_PATH ="/actuator/prometheus**";
    public static final String LOGGERS_PATH ="/actuator/loggers/**";
    public static final String CONFIGURATIONS_ROOT_PATH = "/configurations/";
    public static final String DEVICES_ROOT_PATH = "/devices/";
    public static final String NOTIFICATIONS_ROOT_PATH = "/notifications";

    public static final String ADMIN_ROLE = "ADMIN";

    public static final String AUTH_AND_IP_ALLOWED = "isAuthenticated() and @webSecurityChecks.checkUserIpAddress(authentication)";
    public static final String ADMIN_AND_IP_ALLOWED = "hasRole('ADMIN') and @webSecurityChecks.checkUserIpAddress(authentication)";

    @Autowired
    WebSecurityChecks webSecurityChecks;
    
    @Autowired
    private Converter<Jwt, AbstractAuthenticationToken> opfabJwtConverter;

    
    @Override
    public void configure(final HttpSecurity http) throws Exception {
        configureCommon(http);
        http
                .oauth2ResourceServer()
                .jwt().jwtAuthenticationConverter(opfabJwtConverter);
    }

    public static void configureCommon(final HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers(HttpMethod.GET,PROMETHEUS_PATH).permitAll()
                .antMatchers(LOGGERS_PATH).hasRole(ADMIN_ROLE)
                .antMatchers(HttpMethod.POST,NOTIFICATIONS_ROOT_PATH).access(AUTH_AND_IP_ALLOWED)
                .antMatchers(CONFIGURATIONS_ROOT_PATH+"**").access(ADMIN_AND_IP_ALLOWED)
                .antMatchers(DEVICES_ROOT_PATH+"**").access(ADMIN_AND_IP_ALLOWED)
                .anyRequest().access(AUTH_AND_IP_ALLOWED);
                
    }

}
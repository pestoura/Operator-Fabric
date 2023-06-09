/* Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */



package org.opfab.cards.publication.configuration.oauth2;

import lombok.extern.slf4j.Slf4j;

import org.opfab.springtools.configuration.oauth.WebSecurityChecks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth 2 http authentication configuration and access rules
 *
 *
 */
@Configuration
@Slf4j
@Profile(value = {"!test"})
public class WebSecurityConfiguration {

    public static final String PROMETHEUS_PATH ="/actuator/prometheus**";
    public static final String LOGGERS_PATH ="/actuator/loggers/**";

    public static final String ADMIN_ROLE = "ADMIN";

    public static final String AUTH_AND_IP_ALLOWED = "isAuthenticated() and @webSecurityChecks.checkUserIpAddress(authentication)";
    public static final String ADMIN_AND_IP_ALLOWED = "hasRole('ADMIN') and @webSecurityChecks.checkUserIpAddress(authentication)";

    @Autowired
    WebSecurityChecks webSecurityChecks;

    @Value("${checkAuthenticationForCardSending:true}")
    private boolean checkAuthenticationForCardSending;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           Converter<Jwt, AbstractAuthenticationToken> opfabJwtConverter) throws Exception {
        configureCommon(http, checkAuthenticationForCardSending);
        http.csrf().disable();
        http
                .oauth2ResourceServer()
                .jwt().jwtAuthenticationConverter(opfabJwtConverter);

        return http.build();
    }

    public static void configureCommon(final HttpSecurity http, boolean checkAuthenticationForCardSending) throws Exception {
        if (checkAuthenticationForCardSending) {
            http
            .authorizeRequests()
            .requestMatchers(HttpMethod.GET,PROMETHEUS_PATH).permitAll()
            .requestMatchers(LOGGERS_PATH).hasRole(ADMIN_ROLE)
            .requestMatchers("/cards/userCard/**").access(AUTH_AND_IP_ALLOWED)
            .requestMatchers("/cards/translateCardField").access(AUTH_AND_IP_ALLOWED)
            .requestMatchers(HttpMethod.DELETE, "/cards").access(ADMIN_AND_IP_ALLOWED)
            .requestMatchers("/**").access(AUTH_AND_IP_ALLOWED);
        } else {
            http
            .authorizeRequests()
            .requestMatchers(LOGGERS_PATH).hasRole(ADMIN_ROLE)
            .requestMatchers("/cards/userCard/**").access(AUTH_AND_IP_ALLOWED)
            .requestMatchers("/cards/translateCardField").access(AUTH_AND_IP_ALLOWED)
            .requestMatchers("/**").permitAll();
        }
    }

}

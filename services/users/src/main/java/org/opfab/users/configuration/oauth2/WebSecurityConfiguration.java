/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */



package org.opfab.users.configuration.oauth2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth 2 http authentication configuration and access rules
 *
 *
 */
@Configuration
@Slf4j
public class WebSecurityConfiguration {

    public static final String PROMETHEUS_PATH ="/actuator/prometheus**";
    public static final String LOGGERS_PATH ="/actuator/loggers/**";

    public static final String USER_PATH = "/users/{login}";
    public static final String USERS_SETTINGS_PATH = "/users/{login}/settings";
    public static final String USERS_PERIMETERS_PATH = "/users/{login}/perimeters";
    public static final String USER_TOKEN_SYNCHRONIZATION_PATH = "/users/synchronizeWithToken";
    public static final String USERS_PATH = "/users/**";
    public static final String USERS = "/users";
    public static final String GROUPS_PATH = "/groups/**";
    public static final String GROUPS = "/groups";
    public static final String ENTITIES_PATH = "/entities/**";
    public static final String ENTITIES = "/entities";
    public static final String PERIMETERS_PATH = "/perimeters/**";
    public static final String USER_ACTION_LOGS = "/userActionLogs";
    public static final String CURRENTUSER_INTERNAL_PATH = "/internal/CurrentUserWithPerimeters";
    public static final String ADMIN_ROLE = "ADMIN";
    public static final String IS_ADMIN_OR_OWNER = "hasRole('ADMIN') or @webSecurityChecks.checkUserLogin(authentication,#login)";
    public static final String IS_ADMIN_OR_OWNER_AND_IP_ALLOWED = "( ( hasRole('ADMIN') or @webSecurityChecks.checkUserLogin(authentication,#login) ) and @webSecurityChecks.checkUserIpAddress(authentication) )";
    public static final String IS_ADMIN_AND_NOT_OWNER_AND_IP_ALLOWED = "hasRole('ADMIN') and ! @webSecurityChecks.checkUserLogin(authentication,#login) and @webSecurityChecks.checkUserIpAddress(authentication)";
    public static final String IS_ADMIN_AND_IP_ALLOWED = "hasRole('ADMIN') and @webSecurityChecks.checkUserIpAddress(authentication)";
    public static final String AUTH_AND_IP_ALLOWED = "isAuthenticated() and @webSecurityChecks.checkUserIpAddress(authentication)";

    @Autowired
    WebSecurityChecks webSecurityChecks;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           Converter<Jwt, AbstractAuthenticationToken> opfabJwtConverter) throws Exception {
        configureCommon(http);
        http
                .oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(opfabJwtConverter);

        return http.build();
    }

    /**This method handles the configuration to be shared with the test WebSecurityConfiguration class (access rules to be tested)
     * */
    public static void configureCommon(final HttpSecurity http) throws Exception {
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .and()
                .authorizeRequests()
                .requestMatchers(HttpMethod.GET,PROMETHEUS_PATH).permitAll()
                .requestMatchers(HttpMethod.POST, USER_TOKEN_SYNCHRONIZATION_PATH).access(AUTH_AND_IP_ALLOWED)
                .requestMatchers(HttpMethod.GET, USER_PATH).access(IS_ADMIN_OR_OWNER_AND_IP_ALLOWED)
                .requestMatchers(HttpMethod.PUT, USER_PATH).access(IS_ADMIN_OR_OWNER_AND_IP_ALLOWED)
                .requestMatchers(HttpMethod.DELETE, USER_PATH).access(IS_ADMIN_AND_NOT_OWNER_AND_IP_ALLOWED)
                .requestMatchers(HttpMethod.GET, USERS_SETTINGS_PATH).access(IS_ADMIN_OR_OWNER_AND_IP_ALLOWED)
                .requestMatchers(HttpMethod.PUT, USERS_SETTINGS_PATH).access(IS_ADMIN_OR_OWNER_AND_IP_ALLOWED)
                .requestMatchers(HttpMethod.PATCH, USERS_SETTINGS_PATH).access(IS_ADMIN_OR_OWNER_AND_IP_ALLOWED)
                .requestMatchers(HttpMethod.GET, USERS_PERIMETERS_PATH).access(IS_ADMIN_OR_OWNER_AND_IP_ALLOWED)
                .requestMatchers(HttpMethod.GET, USERS).access(AUTH_AND_IP_ALLOWED)
                .requestMatchers(USERS_PATH).access(IS_ADMIN_AND_IP_ALLOWED)
                .requestMatchers(HttpMethod.GET, GROUPS).access(AUTH_AND_IP_ALLOWED)
                .requestMatchers(GROUPS_PATH).access(IS_ADMIN_AND_IP_ALLOWED)
                .requestMatchers(HttpMethod.GET, ENTITIES).access(AUTH_AND_IP_ALLOWED)      // OC-1067 : we authorize all users for GET /entities
                .requestMatchers(ENTITIES_PATH).access(IS_ADMIN_AND_IP_ALLOWED)
                .requestMatchers(PERIMETERS_PATH).access(IS_ADMIN_AND_IP_ALLOWED)
                .requestMatchers(USER_ACTION_LOGS).access(IS_ADMIN_AND_IP_ALLOWED)
                .requestMatchers(CURRENTUSER_INTERNAL_PATH).authenticated()
                .requestMatchers(LOGGERS_PATH).hasRole(ADMIN_ROLE)
                .anyRequest().access(AUTH_AND_IP_ALLOWED);
    }

}

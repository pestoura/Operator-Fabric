/* Copyright (c) 2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

package org.opfab.externalapp.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth 2 http authentication configuration and access rules
 *
 *
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {


    @Value("${authenticationRequired:true}")
    private boolean authenticationRequired;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        configureCommon(http, authenticationRequired);
        http
                .oauth2ResourceServer()
                .jwt();

        return http.build();
    }

    public static void configureCommon(final HttpSecurity http, boolean authenticationRequired) throws Exception {
        http.csrf().disable();
        if (authenticationRequired) {
            http
                    .authorizeHttpRequests()
                    .requestMatchers("/test").authenticated()
                    .requestMatchers("/test/**").authenticated()
                    .requestMatchers("/**").permitAll();
        }
    }

}

/* Copyright (c) 2018, RTE (http://www.rte-france.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.lfenergy.operatorfabric.cards.consultation.config.webflux;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;


/**
 * Configures web scurity
 *
 * @author David Binder
 */
@Configuration
@Slf4j
@EnableWebFluxSecurity
public class WebSecurityConfig {

    /**
     * Secures access (all uris are secured)
     * @param http
     * @param opfabJwtReactiveAuthenticationManager
     * @return
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,ReactiveAuthenticationManager opfabJwtReactiveAuthenticationManager) {
        http.headers().frameOptions().disable();
        http
                .authorizeExchange()
                .anyExchange().authenticated().and()
                .oauth2ResourceServer().jwt().authenticationManager(opfabJwtReactiveAuthenticationManager);
                return http.build();
    }


}

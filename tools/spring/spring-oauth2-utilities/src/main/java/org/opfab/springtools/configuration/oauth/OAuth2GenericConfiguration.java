/* Copyright (c) 2018-2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */


package org.opfab.springtools.configuration.oauth;


import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.opfab.springtools.configuration.oauth.jwt.JwtProperties;
import org.opfab.springtools.configuration.oauth.jwt.groups.GroupsMode;
import org.opfab.springtools.configuration.oauth.jwt.groups.GroupsProperties;
import org.opfab.springtools.configuration.oauth.jwt.groups.GroupsUtils;
import org.opfab.users.model.CurrentUserWithPerimeters;
import org.opfab.users.model.User;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Common configuration (MVC and Webflux)
 *
 *
 */
@Configuration
@EnableFeignClients
@EnableCaching
@Import({UserServiceCache.class
        ,I18nProcessesCache.class
        ,ProcessesCache.class
        ,UpdateUserListenerConfiguration.class
        , GroupsProperties.class
        , GroupsUtils.class
        , JwtProperties.class})
@Slf4j
public class OAuth2GenericConfiguration {


    protected UserServiceCache userServiceCache;
	protected GroupsProperties groupsProperties;
	protected JwtProperties jwtProperties;
	protected GroupsUtils groupsUtils;


    public OAuth2GenericConfiguration(UserServiceCache userServiceCache,GroupsProperties groupsProperties,JwtProperties jwtProperties, GroupsUtils groupsUtils) {
        this.userServiceCache = userServiceCache;
        this.groupsProperties = groupsProperties;
        this.jwtProperties = jwtProperties;
        this.groupsUtils = groupsUtils;       
    }
    /**
     * Generates a converter that converts {@link Jwt} to {@link OpFabJwtAuthenticationToken} whose principal is  a
     * {@link User} model object
     *
     * @return Converter from {@link Jwt} to {@link OpFabJwtAuthenticationToken}
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> opfabJwtConverter() {

        return new Converter<Jwt, AbstractAuthenticationToken>(){
            @Override
            public AbstractAuthenticationToken convert(Jwt jwt) throws FeignException {
                return generateOpFabJwtAuthenticationToken(jwt);
            }
        };
    }


    @Bean
    public WebSecurityChecks webSecurityChecks() {
        return new WebSecurityChecks();
    }


    public AbstractAuthenticationToken generateOpFabJwtAuthenticationToken(Jwt jwt) {
        
        String principalId = jwt.getClaimAsString(jwtProperties.getLoginClaim()).toLowerCase();
        UserServiceCache.setTokenForUserRequest(principalId,jwt.getTokenValue());
        ProcessesCache.setTokenForUserRequest(principalId,jwt.getTokenValue());
        CurrentUserWithPerimeters currentUserWithPerimeters = userServiceCache.fetchCurrentUserWithPerimetersFromCacheOrProxy(principalId);
        User user = currentUserWithPerimeters.getUserData();

        // override the groups list from JWT mode, otherwise, default mode is OPERATOR_FABRIC
		if (groupsProperties.getMode() == GroupsMode.JWT) user.setGroups(getGroupsList(jwt));
        
		List<GrantedAuthority> authorities = OAuth2JwtProcessingUtilities.computeAuthorities(currentUserWithPerimeters.getPermissions());
		
		log.debug("user [{}] has these roles '{}' through the {} mode and entities {}",principalId,authorities,groupsProperties.getMode(),user.getEntities());
        
        return new OpFabJwtAuthenticationToken(jwt, currentUserWithPerimeters, authorities);
    }

	/**
	 * Creates group list from a jwt
	 * 
	 * @param jwt user's token
	 * @return a group list
	 */
	public List<String> getGroupsList(Jwt jwt) {
		return groupsUtils.createGroupsList(jwt);

    }

}

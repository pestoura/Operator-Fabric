/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */



package org.opfab.springtools.configuration.oauth;

import org.opfab.users.model.CurrentUserWithPerimeters;
import org.opfab.users.model.UserSettings;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


/**
 * Feign proxy for User service
 */
@FeignClient(url="${operatorfabric.servicesUrls.users}", name = "users", configuration=FeignConfiguration.class)
public interface UserServiceProxy {

    @GetMapping(value = "/internal/CurrentUserWithPerimeters",
            produces = { "application/json" })
    CurrentUserWithPerimeters fetchCurrentUserWithPerimeters(@RequestHeader("Authorization") String token);

    @PatchMapping(value = "/users/{login}/settings",
            consumes = { "application/json" },
            produces = { "application/json" })
    UserSettings patchUserSettings(@RequestHeader("Authorization") String token, @PathVariable(name = "login") String login, @RequestBody UserSettings settings) ;

}

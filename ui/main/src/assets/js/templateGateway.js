/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

const templateGateway = {
    opfabEntityNames: null,
    opfabEntities: null,
    childCards: [],
    userAllowedToRespond: false,
    userMemberOfAnEntityRequiredToRespond: false,
    entitiesAllowedToRespond: [],
    entityUsedForUserResponse: null,
    displayContext: '',
    isLocked: false,

    setEntityNames: function (entityNames) {
        this.opfabEntityNames = entityNames;
    },

    setEntities: function (entities) {
        this.opfabEntities = entities;
    },

    // UTILITIES FOR TEMPLATE

    getEntityName: function (entityId) {
        if (!this.opfabEntityNames) {
            console.log(new Date().toISOString(), ` Template.js : no entities information loaded`);
            return entityId;
        }
        if (!this.opfabEntityNames.has(entityId)) {
            console.log(new Date().toISOString(), ` Template.js : entityId ${entityId} is unknown`);
            return entityId;
        }
        return this.opfabEntityNames.get(entityId);
    },

    getEntity: function (entityId) {
        if (!this.opfabEntities) {
            console.log(new Date().toISOString(), ` Template.js : no entities information loaded`);
            return entityId;
        }
        if (!this.opfabEntities.has(entityId)) {
            console.log(new Date().toISOString(), ` Template.js : entityId ${entityId} is unknown`);
            return entityId;
        }
        return this.opfabEntities.get(entityId);
    },

    getAllEntities: function () {
        return Array.from(this.opfabEntities.values());
    },

    redirectToBusinessMenu: function (menuId, menuItemId, urlExtension) {
        const urlSplit = document.location.href.split('#');
        var newUrl = urlSplit[0] + '#/businessconfigparty/' + menuId + '/' + menuItemId;
        if (!!urlExtension) newUrl += urlExtension;
        document.location.href = newUrl;
    },

    // True if user is allowed to respond to the current card :
    //  - his entity is allowed to respond
    //  - he is member of a group having a perimeter permitting the response
    isUserAllowedToRespond: function () {
        return this.userAllowedToRespond;
    },

    // True if user is member of an entity required to respond to the current card
    isUserMemberOfAnEntityRequiredToRespond: function () {
        return this.userMemberOfAnEntityRequiredToRespond;
    },

    // Returns an array containing the ids of the entities allowed to respond
    getEntitiesAllowedToRespond() {
        return this.entitiesAllowedToRespond;
    },

    getEntityUsedForUserResponse() {
        return this.entityUsedForUserResponse;
    },

    getDisplayContext() {
        return this.displayContext;
    },

    //
    // FUNCTIONS TO OVERRIDE BY TEMPLATES
    //

    initTemplateGateway: function () {
        this.childCards = [];
        this.userAllowedToRespond = false;
        this.userMemberOfAnEntityRequiredToRespond = false;
        this.entitiesAllowedToRespond = [];
        this.entityUsedForUserResponse = null;
        this.displayContext = '';
        this.isLocked = false;

        // OpFab calls this function to inform the template that the card is locked
        this.lockAnswer = function () {
            // This function should be overridden in the template.
        };

        // OpFab calls this function to inform the template that the card is unlocked
        this.unlockAnswer = function () {
            // This function should be overridden in the template.
        };

        // OpFab calls this function to inform that the template has to apply child cards (called after template rendering and after change in child cards)
        this.applyChildCards = function () {
            // This function should be overridden in the template.
        };

        // OpFab calls this function when lttd expire with expired set to true
        this.setLttdExpired = function (expired) {
            // This function should be overridden in the template.
        };

        // OpFab calls this method to inform the template of the size of the screen dedicated to the card
        // size = 'md' for standard size
        // size = 'lg' for large size , i.e when the card is in full screen mode
        this.setScreenSize = function (size) {
            // This function should be overridden in the template.
        };

        // OpFab calls this method to get the form result when the user wants to send a response
        this.getUserResponse = function () {
            console.log(
                new Date().toISOString(),
                ` Template.js : no getUserResponse method defined in template , valid set to false`
            );
            return {valid: false, errorMsg: 'Impossible to respond due to a technical error in the template'};
        };

        // OpFab calls this method when it has finished all tasks regarding rendering template :
        // it is called after applyChildCard(), lockAnswer(), setLttdExpired() and setScreenSize()
        this.onTemplateRenderingComplete = function () {
            // This function may be overridden in the template.
        };

        // The template may call this method to display a spinner when the card is loaded but is in a time consuming process
        this.displayLoadingSpinner = function() {
            // This function is overridden in CardBodyComponent and in CardDetailComponent.
        };

        // The template may call this method to hide the spinner displayed after displayLoadingSpinner(), once the computations
        // of the template are done.
        this.hideLoadingSpinner = function() {
            // This function is overridden in CardBodyComponent and in CardDetailComponent.
        };

        // OpFab calls this function when global style has changed
        this.onStyleChange = function (expired) {
            // This function should be overridden in the template.
        };
    }
};

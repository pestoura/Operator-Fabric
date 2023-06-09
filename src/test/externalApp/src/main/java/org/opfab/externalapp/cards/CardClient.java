/* Copyright (c) 2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

package org.opfab.externalapp.cards;

import org.opfab.cards.model.Card;
import org.opfab.cards.model.CardCreationReport;
import org.opfab.externalapp.common.HttpClientInterceptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CardClient {

    private RestTemplateBuilder builder;

    public CardClient(RestTemplateBuilder builder) {
        this.builder = builder;
    }

    public CardCreationReport postCard(String url, String authToken, Card card) {
        RestTemplate restTemplate = builder.build();
        restTemplate.setInterceptors(List.of(new HttpClientInterceptor()));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization","Bearer " + authToken);
        HttpEntity<Card> request = new HttpEntity<>(card, headers);
        ResponseEntity<CardCreationReport> response = restTemplate.postForEntity(url, request, CardCreationReport.class);
        return response.getBody();
    }

    public Card getCard(String url, String authToken, String cardId) {
        RestTemplate restTemplate = builder.build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization","Bearer " + authToken);
        HttpEntity<String> request = new HttpEntity<>(headers);
        Map<String, String> params = new HashMap<>();
       
        ResponseEntity<CardData> response = restTemplate.exchange(url + "/" + cardId, HttpMethod.GET,
                request, CardData.class, params);
        CardData cardData = response.getBody();
        if (cardData!=null)  return cardData.card;
        else return null;
    }



}

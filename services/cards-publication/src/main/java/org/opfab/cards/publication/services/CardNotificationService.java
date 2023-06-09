/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */



package org.opfab.cards.publication.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.opfab.cards.model.CardOperationTypeEnum;
import org.opfab.cards.publication.model.CardOperationData;
import org.opfab.cards.publication.model.CardPublicationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <p>Aim of this service whose sole externally accessible method is
 * {@link #notifyOneCard(CardPublicationData, CardOperationTypeEnum)} is to
 * prepare data and notify AMQP exchange of it. Information about card
 * publication and deletion is then accessible to other services or
 * entities through bindings to these exchanges.
 * </p>
 * <p>One exchange is used, carsExchange 
 * See amqp.xml resource file ([project]/services/cards-publication/src/main/resources/amqp.xml)
 * for the exact configuration</p>
 */
@Service
@Slf4j
public class CardNotificationService {

    
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper;

   
    public CardNotificationService(RabbitTemplate rabbitTemplate,
                                   ObjectMapper mapper
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.mapper = mapper;
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    public void notifyOneCard(CardPublicationData card, CardOperationTypeEnum type) {
        CardOperationData.BuilderEncapsulator builderEncapsulator = CardOperationData.encapsulatedBuilder();
        builderEncapsulator.builder().type(type);
        switch (type) {
            case ADD:
            case UPDATE:
                builderEncapsulator.builder().card(card.toLightCard());
                break;
            case DELETE:
                builderEncapsulator.builder().cardId(card.getId());

        }
        CardOperationData cardOperation = builderEncapsulator.builder().build();
        List<String> listOfGroupRecipients = new ArrayList<>();
        if (card.getGroupRecipients() != null)
            card.getGroupRecipients().forEach(listOfGroupRecipients::add);
        cardOperation.setGroupRecipientsIds(listOfGroupRecipients);

        List<String> listOfEntityRecipients = new ArrayList<>();
        if (card.getEntityRecipients() != null)
            card.getEntityRecipients().forEach(listOfEntityRecipients::add);
        cardOperation.setEntityRecipientsIds(listOfEntityRecipients);

        List<String> listOfUserRecipients = new ArrayList<>();
        if (card.getUserRecipients() != null)
            card.getUserRecipients().forEach(listOfUserRecipients::add);
        cardOperation.setUserRecipientsIds(listOfUserRecipients);

        pushCardInRabbit(cardOperation);
    }

    private void pushCardInRabbit(CardOperationData cardOperation) {
        try {
            rabbitTemplate.convertAndSend("CARD_EXCHANGE", "", mapper.writeValueAsString(cardOperation));
            log.debug("Operation sent to CARD_EXCHANGE, type={}, ids={}, cards={}, groupRecipientsIds={}, entityRecipientsIds={}, userRecipientsIds={}"
                    , cardOperation.getType()
                    , cardOperation.getCardId()
                    , (cardOperation.getCard() != null ? cardOperation.getCard().toString() : "")
                    , cardOperation.getGroupRecipientsIds().toString()
                    , cardOperation.getEntityRecipientsIds().toString()
                    , cardOperation.getUserRecipientsIds().toString());
        } catch (JsonProcessingException e) {
            log.error("Unable to linearize card to json on amqp notification");
        }
    }

    public void pushAckOfCardInRabbit(String cardUid, String cardId, List<String> entitiesAcks) {
        CardOperationData.BuilderEncapsulator builderEncapsulator = CardOperationData.encapsulatedBuilder();
        builderEncapsulator.builder().type(CardOperationTypeEnum.ACK);
        builderEncapsulator.builder().cardUid(cardUid);
        builderEncapsulator.builder().cardId(cardId);
        builderEncapsulator.builder().entitiesAcks(entitiesAcks);
        CardOperationData cardOperation = builderEncapsulator.builder().build();

        try {
            rabbitTemplate.convertAndSend("ACK_EXCHANGE", "", mapper.writeValueAsString(cardOperation));
            log.debug("Acknowledgement for cardUid={} and cardId={} with entitiesAcks={} sent to ACK_EXCHANGE", cardUid, cardId, entitiesAcks);
        } catch (JsonProcessingException e) {
            log.error("Unable to linearize card operation for acknowledgement to json on amqp notification");
        }
    }
}

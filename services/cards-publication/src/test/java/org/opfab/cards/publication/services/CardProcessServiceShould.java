/* Copyright (c) 2018-2023, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */


package org.opfab.cards.publication.services;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opfab.cards.model.SeverityEnum;
import org.opfab.cards.publication.application.UnitTestApplication;
import org.opfab.cards.publication.configuration.TestCardReceiver;
import org.opfab.cards.publication.model.ArchivedCardPublicationData;
import org.opfab.cards.publication.model.CardPublicationData;
import org.opfab.cards.publication.model.HoursAndMinutes;
import org.opfab.cards.publication.model.HoursAndMinutesPublicationData;
import org.opfab.cards.publication.model.I18nPublicationData;
import org.opfab.cards.publication.model.PublisherTypeEnum;
import org.opfab.cards.publication.model.RecurrencePublicationData;
import org.opfab.cards.publication.model.TimeSpanPublicationData;
import org.opfab.cards.publication.repositories.ArchivedCardRepositoryForTest;
import org.opfab.cards.publication.repositories.CardRepositoryForTest;
import org.opfab.springtools.error.model.ApiErrorException;
import org.opfab.users.model.ComputedPerimeter;
import org.opfab.users.model.CurrentUserWithPerimeters;
import org.opfab.users.model.RightsEnum;
import org.opfab.users.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.ConstraintViolationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.nio.charset.Charset.forName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.named;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {UnitTestApplication.class})
@ActiveProfiles(profiles = {"native", "test"})
@Import({RestTemplate.class})
@Slf4j
class CardProcessServiceShould {

    private static final String API_TEST_EXTERNAL_RECIPIENT_1 = "api_test_externalRecipient1";

    @Autowired
    private CardProcessingService cardProcessingService;

    @Autowired
    private CardRepositoryForTest cardRepository;

    @Autowired
    private ArchivedCardRepositoryForTest archiveRepository;

    @Autowired
    private TestCardReceiver testCardReceiver;

    @Autowired
    private CardRepositoryService cardRepositoryService;


    @Autowired
    RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @AfterEach
    public void cleanAfter() {
        cardRepository.deleteAll();
        archiveRepository.deleteAll();
        testCardReceiver.clear();
    }

    @BeforeEach
    public void cleanBefore() {
        testCardReceiver.clear();
    }

        @BeforeEach
        public void init() {
                mockServer = MockRestServiceServer.createServer(restTemplate);
        }


        private User user;
        private CurrentUserWithPerimeters currentUserWithPerimeters;
        private Optional<Jwt> token;

    public CardProcessServiceShould() {

        user = new User();
        user.setLogin("dummyUser");
        user.setFirstName("Test");
        user.setLastName("User");
        List<String> groups = new ArrayList<>();
        groups.add("rte");
        groups.add("operator");
        user.setGroups(groups);
        List<String> entities = new ArrayList<>();
        entities.add("newPublisherId");
        entities.add("entity2");
        user.setEntities(entities);
        currentUserWithPerimeters = new CurrentUserWithPerimeters();
        currentUserWithPerimeters.setUserData(user);
        ComputedPerimeter c1 = new ComputedPerimeter();
        ComputedPerimeter c2 = new ComputedPerimeter();
        ComputedPerimeter c3 = new ComputedPerimeter();
        c1.setProcess("PROCESS_CARD_USER") ;
        c1.setState("STATE1");
        c1.setRights(RightsEnum.RECEIVEANDWRITE);
        c2.setProcess("PROCESS_CARD_USER") ;
        c2.setState("STATE2");
        c2.setRights(RightsEnum.RECEIVE);
        c3.setProcess("PROCESS_CARD_USER") ;
        c3.setState("STATE3");
        c3.setRights(RightsEnum.WRITE);
        List<ComputedPerimeter> list=new ArrayList<>();
        list.add(c1);
        list.add(c2);
        list.add(c3);
        currentUserWithPerimeters.setComputedPerimeters(list);

        token = Optional.empty();
    }

    private List<CardPublicationData> generateCards() {
        ArrayList<CardPublicationData> cards = new ArrayList<>();
        cards.add(
                CardPublicationData.builder().publisher("PUBLISHER_1").processVersion("0")
                        .processInstanceId("PROCESS_1").severity(SeverityEnum.ALARM)
                        .title(I18nPublicationData.builder().key("title").build())
                        .summary(I18nPublicationData.builder().key("summary").build())
                        .startDate(Instant.now())
                        .timeSpan(TimeSpanPublicationData.builder()
                                .start(Instant.ofEpochMilli(123l)).build())
                        .process("process1")
                        .state("state1")
                        .build()
                );
        cards.add(
                CardPublicationData.builder().publisher("PUBLISHER_2").processVersion("0")
                        .processInstanceId("PROCESS_1").severity(SeverityEnum.INFORMATION)
                        .title(I18nPublicationData.builder().key("title").build())
                        .summary(I18nPublicationData.builder().key("summary").build())
                        .startDate(Instant.now())
                        .process("process2")
                        .state("state2")
                        .build());
        cards.add(
                CardPublicationData.builder().publisher("PUBLISHER_2").processVersion("0")
                        .processInstanceId("PROCESS_2").severity(SeverityEnum.COMPLIANT)
                        .title(I18nPublicationData.builder().key("title").build())
                        .summary(I18nPublicationData.builder().key("summary").build())
                        .startDate(Instant.now())
                        .process("process3")
                        .state("state3")
                        .build());
        cards.add(
                CardPublicationData.builder().publisher("PUBLISHER_1").processVersion("0")
                        .processInstanceId("PROCESS_2").severity(SeverityEnum.INFORMATION)
                        .title(I18nPublicationData.builder().key("title").build())
                        .summary(I18nPublicationData.builder().key("summary").build())
                        .startDate(Instant.now())
                        .process("process4")
                        .state("state4")
                        .build());
        cards.add(                
                CardPublicationData.builder().publisher("PUBLISHER_1").processVersion("0")
                        .processInstanceId("PROCESS_1").severity(SeverityEnum.INFORMATION)
                        .title(I18nPublicationData.builder().key("title").build())
                        .summary(I18nPublicationData.builder().key("summary").build())
                        .startDate(Instant.now())
                        .process("process5")
                        .state("state5")
                        .build());
        return cards;
    }

    private CardPublicationData generateWrongCardData(String publisher, String process) {
        return CardPublicationData.builder().publisher(publisher).processVersion("0").processInstanceId(process)
                .build();
    }

    private CardPublicationData generateOneCard(String publisher) {
        return CardPublicationData.builder().publisher(publisher).processVersion("0")
        .processInstanceId("PROCESS_1").severity(SeverityEnum.ALARM)
        .title(I18nPublicationData.builder().key("title").build())
        .summary(I18nPublicationData.builder().key("summary").build())
        .startDate(Instant.now())
        .timeSpan(TimeSpanPublicationData.builder()
                .start(Instant.ofEpochMilli(123L)).build())
        .process("process1")
        .state("state1")
        .build();
    }

    @Test
    void createCards() {
        generateCards().forEach(card -> { cardProcessingService.processCard(card); });
        Assertions.assertThat(checkCardCount(5)).isTrue();
        Assertions.assertThat(checkArchiveCount(5)).isTrue();
    }

    private static final String EXTERNALAPP_URL = "http://localhost:8090/test";

    @Test
    void createUserCards() throws URISyntaxException {
        ArrayList<String> externalRecipients = new ArrayList<>();
        externalRecipients.add(API_TEST_EXTERNAL_RECIPIENT_1);

        CardPublicationData card = CardPublicationData.builder().publisher("newPublisherId").processVersion("0")
                .processInstanceId("PROCESS_CARD_USER").severity(SeverityEnum.INFORMATION)
                .process("PROCESS_CARD_USER")
                .state("STATE1")
                .title(I18nPublicationData.builder().key("title").build())
                .summary(I18nPublicationData.builder().key("summary").build())
                .startDate(Instant.now())
                .externalRecipients(externalRecipients)
                .state("state1")
                .build();

        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(EXTERNALAPP_URL)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                );

        Assertions.assertThatCode(() -> cardProcessingService.processUserCard(card, currentUserWithPerimeters, token))
            .doesNotThrowAnyException();
        Assertions.assertThat(checkCardPublisherId(card)).isTrue();

    }

    @Test
    void createUserCardsWithWrongPublisher() throws URISyntaxException {
        ArrayList<String> externalRecipients = new ArrayList<>();
        externalRecipients.add(API_TEST_EXTERNAL_RECIPIENT_1);

        CardPublicationData card = CardPublicationData.builder().publisher("PUBLISHER_X").processVersion("0")
                .processInstanceId("PROCESS_CARD_USER").severity(SeverityEnum.INFORMATION)
                .process("PROCESS_CARD_USER")
                .state("STATE1")
                .title(I18nPublicationData.builder().key("title").build())
                .summary(I18nPublicationData.builder().key("summary").build())
                .startDate(Instant.now())
                .externalRecipients(externalRecipients)
                .state("state1")
                .build();

        Assertions.assertThatThrownBy(() -> cardProcessingService.processUserCard(card, currentUserWithPerimeters, token))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Publisher is not valid, the card is rejected");
        Assertions.assertThat(checkCardCount(0)).isTrue();
        Assertions.assertThat(checkArchiveCount(0)).isTrue();
    }

    @Test
    void childCards() throws URISyntaxException {
        int numberOfCards = 1;
        List<CardPublicationData> cards = createRandomCards(numberOfCards);

        cards.forEach(card -> cardProcessingService.processCard(card));

        Long block = cardRepository.count();
        Assertions.assertThat(block).withFailMessage(
                "The number of registered cards should be '%d' but is " + "'%d' actually",
                numberOfCards, block).isEqualTo(numberOfCards);

        CardPublicationData firstCard = cards.get(0);
        String id = firstCard.getId();

        ArrayList<String> externalRecipients = new ArrayList<>();
        externalRecipients.add(API_TEST_EXTERNAL_RECIPIENT_1);

        CardPublicationData card = CardPublicationData.builder().publisher("newPublisherId").processVersion("0")
                .processInstanceId("PROCESS_CARD_USER").severity(SeverityEnum.INFORMATION)
                .process("PROCESS_CARD_USER")
                .parentCardId(cards.get(0).getId())
                .initialParentCardUid(cards.get(0).getUid())
                .state("STATE1")
                .title(I18nPublicationData.builder().key("title").build())
                .summary(I18nPublicationData.builder().key("summary").build())
                .startDate(Instant.now())
                .externalRecipients(externalRecipients)
                .state("state1")
                .build();

        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(EXTERNALAPP_URL)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                );

        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(EXTERNALAPP_URL + "/PROCESS_CARD_USER.PROCESS_CARD_USER")))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                );

        Assertions.assertThatCode(() -> cardProcessingService.processUserCard(card, currentUserWithPerimeters, token))
                .doesNotThrowAnyException();
        Assertions.assertThat(checkCardPublisherId(card)).isTrue();

        Assertions.assertThat(cardRepository.count())
                .withFailMessage("The number of registered cards should be '%d' but is '%d' ",
                        2, block)
                .isEqualTo(2);

        cardProcessingService.deleteCard(cards.get(0).getId(), token);

        Assertions.assertThat(cardRepository.count())
                .withFailMessage("The number of registered cards should be '%d' but is '%d' "
                                + "when first parent card is deleted(processInstanceId:'%s').",
                        0, block, id)
                .isZero();
    }

    @Test
    void createCardWithError() {
        Assertions.assertThatThrownBy(() -> cardProcessingService.processCard(generateWrongCardData("PUBLISHER_1", "PROCESS_1")))
                .isInstanceOf(ConstraintViolationException.class);
        Assertions.assertThat(checkCardCount(0)).isTrue();
        Assertions.assertThat(checkArchiveCount(0)).isTrue();
    }

    @Test
    void preserveData() {
        // as date are stored in millis in mongo , we should not use nanos otherwise
        // we will have different results when comparing date send and date stored 
        // resulting  in failed test 

        Instant start = Instant.ofEpochMilli(Instant.now().toEpochMilli()).plusSeconds(3600);
        
        LinkedHashMap data = new LinkedHashMap();
        data.put("int", 123);
        data.put("string", "test");
        LinkedHashMap subdata = new LinkedHashMap();
        subdata.put("int", 456);
        subdata.put("string", "test2");
        data.put("object", subdata);
        ArrayList<String> entityRecipients = new ArrayList<>();
        entityRecipients.add("Dispatcher");
        entityRecipients.add("Planner");

        List<Integer> daysOfWeek = new ArrayList<>();
        List<Integer> months = new ArrayList<>();
        daysOfWeek.add(2);
        daysOfWeek.add(3);
        months.add(2);
        months.add(3);
        Integer duration = 15;
        HoursAndMinutes hoursAndMinutes = new HoursAndMinutesPublicationData(2,10);
        RecurrencePublicationData recurrence = new RecurrencePublicationData("timezone", daysOfWeek, hoursAndMinutes, duration, months);

        CardPublicationData newCard = CardPublicationData.builder().publisher("publisher(")
                .processVersion("0").processInstanceId("PROCESS_1").severity(SeverityEnum.ALARM)
                .startDate(start).title(I18nPublicationData.builder().key("title").build())
                .summary(I18nPublicationData.builder().key("summary").parameter("arg1", "value1")
                        .build())
                .endDate(start.plusSeconds(60)).lttd(start.minusSeconds(600))
                .tag("tag1").tag("tag2").data(data)
                .entityRecipients(entityRecipients)
                .timeSpan(TimeSpanPublicationData.builder().start(Instant.ofEpochMilli(123l)).recurrence(recurrence).build())
                .process("process1")
                .state("state1")
                .publisherType(PublisherTypeEnum.EXTERNAL)
                .representative("ENTITY1")
                .representativeType(PublisherTypeEnum.ENTITY)
                .wktGeometry("POINT (6.530 53.221)")
                .wktProjection("EPSG:4326")
                .secondsBeforeTimeSpanForReminder(Integer.valueOf(1000))
                .build();
        cardProcessingService.processCard(newCard);
        CardPublicationData persistedCard = cardRepository.findById(newCard.getId()).get();
        assertThat(persistedCard).isEqualTo(newCard);
        assertThat(persistedCard.getTitleTranslated()).isEqualTo("Title translated");
        assertThat(persistedCard.getSummaryTranslated()).isEqualTo("Summary translated value1");

        ArchivedCardPublicationData archivedPersistedCard = archiveRepository.findById(newCard.getUid()).get();
        assertThat(archivedPersistedCard).usingRecursiveComparison().ignoringFields("uid", "id",
        "actions", "timeSpans", "deletionDate", "entitiesAcks").isEqualTo(newCard);
        assertThat(archivedPersistedCard.getId()).isEqualTo(newCard.getUid());
        assertThat(archivedPersistedCard.getTitleTranslated()).isEqualTo("Title translated");
        assertThat(archivedPersistedCard.getSummaryTranslated()).isEqualTo("Summary translated value1");

        assertThat(testCardReceiver.getCardQueue()).hasSize(1);
    }

    private boolean checkCardCount(long expectedCount) {
        Long count = cardRepository.count();
        if (count == expectedCount) {
            return true;
        } else {
            log.warn("Expected card count " + expectedCount + " but was " + count);
            return false;
        }
    }

    private boolean checkCardPublisherId(CardPublicationData card) {
        if (user.getEntities().contains(card.getPublisher())) {
            return true;
        } else {
            log.warn("Expected card publisher id is " + user.getEntities().get(0) + " but it was " + card.getPublisher());
            return false;
        }
    }

    private boolean checkArchiveCount(long expectedCount) {
        Long count = archiveRepository.count();
        if (count == expectedCount)
            return true;
        else {
            log.warn("Expected card count " + expectedCount + " but was " + count);
            return false;
        }
    }

    @Test
    void deleteOneCard_with_it_s_Id() {

        int numberOfCards = 13;
        List<CardPublicationData> cards = createRandomCards(numberOfCards);

        cards.forEach(card-> cardProcessingService.processCard(card));

        CardPublicationData firstCard = cards.get(0);
        String id = firstCard.getId();
        cardProcessingService.deleteCard(id, token);

        /* one card should be deleted(the first one) */
        int thereShouldBeOneCardLess = numberOfCards - 1;

        assertThat(cardRepository.count()).isEqualTo(thereShouldBeOneCardLess);
    }

    @Test
    void deleteOneCard_with_card_no_id() {

        int numberOfCards = 13;
        List<CardPublicationData> cards = createRandomCards(numberOfCards);

        cards.forEach(card-> cardProcessingService.processCard(card));
        CardPublicationData firstCard = cards.get(0);
        firstCard.setId(null);
        cardProcessingService.prepareAndDeleteCard(firstCard);

        /* one card should be deleted(the first one) */
        int thereShouldBeOneCardLess = numberOfCards - 1;

        assertThat(cardRepository.count()).isEqualTo(thereShouldBeOneCardLess);
    }

    @Test
    void deleteOneCard_with_card_with_id() {

        int numberOfCards = 13;
        List<CardPublicationData> cards = createRandomCards(numberOfCards);

        cards.forEach(card-> cardProcessingService.processCard(card));

        CardPublicationData firstCard = cards.get(0);
        cardProcessingService.prepareAndDeleteCard(firstCard);

        /* one card should be deleted(the first one) */
        int thereShouldBeOneCardLess = numberOfCards - 1;

        assertThat(cardRepository.count()).isEqualTo(thereShouldBeOneCardLess);
    }

    @Test
    void deleteOneCard_with_externalRecipient() throws URISyntaxException {

        int numberOfCards = 5;
        List<CardPublicationData> cards = createRandomCards(numberOfCards);
        cards.forEach(card-> cardProcessingService.processCard(card));

        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(EXTERNALAPP_URL + "/" + cards.get(0).getId())))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                );

        CardPublicationData firstCard = cards.get(0);
        List<String> externalRecipients = new ArrayList<>();
        externalRecipients.add(API_TEST_EXTERNAL_RECIPIENT_1);
        firstCard.setExternalRecipients(externalRecipients);
        cardProcessingService.prepareAndDeleteCard(firstCard);

        /* one card should be deleted(the first one) */
        int thereShouldBeOneCardLess = numberOfCards - 1;

        assertThat(cardRepository.count()).isEqualTo(thereShouldBeOneCardLess);
    }

    @Test
    void deleteOneCard_with_invalid_externalRecipient() throws URISyntaxException {

        int numberOfCards = 3;
        List<CardPublicationData> cards = createRandomCards(numberOfCards);

        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(EXTERNALAPP_URL)))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                );

        cards.forEach(card-> cardProcessingService.processCard(card));

        CardPublicationData firstCard = cards.get(0);
        List<String> externalRecipients = new ArrayList<>();
        externalRecipients.add("thisIsAnInvalidExternalRecipient");
        firstCard.setExternalRecipients(externalRecipients);
        cardProcessingService.prepareAndDeleteCard(firstCard);

        /* one card should be deleted(the first one) */
        int thereShouldBeOneCardLess = numberOfCards - 1;

        assertThat(cardRepository.count()).isEqualTo(thereShouldBeOneCardLess);
    }

    @NotNull
    private List<CardPublicationData> createRandomCards(int numberOfCards) {
        EasyRandom easyRandom = instantiateRandomCardGenerator();
        List<CardPublicationData> cards = instantiateSeveralRandomCards(easyRandom, numberOfCards);
        cards.forEach(c -> {
            c.setParentCardId(null);
            c.setInitialParentCardUid(null);

            // process, processVersion, title and summary can't be random anymore because we check if i18n file exists via a mock (see issue #3178)
            c.setProcess("process1");
            c.setProcessVersion("0");
            c.setTitle(I18nPublicationData.builder().key("title").build());
            c.setSummary(I18nPublicationData.builder().key("summary").build());
        });
        return cards;
    }

    private List<CardPublicationData> instantiateSeveralRandomCards(EasyRandom randomGenerator, int cardNumber) {

        List<CardPublicationData> cardsList = randomGenerator.objects(CardPublicationData.class, cardNumber)
                .collect(Collectors.toList());

        // endDate must be after startDate
        // expirationDate must be after startDate
        // toNotify must be true
        if (cardsList != null) {
            for (CardPublicationData cardPublicationData : cardsList) {
                if (cardPublicationData != null) {
                    Instant startDateInstant = cardPublicationData.getStartDate();
                    if (startDateInstant != null && startDateInstant
                            .compareTo(cardPublicationData.getEndDate()) >= 0) {
                        cardPublicationData.setEndDate(startDateInstant.plusSeconds(86400));
                    }
                    if (startDateInstant != null && startDateInstant
                            .compareTo(cardPublicationData.getExpirationDate()) >= 0) {
                        cardPublicationData.setExpirationDate(startDateInstant.plusSeconds(86400));
                    }
                    cardPublicationData.setProcess("api_test");
                    cardPublicationData.setState("messageState");
                    cardPublicationData.setProcessVersion("1");
                    cardPublicationData.setToNotify(true);

                    // process, processVersion, title and summary can't be random anymore because we check if i18n file exists via a mock (see issue #3178)
                    cardPublicationData.setProcess("process1");
                    cardPublicationData.setProcessVersion("0");
                    cardPublicationData.setTitle(I18nPublicationData.builder().key("title").build());
                    cardPublicationData.setSummary(I18nPublicationData.builder().key("summary").build());
                }
            }
        }
        return cardsList;
    }

    @NotNull
    private EasyRandom instantiateRandomCardGenerator() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plus(1, ChronoUnit.DAYS);

        LocalTime nine = LocalTime.of(9, 0);
        LocalTime fifteen = LocalTime.of(17, 0);

        EasyRandomParameters parameters = new EasyRandomParameters().seed(5467L).objectPoolSize(100)
                .randomizationDepth(3).charset(forName("UTF-8")).timeRange(nine, fifteen)
                .dateRange(today, tomorrow).stringLengthRange(5, 50).collectionSizeRange(1, 10)
                .excludeField(named("data"))
                .excludeField(named("parameters"))
                .excludeField(named("timeSpans"))
                .scanClasspathForConcreteTypes(true).overrideDefaultInitialization(false)
                .ignoreRandomizationErrors(true);

        return new EasyRandom(parameters);
    }

    @Test
    void deleteCards_Non_existentId() {
        int numberOfCards = 13;
        List<CardPublicationData> cards = createRandomCards(numberOfCards);

        cards.forEach(card-> cardProcessingService.processCard(card));

        Long block = cardRepository.count();
        Assertions.assertThat(block).withFailMessage(
                "The number of registered cards should be '%d' but is " + "'%d' actually",
                numberOfCards, block).isEqualTo(numberOfCards);

        final String id = generateIdNotInCardRepository();
        cardProcessingService.deleteCard(id, token);

        int expectedNumberOfCards = numberOfCards;/* no card should be deleted */

        block = cardRepository.count();
        Assertions.assertThat(block)
                .withFailMessage(
                        "The number of registered cards should remain '%d' but is '%d' "
                                + "when an non-existing processInstanceId('%s') is used.",
                        expectedNumberOfCards, block, id)
                .isEqualTo(expectedNumberOfCards);

    }

    private String generateIdNotInCardRepository() {
        Set<String> ids = new HashSet<>();
        cardRepository.findAll().forEach(card ->ids.add(card.getId()));

        EasyRandom easyRandom = instantiateRandomCardGenerator();
        String id = easyRandom.nextObject(String.class);
        while (ids.contains(id)) {
            id = easyRandom.nextObject(String.class);
        }
        return id;
    }

    @Test
    void findCardToDelete_should_Only_return_Card_with_NullData() {
        EasyRandom easyRandom = instantiateRandomCardGenerator();
        List<CardPublicationData> card = instantiateSeveralRandomCards(easyRandom, 1);
        String fakeDataContent = easyRandom.nextObject(String.class);
        CardPublicationData publishedCard = card.get(0);
        publishedCard.setParentCardId(null);
        publishedCard.setInitialParentCardUid(null);
        publishedCard.setData(fakeDataContent);

        cardProcessingService.processCard(publishedCard);

        Long block = cardRepository.count();
        Assertions.assertThat(block)
                .withFailMessage("The number of registered cards should be '%d' but is '%d", 1, block)
                .isEqualTo(1);

        String computedCardId = publishedCard.getProcess() + "." + publishedCard.getProcessInstanceId();
        CardPublicationData cardToDelete = cardRepositoryService.findCardById(computedCardId);

        Assertions.assertThat(cardToDelete).isNotNull();

        Object resultingData = cardToDelete.getData();
        Assertions.assertThat(resultingData).isNull();

    }
        
    @Test
    void processAddUserAcknowledgement() {
        EasyRandom easyRandom = instantiateRandomCardGenerator();
        int numberOfCards = 1;
        List<CardPublicationData> cards = instantiateSeveralRandomCards(easyRandom, numberOfCards);
        cards.get(0).setUsersAcks(null);
        cards.get(0).setParentCardId(null);
        cards.get(0).setInitialParentCardUid(null);
        cards.forEach(card-> cardProcessingService.processCard(card));

        Long block = cardRepository.count();
        Assertions.assertThat(block).withFailMessage(
                        "The number of registered cards should be '%d' but is " + "'%d' actually",
                        numberOfCards, block).isEqualTo(numberOfCards);

        CardPublicationData firstCard = cardRepository.findById(cards.get(0).getId()).get();
        Assertions.assertThat(firstCard.getUsersAcks()).as("Expecting Card doesn't contain any ack at the beginning").isNullOrEmpty();
        
        String cardUid = firstCard.getUid();
        user.setLogin("aaa");

        UserBasedOperationResult res = cardProcessingService.processUserAcknowledgement(cardUid, currentUserWithPerimeters, user.getEntities());
        Assertions.assertThat(res.isCardFound() && res.getOperationDone()).as("Expecting one successful addition").isTrue();
        
        CardPublicationData cardReloaded = cardRepository.findByUid(cardUid).get();
        Assertions.assertThat(cardReloaded.getUsersAcks()).as("Expecting Card after ack processing contains exactly an ack by user aaa").containsExactly("aaa");
        Assertions.assertThat(cardReloaded.getEntitiesAcks()).as("Expecting Card after ack processing contains exactly 2 entities in field entitiesAcks").containsExactly("newPublisherId", "entity2");

        user.setLogin("bbb");
        user.setEntities(Arrays.asList("newPublisherId_bbb", "entity2_bbb"));
        res = cardProcessingService.processUserAcknowledgement(cardUid, currentUserWithPerimeters, user.getEntities());
        Assertions.assertThat(res.isCardFound() && res.getOperationDone()).as("Expecting one successful addition").isTrue();
        
        cardReloaded = cardRepository.findByUid(cardUid).get();
        Assertions.assertThat(cardReloaded.getUsersAcks()).as("Expecting Card after ack processing contains exactly two acks by users aaa and bbb").containsExactly("aaa","bbb");
        Assertions.assertThat(cardReloaded.getEntitiesAcks()).as("Expecting Card after ack processing contains exactly 4 entities in field entitiesAcks")
                .containsExactly("newPublisherId", "entity2", "newPublisherId_bbb", "entity2_bbb");

        //try to insert aaa again
        user.setLogin("aaa");
        user.setEntities(Arrays.asList("newPublisherId", "entity2"));
        res = cardProcessingService.processUserAcknowledgement(cardUid, currentUserWithPerimeters, user.getEntities());
        Assertions.assertThat(res.isCardFound() && res.getOperationDone()).as("Expecting update to lastAckDate is done").isTrue();
        
        cardReloaded = cardRepository.findByUid(cardUid).get();
        Assertions.assertThat(cardReloaded.getUsersAcks()).as("Expecting  Card after ack processing contains exactly two acks by users aaa(only once) and bbb").containsExactly("aaa","bbb");
        Assertions.assertThat(cardReloaded.getEntitiesAcks()).as("Expecting Card after ack processing contains exactly 4 entities in field entitiesAcks")
                .containsExactly("newPublisherId", "entity2", "newPublisherId_bbb", "entity2_bbb");
    }



    @Test
    void processDeleteUserAcknowledgement() {
        EasyRandom easyRandom = instantiateRandomCardGenerator();
        int numberOfCards = 2;
        List<CardPublicationData> cards = instantiateSeveralRandomCards(easyRandom, numberOfCards);
        cards.get(0).setUsersAcks(Arrays.asList("someUser","someOtherUser"));
        cards.get(1).setUsersAcks(null);
        cards.get(0).setParentCardId(null);
        cards.get(0).setInitialParentCardUid(null);
        cards.get(1).setParentCardId(null);
        cards.get(1).setInitialParentCardUid(null);
        cards.forEach(card-> cardProcessingService.processCard(card));

        Long block = cardRepository.count();
        Assertions.assertThat(block).withFailMessage(
                        "The number of registered cards should be '%d' but is " + "'%d' actually",
                        numberOfCards, block).isEqualTo(numberOfCards);

        CardPublicationData firstCard = cardRepository.findById(cards.get(0).getId()).get();
        Assertions.assertThat(firstCard.getUsersAcks()).as("Expecting Card contains exactly 2 user acks").hasSize(2);
        
        String cardUid = firstCard.getUid();

        UserBasedOperationResult res = cardProcessingService.deleteUserAcknowledgement(cardUid, "someUser");
        firstCard = cardRepository.findByUid(cardUid).get();
        Assertions.assertThat(firstCard.getUsersAcks()).as("Expecting Card1 doesn't contain someUser's card acknowledgement").containsExactly("someOtherUser");
        Assertions.assertThat(res.isCardFound() && res.getOperationDone()).isTrue();
        
        res = cardProcessingService.deleteUserAcknowledgement(cardUid, "someUser");
        firstCard = cardRepository.findByUid(cardUid).get();
        Assertions.assertThat(firstCard.getUsersAcks()).as("Expecting Card1 doesn't contain someUser card acknowledgement").containsExactly("someOtherUser");
        Assertions.assertThat(res.isCardFound() && !res.getOperationDone()).isTrue();
        
        CardPublicationData secondCard = cardRepository.findById(cards.get(1).getId()).get();
        String secondCardUid = secondCard.getUid();
        res = cardProcessingService.deleteUserAcknowledgement(secondCardUid, "someUser");
        secondCard = cardRepository.findByUid(secondCardUid).get();
        Assertions.assertThat(secondCard.getUsersAcks()).as("Expecting no errors from deleting unexisting user ack from a card(card2) not having any user ack").isNullOrEmpty();
        Assertions.assertThat(res.isCardFound() && !res.getOperationDone()).isTrue();
    }

    @Test
    void processAddUserRead() {
        EasyRandom easyRandom = instantiateRandomCardGenerator();
        int numberOfCards = 1;
        List<CardPublicationData> cards = instantiateSeveralRandomCards(easyRandom, numberOfCards);
        cards.get(0).setUsersReads(null);
        cards.get(0).setParentCardId(null);
        cards.get(0).setInitialParentCardUid(null);
        cards.forEach(card-> cardProcessingService.processCard(card));

        Long block = cardRepository.count();
        Assertions.assertThat(block).withFailMessage(
                        "The number of registered cards should be '%d' but is " + "'%d' actually",
                        numberOfCards, block).isEqualTo(numberOfCards);

        CardPublicationData firstCard = cardRepository.findById(cards.get(0).getId()).get();
        Assertions.assertThat(firstCard.getUsersReads()).as("Expecting Card doesn't contain any read at the beginning").isNullOrEmpty();

        String cardUid = firstCard.getUid();

        UserBasedOperationResult res = cardProcessingService.processUserRead(cardUid, "aaa");
        Assertions.assertThat(res.isCardFound() && res.getOperationDone()).as("Expecting one successful addition").isTrue();

        CardPublicationData cardReloaded = cardRepository.findByUid(cardUid).get();
        Assertions.assertThat(cardReloaded.getUsersReads()).as("Expecting Card after read processing contains exactly an read by user aaa").containsExactly("aaa");

        res = cardProcessingService.processUserRead(cardUid, "bbb");
        Assertions.assertThat(res.isCardFound() && res.getOperationDone()).as("Expecting one successful addition").isTrue();

        cardReloaded = cardRepository.findByUid(cardUid).get();
        Assertions.assertThat(cardReloaded.getUsersReads()).as("Expecting Card after read processing contains exactly two read by users aaa and bbb").containsExactly("aaa","bbb");
        //try to insert aaa again
        res = cardProcessingService.processUserRead(cardUid, "aaa");
        Assertions.assertThat(res.isCardFound() && !res.getOperationDone()).as("Expecting no addition because already done").isTrue();

        cardReloaded = cardRepository.findByUid(cardUid).get();
        Assertions.assertThat(cardReloaded.getUsersReads()).as("Expecting  Card after read processing contains exactly two read by users aaa(only once) and bbb").containsExactly("aaa","bbb");
    }

    @Test
    void processDeleteUserRead() {
        EasyRandom easyRandom = instantiateRandomCardGenerator();
        int numberOfCards = 2;
        List<CardPublicationData> cards = instantiateSeveralRandomCards(easyRandom, numberOfCards);
        cards.get(0).setUsersReads(Arrays.asList("someUser","someOtherUser"));
        cards.get(1).setUsersReads(null);
        cards.get(0).setParentCardId(null);
        cards.get(0).setInitialParentCardUid(null);
        cards.get(1).setParentCardId(null);
        cards.get(1).setInitialParentCardUid(null);
        cards.forEach(card-> cardProcessingService.processCard(card));

        Long block = cardRepository.count();
        Assertions.assertThat(block).withFailMessage(
                        "The number of registered cards should be '%d' but is " + "'%d' actually",
                        numberOfCards, block).isEqualTo(numberOfCards);

        CardPublicationData firstCard = cardRepository.findById(cards.get(0).getId()).get();
        Assertions.assertThat(firstCard.getUsersReads()).as("Expecting Card contains exactly 2 user reads").hasSize(2);
        
        String cardUid = firstCard.getUid();

        UserBasedOperationResult res = cardProcessingService.deleteUserRead(cardUid, "someUser");
        firstCard = cardRepository.findByUid(cardUid).get();
        Assertions.assertThat(firstCard.getUsersReads()).as("Expecting Card1 doesn't contain someUser's card read").containsExactly("someOtherUser");
        Assertions.assertThat(res.isCardFound() && res.getOperationDone()).isTrue();
        
        res = cardProcessingService.deleteUserRead(cardUid, "someUser");
        firstCard = cardRepository.findByUid(cardUid).get();
        Assertions.assertThat(firstCard.getUsersReads()).as("Expecting Card1 doesn't contain someUser card read").containsExactly("someOtherUser");
        Assertions.assertThat(res.isCardFound() && !res.getOperationDone()).isTrue();
        
        CardPublicationData secondCard = cardRepository.findById(cards.get(1).getId()).get();
        String secondCardUid = secondCard.getUid();
        res = cardProcessingService.deleteUserRead(secondCardUid, "someUser");
        secondCard = cardRepository.findByUid(secondCardUid).get();
        Assertions.assertThat(secondCard.getUsersReads()).as("Expecting no errors from deleting unexisting user read from a card(card2) not having any user read").isNullOrEmpty();
        Assertions.assertThat(res.isCardFound() && !res.getOperationDone()).isTrue();
    }

    @Test
    void validate_processOk() {

        Assertions.assertThatCode(() -> cardProcessingService.processCard(
                CardPublicationData.builder()
                        .uid("uid_1")
                        .publisher("PUBLISHER_1").processVersion("0")
                        .processInstanceId("PROCESS_1").severity(SeverityEnum.ALARM)
                        .title(I18nPublicationData.builder().key("title").build())
                        .summary(I18nPublicationData.builder().key("summary").build())
                        .startDate(Instant.now())
                        .timeSpan(TimeSpanPublicationData.builder()
                                .start(Instant.ofEpochMilli(123l)).build())
                        .process("process1")
                        .state("state1")
                        .build())).doesNotThrowAnyException();

        CardPublicationData childCard = CardPublicationData.builder()
                .parentCardId("process1.PROCESS_1")
                .initialParentCardUid("uid_1")
                .publisher("PUBLISHER_1").processVersion("0")
                .processInstanceId("PROCESS_1").severity(SeverityEnum.ALARM)
                .title(I18nPublicationData.builder().key("title").build())
                .summary(I18nPublicationData.builder().key("summary").build())
                .startDate(Instant.now())
                .timeSpan(TimeSpanPublicationData.builder()
                        .start(Instant.ofEpochMilli(123l)).build())
                .process("process2")
                .state("state2")
                .build();

        cardProcessingService.validate(childCard);
    }

    @Test
    void validate_parentCardId_NotIdPresentInDb() {

        CardPublicationData card = CardPublicationData.builder()
                .parentCardId("id_1")
                .publisher("PUBLISHER_1").processVersion("0")
                .process("PROCESS_1")
                .processInstanceId("PROCESS_1").severity(SeverityEnum.ALARM)
                .title(I18nPublicationData.builder().key("title").build())
                .summary(I18nPublicationData.builder().key("summary").build())
                .startDate(Instant.now())
                .timeSpan(TimeSpanPublicationData.builder()
                        .start(Instant.ofEpochMilli(123l)).build())
                .build();
        try {
            cardProcessingService.validate(card);
        } catch (ConstraintViolationException e) {
            Assertions.assertThat(e.getMessage()).isEqualTo("The parentCardId " + card.getParentCardId() + " is not the id of any card");
        }
    }

    @Test
    void validate_initialParentCardUid_NotPresentInDb() {

        Assertions.assertThatCode(() -> cardProcessingService.processCard(
                CardPublicationData.builder()
                        .uid("uid_1")
                        .publisher("PUBLISHER_1").processVersion("0")
                        .processInstanceId("PROCESS_1").severity(SeverityEnum.ALARM)
                        .title(I18nPublicationData.builder().key("title").build())
                        .summary(I18nPublicationData.builder().key("summary").build())
                        .startDate(Instant.now())
                        .timeSpan(TimeSpanPublicationData.builder()
                                .start(Instant.ofEpochMilli(123l)).build())
                        .process("process1")
                        .state("state1")
                        .build())).doesNotThrowAnyException();

        CardPublicationData childCard = CardPublicationData.builder()
                .parentCardId("process1.PROCESS_1")
                .initialParentCardUid("initialParentCardUidNotExisting")
                .publisher("PUBLISHER_1").processVersion("0")
                .processInstanceId("PROCESS_1").severity(SeverityEnum.ALARM)
                .title(I18nPublicationData.builder().key("title").build())
                .summary(I18nPublicationData.builder().key("summary").build())
                .startDate(Instant.now())
                .timeSpan(TimeSpanPublicationData.builder()
                        .start(Instant.ofEpochMilli(123l)).build())
                .process("process2")
                .state("state2")
                .build();

        try {
            cardProcessingService.validate(childCard);
        } catch (ConstraintViolationException e) {
            Assertions.assertThat(e.getMessage()).isEqualTo("The initialParentCardUid " +
                    childCard.getInitialParentCardUid() + " is not the uid of any card");
        }
    }

    @Test
    void validate_noParentCardId_processOk() {

        CardPublicationData card = CardPublicationData.builder()
                .publisher("PUBLISHER_1").processVersion("0")
                .processInstanceId("PROCESS_1").severity(SeverityEnum.ALARM)
                .title(I18nPublicationData.builder().key("title").build())
                .summary(I18nPublicationData.builder().key("summary").build())
                .startDate(Instant.now())
                .timeSpan(TimeSpanPublicationData.builder()
                        .start(Instant.ofEpochMilli(123l)).build())
                .process("process1")
                .state("state1")
                .build();

        cardProcessingService.validate(card);
    }

    @Test
    void processKeepChildCardsNull() {
        EasyRandom easyRandom = instantiateRandomCardGenerator();
        int numberOfCards = 1;
        List<CardPublicationData> cards = instantiateSeveralRandomCards(easyRandom, numberOfCards);
        cards.get(0).setParentCardId(null);
        cards.get(0).setInitialParentCardUid(null);
        cards.get(0).setKeepChildCards(null);
        cards.forEach(card-> cardProcessingService.processCard(card));

        Long block = cardRepository.count();
        Assertions.assertThat(block).withFailMessage(
                        "The number of registered cards should be '%d' but is " + "'%d' actually",
                        numberOfCards, block).isEqualTo(numberOfCards);

        CardPublicationData firstCard = cardRepository.findById(cards.get(0).getId()).get();
        Assertions.assertThat(firstCard.getKeepChildCards()).isNotNull();
        Assertions.assertThat(firstCard.getKeepChildCards()).isFalse();
    }

    @Test
    void processCardWithEntitiesRequiredToRespondNotSubsetOfEntitiesAllowedToRespond() {

        // Generate random card (this generator is common to all tests so it just generates a random list for
        // entitiesAllowedToRespond and for entitiesRequiredToRespond
        EasyRandom easyRandom = instantiateRandomCardGenerator();
        int numberOfCards = 1;
        List<CardPublicationData> cards = instantiateSeveralRandomCards(easyRandom, numberOfCards);

        cards.get(0).setParentCardId(null);
        cards.get(0).setInitialParentCardUid(null);

        // Generate a list of entitiesAllowedToRespond and use it to initialize entitiesRequiredToRespond as well
        int numberOfEntitiesAllowedToRespond = 10;
        List<String> entitiesAllowedToRespond = easyRandom.objects(String.class, numberOfEntitiesAllowedToRespond)
                .collect(Collectors.toList());

        List<String> entitiesRequiredToRespond = new ArrayList<>(entitiesAllowedToRespond);

        // Take one entity out of entitiesAllowedToRespond to make sure entitiesRequiredToRespond is not a subset of
        // entitiesRequiredToRespond
        entitiesAllowedToRespond.remove(0);

        cards.get(0).setEntitiesAllowedToRespond(entitiesAllowedToRespond);
        cards.get(0).setEntitiesRequiredToRespond(entitiesRequiredToRespond);

        cards.forEach(card-> cardProcessingService.processCard(card));

        Long block = cardRepository.count();
        Assertions.assertThat(block).withFailMessage(
                "The number of registered cards should be '%d' but is " + "'%d' actually",
                numberOfCards, block).isEqualTo(numberOfCards);
    }

    @Test
    void deleteCards_by_endDate() {

        EasyRandom easyRandom = instantiateRandomCardGenerator();
        int numberOfCards = 10;

        List<CardPublicationData> cards = instantiateSeveralRandomCards(easyRandom, numberOfCards);
        
        Instant ref = Instant.now();
        AtomicInteger i = new AtomicInteger(1);
        cards.forEach(c -> {
            c.setParentCardId(null);
            c.setInitialParentCardUid(null);
            c.setExpirationDate(null);

            if (i.get() % 2 == 0) {
                c.setEndDate(null);
            } else {
                c.setEndDate(ref.minus(i.get(),ChronoUnit.DAYS));
            }
            // last 2 card should not be removed
            if (i.get() > 8) {
                c.setEndDate(ref.plus(1,ChronoUnit.DAYS));
            }
            // second card should not be removed
            if (i.get() == 2) {
                c.setStartDate(ref.plus(i.incrementAndGet(),ChronoUnit.DAYS));
            } else  {
                c.setStartDate(ref.minus(i.incrementAndGet(),ChronoUnit.DAYS));
            }

        });

        cards.forEach(card -> cardProcessingService.processCard(card));
        cardProcessingService.deleteCards(Instant.now());

        /* 7 cards should be removed */
        int thereShouldBeOneCardLess = numberOfCards - 7;
        Assertions.assertThat(cardRepository.count()).isEqualTo(thereShouldBeOneCardLess);

    }

    @Test
    void deleteCards_by_expirationDate() {

        EasyRandom easyRandom = instantiateRandomCardGenerator();
        int numberOfCards = 10;

        List<CardPublicationData> cards = instantiateSeveralRandomCards(easyRandom, numberOfCards);

        Instant ref = Instant.now();
        AtomicInteger i = new AtomicInteger(1);
        cards.forEach(c -> {
            c.setParentCardId(null);
            c.setInitialParentCardUid(null);
            c.setEndDate(null);

            if (i.get() % 2 == 0) {
                c.setExpirationDate(null);
            } else {
                c.setExpirationDate(ref.minus(i.get(),ChronoUnit.DAYS));
            }
            if (i.get() > 8) {
                c.setExpirationDate(ref.minus(1,ChronoUnit.DAYS));
            }
            if (i.get() == 2) {
                c.setExpirationDate(ref.plus(i.incrementAndGet(),ChronoUnit.DAYS));
            } else  {
                c.setStartDate(ref.minus(i.incrementAndGet(),ChronoUnit.DAYS));
            }
        });

        cards.forEach(card -> cardProcessingService.processCard(card));
        cardProcessingService.deleteCardsByExpirationDate(Instant.now());

        /* 6 cards should be removed */
        int thereShouldBeFourCardLeft = numberOfCards - 6;
        Assertions.assertThat(cardRepository.count()).isEqualTo(thereShouldBeFourCardLeft);

    }

    @Test
    void checkUserPublisher() {

        User user = new User();
        user.setLogin("wrongUser");
        user.setFirstName("Test");
        user.setLastName("User");
        CurrentUserWithPerimeters wrongUser = new CurrentUserWithPerimeters();
        wrongUser.setUserData(user);

        CardPublicationData card = generateOneCard(currentUserWithPerimeters.getUserData().getLogin());
        card.setPublisherType(PublisherTypeEnum.EXTERNAL);
        Optional<CurrentUserWithPerimeters> optionalWrongUser = Optional.of(wrongUser);
        Assertions.assertThatThrownBy(() -> cardProcessingService.processCard(card, optionalWrongUser, token))
        .isInstanceOf(ApiErrorException.class).hasMessage("Card publisher is set to dummyUser and account login is wrongUser, the card cannot be sent");
        Assertions.assertThat(checkCardCount(0)).isTrue();


        cardProcessingService.processCard(card, Optional.of(currentUserWithPerimeters), token);
        Assertions.assertThat(checkCardCount(1)).isTrue();
        String cardId = card.getId();
        Assertions.assertThatThrownBy(() -> cardProcessingService.deleteCard(cardId, optionalWrongUser, token))
            .isInstanceOf(ApiErrorException.class).hasMessage("Card publisher is set to dummyUser and account login is wrongUser, the card cannot be deleted");
        Assertions.assertThat(checkCardCount(1)).isTrue();
       
        cardProcessingService.deleteCard(card.getId(), Optional.of(currentUserWithPerimeters), token);
        Assertions.assertThat(checkCardCount(0)).isTrue();
    }

    @Test
    void checkUserRepresentative() {

        User user = new User();
        user.setLogin("wrongUser");
        user.setFirstName("Test");
        user.setLastName("User");
        CurrentUserWithPerimeters wrongUser = new CurrentUserWithPerimeters();
        wrongUser.setUserData(user);

        CardPublicationData card = generateOneCard("IGNORED_PUBLISHER");
        card.setPublisherType(PublisherTypeEnum.EXTERNAL);
        card.setRepresentativeType(PublisherTypeEnum.EXTERNAL);
        card.setRepresentative(currentUserWithPerimeters.getUserData().getLogin());
        Optional<CurrentUserWithPerimeters> optionalWrongUser = Optional.of(wrongUser);
        Assertions.assertThatThrownBy(() -> cardProcessingService.processCard(card, optionalWrongUser, token))
        .isInstanceOf(ApiErrorException.class).hasMessage("Card representative is set to dummyUser and account login is wrongUser, the card cannot be sent");
        Assertions.assertThat(checkCardCount(0)).isTrue();


        cardProcessingService.processCard(card, Optional.of(currentUserWithPerimeters), token);
        Assertions.assertThat(checkCardCount(1)).isTrue();
        String cardId = card.getId();
        Assertions.assertThatThrownBy(() -> cardProcessingService.deleteCard(cardId, optionalWrongUser, token))
            .isInstanceOf(ApiErrorException.class).hasMessage("Card representative is set to dummyUser and account login is wrongUser, the card cannot be deleted");
        Assertions.assertThat(checkCardCount(1)).isTrue();
       
        cardProcessingService.deleteCard(card.getId(), Optional.of(currentUserWithPerimeters), token);
        Assertions.assertThat(checkCardCount(0)).isTrue();
    }



    @Test
    void checkEntitiesAllowedToEdit() {

        CardPublicationData card = CardPublicationData.builder().publisher("entity2").processVersion("0")
        .processInstanceId("PROCESS_CARD_USER").severity(SeverityEnum.INFORMATION)
        .process("PROCESS_CARD_USER")
        .parentCardId(null)
        .initialParentCardUid(null)
        .state("STATE1")
        .title(I18nPublicationData.builder().key("title").build())
        .summary(I18nPublicationData.builder().key("summary").build())
        .startDate(Instant.now())
        .state("state1")
        .build();


        List<String> entitiesAllowedToEdit = new ArrayList();
        entitiesAllowedToEdit.add("entityallowed");

        card.setEntitiesAllowedToEdit(entitiesAllowedToEdit);

        cardProcessingService.processUserCard(card, currentUserWithPerimeters, token);
        Assertions.assertThat(checkCardCount(1)).isTrue();


        card.setUid(null);
        card.setPublisher("entityallowed");
        currentUserWithPerimeters.getUserData().setEntities(Arrays.asList("entityallowed"));

        Assertions.assertThatCode(() -> cardProcessingService.processUserCard(card, currentUserWithPerimeters, token))
        .doesNotThrowAnyException();

        card.setUid(null);
        card.setPublisher("notallowed");
        currentUserWithPerimeters.getUserData().setEntities(Arrays.asList("notallowed"));

        Assertions.assertThatThrownBy(() -> cardProcessingService.processUserCard(card, currentUserWithPerimeters, token))
            .isInstanceOf(ApiErrorException.class).hasMessage("User is not the sender of the original card or user is not part of entities allowed to edit card. Card is rejected");
    }

    @Test
    void checkEditChangingPublisher() {
        CardPublicationData card = CardPublicationData.builder().publisher("entity2").processVersion("0")
        .processInstanceId("PROCESS_CARD_USER_2").severity(SeverityEnum.INFORMATION)
        .process("PROCESS_CARD_USER")
        .parentCardId(null)
        .initialParentCardUid(null)
        .state("STATE1")
        .title(I18nPublicationData.builder().key("title").build())
        .summary(I18nPublicationData.builder().key("summary").build())
        .startDate(Instant.now())
        .state("state1")
        .build();


        List<String> entitiesAllowedToEdit = new ArrayList();
        entitiesAllowedToEdit.add("entityallowed");

        card.setEntitiesAllowedToEdit(entitiesAllowedToEdit);

        currentUserWithPerimeters.getUserData().setEntities(Arrays.asList("entity2", "newPublisherId"));

        cardProcessingService.processUserCard(card, currentUserWithPerimeters, token);
        Assertions.assertThat(checkCardCount(1)).isTrue();

        card.setUid(null);
        card.setPublisher("newPublisherId");

        cardProcessingService.processUserCard(card, currentUserWithPerimeters, token);
        Assertions.assertThat(checkCardCount(1)).isTrue();
    }

    @Test
    void doesProcessStateExistInBundles() {
        assertThat(cardProcessingService.doesProcessStateExistInBundles("api_test", "1", "messageState")).isTrue();
        assertThat(cardProcessingService.doesProcessStateExistInBundles("api_test", "1", "unexistingState")).isFalse();
        assertThat(cardProcessingService.doesProcessStateExistInBundles("api_test", "99", "messageState")).isFalse();
        assertThat(cardProcessingService.doesProcessStateExistInBundles("unexistingProcess", "1", "messageState")).isFalse();
        assertThat(cardProcessingService.doesProcessStateExistInBundles("processWithNoState", "1", "messageState")).isFalse();
    }

    @Nested
    class CardProcessServiceWithCheckPerimeterForCardSendingShould {

        @BeforeEach
        void setup() {
            cardProcessingService.checkPerimeterForCardSending = true;
        }

        @AfterEach
        void cleanup() {
            cardProcessingService.checkPerimeterForCardSending = false;
        }
        @Test
        void checkPerimeterForCardSending() {

            User testuser = new User();
            testuser.setLogin("dummyUser");

            CurrentUserWithPerimeters testCurrentUserWithPerimeters = new CurrentUserWithPerimeters();
            testCurrentUserWithPerimeters.setUserData(testuser);

            ComputedPerimeter c1 = new ComputedPerimeter();
            c1.setProcess("process1") ;
            c1.setState("state1");
            c1.setRights(RightsEnum.RECEIVE);


            ComputedPerimeter c2 = new ComputedPerimeter();
            c2.setProcess("process1") ;
            c2.setState("state1");
            c2.setRights(RightsEnum.RECEIVEANDWRITE);


            CardPublicationData card1 = CardPublicationData.builder().publisher("dummyUser").processVersion("0")
                    .processInstanceId("process1_1").severity(SeverityEnum.INFORMATION)
                    .process("process1")
                    .parentCardId(null)
                    .initialParentCardUid(null)
                    .state("state1")
                    .title(I18nPublicationData.builder().key("title").build())
                    .summary(I18nPublicationData.builder().key("summary").build())
                    .startDate(Instant.now())
                    .build();

            List<ComputedPerimeter> list=new ArrayList<>();
            list.add(c1);
            testCurrentUserWithPerimeters.setComputedPerimeters(list);
            Optional<CurrentUserWithPerimeters> user = Optional.of(testCurrentUserWithPerimeters);

            Assertions.assertThatThrownBy(() -> cardProcessingService.processCard(card1, user, token))
                    .isInstanceOf(AccessDeniedException.class).hasMessage("user not authorized, the card is rejected");

            list=new ArrayList<>();
            list.add(c2);
            testCurrentUserWithPerimeters.setComputedPerimeters(list);

            Assertions.assertThat(checkCardCount(0)).isTrue();
            cardProcessingService.processCard(card1, user, token);
            Assertions.assertThat(checkCardCount(1)).isTrue();
        }

    }

}

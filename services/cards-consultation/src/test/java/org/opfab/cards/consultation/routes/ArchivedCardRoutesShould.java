/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */



package org.opfab.cards.consultation.routes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opfab.cards.consultation.application.IntegrationTestApplication;
import org.opfab.cards.consultation.configuration.webflux.ArchivedCardRoutesConfig;
import org.opfab.cards.consultation.model.ArchivedCardConsultationData;
import org.opfab.cards.consultation.model.ArchivedCardData;
import org.opfab.cards.consultation.repositories.ArchivedCardRepository;
import org.opfab.springtools.configuration.test.WithMockOpFabUserReactive;
import org.opfab.test.EmptyListComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opfab.cards.consultation.TestUtilities.createSimpleArchivedCard;
import static org.opfab.cards.consultation.TestUtilities.roundingToMillis;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {IntegrationTestApplication.class, ArchivedCardRoutesConfig.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles(profiles = {"native", "test"})
@Tag("end-to-end")
@Tag("mongo")
class ArchivedCardRoutesShould {

    private static String publisher = "PUBLISHER";

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private RouterFunction<ServerResponse> archivedCardRoutes;
    @Autowired
    private ArchivedCardRepository repository;

    @AfterEach
    public void cleanArchivedCardRepository(){
        repository.deleteAll().subscribe();

    }

    @Nested
    @WithMockOpFabUserReactive(login="userWithGroup", groups = {"SOME_GROUP"})
    class GivenUserWithGroupArchivedCardRoutesShould {

        @Test
        void respondOkIfOptions() {
            assertThat(archivedCardRoutes).isNotNull();
            webTestClient.options().uri("/archives/id").exchange()
                    .expectStatus().isOk();
        }

        @Test
        void respondNotFound() {
            assertThat(archivedCardRoutes).isNotNull();
            webTestClient.get().uri("/archives/id").exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        void findArchivedCardById() {
            Instant now = roundingToMillis(Instant.now());
            ArchivedCardConsultationData simpleCard = createSimpleArchivedCard(1, publisher,now,now,now.plusSeconds(3600),"userWithGroup",null,null);
            StepVerifier.create(repository.save(simpleCard))
                    .expectNextCount(1)
                    .expectComplete()
                    .verify();
            assertThat(archivedCardRoutes).isNotNull();
            webTestClient.get().uri("/archives/{id}", simpleCard.getId()).exchange()
                    .expectStatus().isOk()
                    .expectBody(ArchivedCardData.class).value(res -> {
                assertThat(res.getCard())
                        .usingRecursiveComparison()
                        //This is necessary because empty lists are ignored in the returned JSON
                        .withComparatorForFields(new EmptyListComparator<String>(), "tags", "details", "userRecipients", "groupRecipients", "timeSpans")
                        .isEqualTo(simpleCard);
            });
        }
    }

    @Nested
    @WithMockOpFabUserReactive(login="userWithNoGroup", groups = {})
    class GivenUserWithNoGroupArchivedCardRoutesShould {

        @Test
        void findOutCard(){
            ArchivedCardConsultationData simpleCard = createSimpleArchivedCard(1, publisher, Instant.now(), Instant.now(), Instant.now().plusSeconds(3600));
            StepVerifier.create(repository.save(simpleCard))
                    .expectNextCount(1)
                    .expectComplete()
                    .verify();
            assertThat(archivedCardRoutes).isNotNull();
            webTestClient.get().uri("/archives/{id}",simpleCard.getId()).exchange()
                    .expectStatus().isNotFound()
            ;
        }

    }

    @Nested
    @WithMockOpFabUserReactive(login="userWithGroupAndEntity", groups={"SOME_GROUP"}, entities={"SOME_ENTITY"})
    class GivenUserWithGroupAndEntityArchivedCardRoutesShould {

        @Test
        void findArchivedCardById() {
        Instant now = roundingToMillis(Instant.now());

            ArchivedCardConsultationData simpleCard1 = createSimpleArchivedCard(1, publisher,now,
                   now,now.plusSeconds(3600), "",
                    new String[]{"OTHER_GROUP", "SOME_GROUP"}, new String[]{"OTHER_ENTITY", "SOME_ENTITY"});//must receive

            ArchivedCardConsultationData simpleCard2 = createSimpleArchivedCard(1, publisher,now,
                   now,now.plusSeconds(3600), "",
                    new String[]{"OTHER_GROUP", "SOME_GROUP"}, new String[]{"OTHER_ENTITY"});//must not receive

            ArchivedCardConsultationData simpleCard3 = createSimpleArchivedCard(1, publisher,now,
                   now,now.plusSeconds(3600), "",
                    new String[]{"OTHER_GROUP"}, new String[]{"OTHER_ENTITY", "SOME_ENTITY"});//must not receive

            ArchivedCardConsultationData simpleCard4 = createSimpleArchivedCard(1, publisher,now,
                   now,now.plusSeconds(3600), "",
                    new String[]{"OTHER_GROUP", "SOME_GROUP"}, null);//must receive

            ArchivedCardConsultationData simpleCard5 = createSimpleArchivedCard(1, publisher,now,
                   now,now.plusSeconds(3600), "",
                    null, new String[]{"OTHER_ENTITY", "SOME_ENTITY"});//must receive

            ArchivedCardConsultationData simpleCard6 = createSimpleArchivedCard(1, publisher,now,
                   now,now.plusSeconds(3600), "",
                    null, null);//must not receive

            StepVerifier.create(repository.save(simpleCard1))
                    .expectNextCount(1)
                    .expectComplete()
                    .verify();
            assertThat(archivedCardRoutes).isNotNull();
            webTestClient.get().uri("/archives/{id}", simpleCard1.getId()).exchange()
                    .expectStatus().isOk()
                    .expectBody(ArchivedCardData.class).value(res -> {
                assertThat(res.getCard())
                        .usingRecursiveComparison()
                        //This is necessary because empty lists are ignored in the returned JSON
                        .withComparatorForFields(new EmptyListComparator<String>(), "tags", "details", "userRecipients", "groupRecipients", "timeSpans")
                        .isEqualTo(simpleCard1);
            });

            StepVerifier.create(repository.save(simpleCard2))
                    .expectNextCount(1)
                    .expectComplete()
                    .verify();
            assertThat(archivedCardRoutes).isNotNull();
            webTestClient.get().uri("/archives/{id}", simpleCard2.getId()).exchange()
                    .expectStatus().isNotFound();

            StepVerifier.create(repository.save(simpleCard3))
                    .expectNextCount(1)
                    .expectComplete()
                    .verify();
            assertThat(archivedCardRoutes).isNotNull();
            webTestClient.get().uri("/archives/{id}", simpleCard3.getId()).exchange()
                    .expectStatus().isNotFound();

            StepVerifier.create(repository.save(simpleCard4))
                    .expectNextCount(1)
                    .expectComplete()
                    .verify();
            assertThat(archivedCardRoutes).isNotNull();
            webTestClient.get().uri("/archives/{id}", simpleCard4.getId()).exchange()
                    .expectStatus().isOk()
                    .expectBody(ArchivedCardData.class).value(res -> {
                assertThat(res.getCard())
                        .usingRecursiveComparison()
                        //This is necessary because empty lists are ignored in the returned JSON
                        .withComparatorForFields(new EmptyListComparator<String>(), "tags", "details", "userRecipients", "groupRecipients", "timeSpans")
                        .isEqualTo(simpleCard4);
            });

            StepVerifier.create(repository.save(simpleCard5))
                    .expectNextCount(1)
                    .expectComplete()
                    .verify();
            assertThat(archivedCardRoutes).isNotNull();
            webTestClient.get().uri("/archives/{id}", simpleCard5.getId()).exchange()
                    .expectStatus().isOk()
                    .expectBody(ArchivedCardData.class).value(res -> {
                assertThat(res.getCard())
                        .usingRecursiveComparison()
                        //This is necessary because empty lists are ignored in the returned JSON
                        .withComparatorForFields(new EmptyListComparator<String>(), "tags", "details", "userRecipients", "groupRecipients", "timeSpans")
                        .isEqualTo(simpleCard5);
            });

            StepVerifier.create(repository.save(simpleCard6))
                    .expectNextCount(1)
                    .expectComplete()
                    .verify();
            assertThat(archivedCardRoutes).isNotNull();
            webTestClient.get().uri("/archives/{id}", simpleCard6.getId()).exchange()
                    .expectStatus().isNotFound();
        }
    }
}

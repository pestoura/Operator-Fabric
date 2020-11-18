package org.lfenergy.operatorfabric.cards.publication.kafka.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lfenergy.operatorfabric.avro.Card;
import org.lfenergy.operatorfabric.avro.CardCommand;
import org.lfenergy.operatorfabric.cards.publication.kafka.CardObjectMapper;
import org.lfenergy.operatorfabric.cards.publication.model.CardPublicationData;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = {"native", "test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BaseCommandHandlerTest {

    private final String ANY_STRING = "any_string";
    private final String PROCESS = "a_process";
    private final String PROCESS_INSTANCE_ID = "a_process_instance_id";
    private final String DATA_KEY = "key";
    private final String DATA_VALUE = "value";

    private CardObjectMapper objectMapper;

    private BaseCommandHandler cut;

    private CardCommand cardCommand;

    private Card card;

    @BeforeAll
    void setUp() throws JsonProcessingException {
        objectMapper = mock(CardObjectMapper.class);
        cut = new BaseCommandHandler();
        ReflectionTestUtils.setField(cut, "objectMapper", objectMapper);
        CardPublicationData cardPublicationData = CardPublicationData.builder()
                .build();
        cardCommand = mock(CardCommand.class);
        card = mock(Card.class);
        when(cardCommand.getCard()).thenReturn(card);
        when(cardCommand.getProcess()).thenReturn(PROCESS);
        when(cardCommand.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
        when(objectMapper.writeValueAsString(card)).thenReturn(ANY_STRING);
        when(objectMapper.readValue(ANY_STRING, CardPublicationData.class)).thenReturn(cardPublicationData);
    }

    @Test
    void testBuildCardPublicationData_withoutDataProperty() throws JsonProcessingException {
        String cardDataAsString = null;
        when(card.getData()).thenReturn(cardDataAsString);
        CardPublicationData result = cut.buildCardPublicationData(cardCommand);
        Map<String,Object> data = (Map<String,Object>) result.getData();
        Assertions.assertThat(data.size()).isEqualTo(0);
        Assertions.assertThat(result.getProcess()).isEqualTo(PROCESS);
        Assertions.assertThat(result.getProcessInstanceId()).isEqualTo(PROCESS_INSTANCE_ID);
    }

    @Test
    void testBuildCardPublicationData_withDataProperty() throws JsonProcessingException {
        String cardDataAsString = "justAString";
        Map<String, Object> cardData = Collections.singletonMap(DATA_KEY, DATA_VALUE);
        when(card.getData()).thenReturn(cardDataAsString);
        when(objectMapper.readValue(anyString(), (TypeReference<Map<String, Object>>) any())).thenReturn(cardData);
        CardPublicationData result = cut.buildCardPublicationData(cardCommand);
        Map<String,Object> data = (Map<String,Object>) result.getData();
        Assertions.assertThat(data.size()).isEqualTo(1);
        Assertions.assertThat(data.get(DATA_KEY)).isEqualTo(DATA_VALUE);
        Assertions.assertThat(result.getProcess()).isEqualTo(PROCESS);
        Assertions.assertThat(result.getProcessInstanceId()).isEqualTo(PROCESS_INSTANCE_ID);
    }

}
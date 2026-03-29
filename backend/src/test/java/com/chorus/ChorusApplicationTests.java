package com.chorus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ChorusApplicationTests {

    @Autowired
    private ChorusApplication application;

    @MockBean
    @SuppressWarnings("unused")
    private ChatModel chatModel;

    @Test
    void contextLoads() {
        assertThat(application).isNotNull();
    }
}

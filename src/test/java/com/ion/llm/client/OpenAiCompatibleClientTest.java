package com.ion.llm.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleClientTest {

    @Test
    void extractDataSupportsRawSseDataLine() {
        String json = "{\"choices\":[{\"delta\":{\"content\":\"연결\"}}]}";

        assertThat(OpenAiCompatibleClient.extractData("data: " + json)).isEqualTo(json);
    }

    @Test
    void extractDataSupportsDecodedSsePayload() {
        String json = "{\"choices\":[{\"delta\":{\"content\":\" 정상\"}}]}";

        assertThat(OpenAiCompatibleClient.extractData(json)).isEqualTo(json);
    }

    @Test
    void extractDataIgnoresBlankLines() {
        assertThat(OpenAiCompatibleClient.extractData("   ")).isNull();
    }
}

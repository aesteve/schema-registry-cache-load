package com.github.aesteve.kafka;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.generic.GenericData;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UseSRClientOutsideSerializerTest extends TestCommon {

    private final static String TOPIC = "topic_outside_serializer";

    @Test
    void withoutPreloadingFirstRegisterMethodGetCalled() throws Exception {
        var client = new MockSchemaRegistrySpy();
        Map<String, String> config = new HashMap<>();
        try (var serializer = new KafkaAvroSerializer(client)) {
            config.put("schema.registry.url", "mock://something");
            config.put("auto.register.schemas", "true");
            serializer.configure(config, false);
            var record = new GenericData.Record(SCHEMA);
            record.put("some_field", "some_value");
            serializer.serialize(TOPIC, record);
            assertEquals(1, client.callsToSR.intValue());

            // Serialize a second record: register should not get called this time (cache is filled)
            serializer.serialize(TOPIC, record);
            assertEquals(1, client.callsToSR.intValue());
        }
    }

    @Test
    void whenPreloadingTheSchemaNoCallIsMade() throws Exception {
        var client = new MockSchemaRegistrySpy();
        Map<String, String> config = new HashMap<>();
        assertTrue(client.schemaCache.isEmpty());
        try (var serializer = new KafkaAvroSerializer(client)) {
            config.put("schema.registry.url", "mock://something");
            config.put("auto.register.schemas", "true");
            serializer.configure(config, false);

            client.register(TOPIC + "-value", PARSED_SCHEMA); // this is where it differs from `PreloadSchemaInSerializerTest`
            assertEquals(1, client.callsToSR.intValue()); // this issued the call to SR
            assertEquals(1, client.schemaCache.size());

            var record = new GenericData.Record(SCHEMA);
            record.put("some_field", "some_value");
            serializer.serialize(TOPIC, record);
            assertEquals(1, client.callsToSR.intValue()); // but now, cache is filled, and no additional call is made
        }
    }

}

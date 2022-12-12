package com.github.aesteve.kafka;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.utils.QualifiedSubject;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;

public class PreloadSchemasInSerializerTest extends TestCommon{

    private final static String TOPIC = "topic_preloaded";

    @Test
    void withoutRegisteringFirstRegisterMethodGetCalled() throws Exception {
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
    void whenRegisteringManuallyTheSchemaNoCallIsMade() throws Exception {
        var client = new MockSchemaRegistrySpy();
        Map<String, String> config = new HashMap<>();
        assertTrue(client.schemaCache.isEmpty());
        try (var serializer = new KafkaAvroSerializer(client)) {
            config.put("schema.registry.url", "mock://something");
            config.put("auto.register.schemas", "true");
            serializer.configure(config, false);

            serializer.register(QualifiedSubject TOPIC + "-value", PARSED_SCHEMA); // register BEFORE serializing
            assertEquals(1, client.callsToSR.intValue()); // this issued the call to SR
            assertEquals(1, client.schemaCache.size());


            var record = new GenericData.Record(SCHEMA);
            record.put("some_field", "some_value");
            serializer.serialize(TOPIC, record);
            assertEquals(1, client.callsToSR.intValue()); // but now, cache is filled, and no additional call is made
        }
    }

}

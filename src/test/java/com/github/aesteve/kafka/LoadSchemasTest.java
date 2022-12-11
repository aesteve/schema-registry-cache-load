package com.github.aesteve.kafka;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;

public class LoadSchemasTest {

    private final static String SCHEMA_DEFINITION = """
            {
                "type": "record",
                "name": "some_record",
                "fields": [{
                    "name": "some_field",
                    "type": "string"
                }]
            }
            """;

    private final static Schema SCHEMA = new Schema.Parser().parse(SCHEMA_DEFINITION);
    private final static ParsedSchema PARSED_SCHEMA = new AvroSchema(SCHEMA);


    private final static String TOPIC = "some_topic";

    @Test
    void withoutPreloadingFirstRegisterMethodGetCalled() throws Exception {
//        var client = PowerMockito.spy(new MockSchemaRegistryClient());
        var client = new MockSchemaRegistrySpy();
        Map<String, String> config = new HashMap<>();
        try (var serializer = new KafkaAvroSerializerPreload(client)) {
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
//        var client = PowerMockito.spy(new MockSchemaRegistryClient());
        var client = new MockSchemaRegistrySpy();
        Map<String, String> config = new HashMap<>();
        assertTrue(client.schemaCache.isEmpty());
        try (var serializer = new KafkaAvroSerializerPreload(client)) {
            config.put("schema.registry.url", "mock://something");
            config.put("auto.register.schemas", "true");
            serializer.configure(config, false);

            serializer.register(PARSED_SCHEMA, TOPIC + "-value");
            assertEquals(1, client.callsToSR.intValue()); // this issued the call to SR
            assertEquals(1, client.schemaCache.size());


            var record = new GenericData.Record(SCHEMA);
            record.put("some_field", "some_value");
            serializer.serialize(TOPIC, record);
            assertEquals(1, client.callsToSR.intValue()); // but now, cache is filled, and no additional call is made
        }
    }

}

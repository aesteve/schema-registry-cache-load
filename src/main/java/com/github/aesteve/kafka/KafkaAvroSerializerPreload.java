package com.github.aesteve.kafka;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

import java.io.IOException;
import java.util.Map;

public class KafkaAvroSerializerPreload extends KafkaAvroSerializer {

    public KafkaAvroSerializerPreload() {
        super();
    }

    public KafkaAvroSerializerPreload(SchemaRegistryClient client) {
        super(client);
    }

    public KafkaAvroSerializerPreload(SchemaRegistryClient client, Map<String, ?> props) {
        super(client, props);
    }

    /**
     * Registers a schema for a subject in the registry;
     * Can be used before any calls to serialize has been made
     * @param schema the schema to register
     * @param subject the subject. Careful that it's not necessarily the topic name depending on the Naming Strategy
     * @throws RestClientException
     * @throws IOException
     */
    public void register(ParsedSchema schema, String subject) throws RestClientException, IOException {
        schemaRegistry.register(subject, schema);
    }

}

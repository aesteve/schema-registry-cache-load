package com.github.aesteve.kafka;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import org.apache.avro.Schema;

public abstract class TestCommon {

    protected final static String SCHEMA_DEFINITION = """
            {
                "type": "record",
                "name": "some_record",
                "fields": [{
                    "name": "some_field",
                    "type": "string"
                }]
            }
            """;

    protected final static Schema SCHEMA = new Schema.Parser().parse(SCHEMA_DEFINITION);
    protected final static ParsedSchema PARSED_SCHEMA = new AvroSchema(SCHEMA);



}

Shows an example on how to extend `KafkaAvroSerializer` to access the underlying `SchemaRegistry` client and use it to register schemas in advance.



Note that although the implementation is quite simple, it is quite tedious to test (since we need to access private fields in `MockSchemaRegistryClient`), making it protected or package private at least could be worth considering. 
## Problem statement

The Schema Registry aware serializers are working in a lazy way: no REST call to SR is issued before the first record we need to serialize actually has to register its schema. When registered, the cache is fulfilled, and future serializations won't produce a REST call to the Schema Registry.
This means the REST call to SR actually happens when `producer.send(...)` is called for the first time for a (Schema, Subject) pair, meaning the cache is actually empty for this pair.

This is the best implementation for a massive majority of scenarios in which the SR-aware serializers are used.

Unfortunately, this can lead to some issues on some cases:
* one is very well described [here](https://github.com/confluentinc/confluent-kafka-python/issues/913) (in Python context)
    * few messages sent by an (highly concurrent) application restarting quite often
    * if `auto.register.schemas` is set to `false`, the error is detected when the first record gets produced.
* another one is the "standby replica" use-case
  * when an instance has to be ready to accept (massive) traffic very suddenly (because it is acting as a standby replica for disaster recovery for example), having that REST call in the critical path (the first "cold call") can lead to issues
    * degradation of response time / SLAs
    * potentially timeouts in situation where users need to avoid those (DRP) (snowball effects)
    * if there's a massive traffic re-routing (actual disaster recovery) this can lead to an API quota exhaustion (N instances of a web application all acting as standby and accepting traffic at the same time -> rate-limiting)

In these cases, it is better to actually communicate with Schema Registry before calling `producer.send(...)`, for example in a readiness probe.

## Implementation notes

### Different Alternatives

#### Access SR from Serializer
Use the `register` method available in any SR-aware serializer implementation (i.e. `AbstractKafkaSchemaSerDe::register`)
Can be found [here](src/test/java/com/github/aesteve/kafka/PreloadSchemasInSerializerTest.java)
Note that this requires to instantiate the producer **manually** and not via reflection, as done when using `props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ...)`

#### Use SR client outside the serializer
`register` can also be called from a `SchemaRegistryClient` instance. And since a schema registry client can be passed to the serializer constructor, this is another way of achieving the same thing.
This can be found [here](src/test/java/com/github/aesteve/kafka/UseSRClientOutsideSerializerTest.java)

#### Plug-ins mechanism for SR cache (persistent cache)

In some use-cases mentioned above (standby replica, huge number of single-producer application instances), using a shared+persisted cache could actually be quite useful.
This would lead to less REST API calls to Schema Registry, at the expanse of maintaining (and invalidating...) a local cache (which can be tricky in case Schemas are deleted).
This one is potentially interesting to investigate a bit further. Would it make sense as a whole? What would the design of such a plugin mechanism be?

### Why this repo?

The goal is mainly to discuss the relevance of exposing methods from the SR client like the existing `register` one or some new ones like `loadSchemasForSubject` for making it more straghtforward for users of the serializers that `producer.send(...)` can actually perform several things in a lazy way, and prevent it.
It's mostly an API design discussion around the form it should take: 
* a blog post or "how-to" dedicated to answering the questions in the problem statement part of this doc
* or a proper change to the SR-aware serializers contract, adding more method signatures related to managing cache


## Side Notes

A few notes taken while writing the different implementations, testing

Although the implementations are quite simple to expose it is quite tedious to test these (since we need to access private fields in `MockSchemaRegistryClient`), making such fields protected or package private at least could be worth considering.
Also, having a way to observe the "REST calls that would have been issued to the actual SR" in `MockSchemaRegistryClient` (as done with the [`nbCallsToSR`](src/test/java/com/github/aesteve/kafka/MockSchemaRegistrySpy.java) counter) could be cool.
(to check cache effectiveness, to avoid regression in client codebases, even if custom, etc.)
plugins {
    id("java")
}

group = "com.github.aesteve.kafka"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
}

object Versions {
    const val confluentPlatform = "7.3.0"
    const val jupiter = "5.9.0"
}


dependencies {
    implementation("io.confluent:kafka-avro-serializer:${Versions.confluentPlatform}")


    testImplementation("org.powermock:powermock-api-mockito2:2.0.9")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    jvmArgs = listOf("--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED")
}

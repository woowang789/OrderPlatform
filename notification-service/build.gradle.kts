plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":common"))

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Test
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

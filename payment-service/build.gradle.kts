plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":common"))

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Test
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter") {
        exclude(group = "org.testcontainers", module = "testcontainers")
    }
    testImplementation("org.testcontainers:postgresql") {
        exclude(group = "org.testcontainers", module = "testcontainers")
    }
    testImplementation("org.testcontainers:testcontainers:2.0.3")
}

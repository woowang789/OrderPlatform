plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":common"))

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")
}

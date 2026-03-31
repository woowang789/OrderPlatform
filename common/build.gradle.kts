plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

tasks.named<Jar>("jar") {
    enabled = true
}

dependencies {
    // Spring Boot Starters
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-web")

    // Spring Security (CurrentMemberIdArgumentResolver에서 사용)
    api("org.springframework.boot:spring-boot-starter-security")

    // Kafka (이벤트 드리븐 통신)
    api("org.springframework.kafka:spring-kafka")

    // API 문서 (SpringDoc OpenAPI)
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
}

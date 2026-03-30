plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val springCloudVersion: String by rootProject.extra

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
}

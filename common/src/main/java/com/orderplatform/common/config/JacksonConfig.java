package com.orderplatform.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper 공통 설정.
 * 이벤트 스키마 Forward Compatibility를 위해 미지 필드를 무시한다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Forward Compatibility — 알 수 없는 필드 무시 (새 Optional 필드 추가 시 기존 Consumer 호환)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 알 수 없는 Enum 값을 null로 처리
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

        // Java 8 날짜/시간 지원 (LocalDateTime, Instant 등)
        mapper.registerModule(new JavaTimeModule());

        // 날짜를 ISO-8601 문자열로 직렬화 (타임스탬프 숫자 비활성화)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}

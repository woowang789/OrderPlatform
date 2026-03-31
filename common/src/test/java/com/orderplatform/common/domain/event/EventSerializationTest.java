package com.orderplatform.common.domain.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderplatform.common.domain.event.payload.OrderItemPayload;
import com.orderplatform.common.domain.event.payload.StockFailureItemPayload;
import com.orderplatform.common.domain.event.payload.StockItemPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * 9개 이벤트의 Jackson 직렬화/역직렬화 단위 테스트.
 * Spring Context 없이 순수 ObjectMapper로 실행하여 빠른 피드백을 확보한다.
 */
class EventSerializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ──────────────────────────────────────────────
    // 주문 이벤트
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("OrderPlacedEvent 직렬화/역직렬화")
    class OrderPlacedEventTest {

        @Test
        @DisplayName("직렬화 후 역직렬화하면 원본과 동일하다")
        void serializeAndDeserialize() throws JsonProcessingException {
            var items = List.of(
                    new OrderItemPayload(1L, "상품A", 10000L, 2),
                    new OrderItemPayload(2L, "상품B", 5000L, 1)
            );
            var event = new OrderPlacedEvent(UUID.randomUUID(), 1L, items, 25000L, "CARD");

            String json = mapper.writeValueAsString(event);
            var deserialized = mapper.readValue(json, OrderPlacedEvent.class);

            assertThat(deserialized).isEqualTo(event);
            assertThat(deserialized.orderId()).isEqualTo(event.orderId());
            assertThat(deserialized.aggregateId()).isEqualTo(event.orderId());
        }

        @Test
        @DisplayName("미지 필드가 포함된 JSON도 오류 없이 역직렬화된다 (Forward Compatibility)")
        void unknownFieldIgnored() throws JsonProcessingException {
            var event = new OrderPlacedEvent(UUID.randomUUID(), 1L, List.of(), 0L, "CARD");
            String json = mapper.writeValueAsString(event);
            // 미지 필드 추가
            String jsonWithUnknown = json.substring(0, json.length() - 1) + ",\"newField\":\"value\"}";

            assertThatNoException().isThrownBy(() ->
                    mapper.readValue(jsonWithUnknown, OrderPlacedEvent.class));
        }

        @Test
        @DisplayName("빈 items 리스트도 정상 처리된다")
        void emptyItemsList() throws JsonProcessingException {
            var event = new OrderPlacedEvent(UUID.randomUUID(), 1L, List.of(), 0L, "CARD");

            String json = mapper.writeValueAsString(event);
            var deserialized = mapper.readValue(json, OrderPlacedEvent.class);

            assertThat(deserialized.items()).isEmpty();
        }

        @Test
        @DisplayName("LocalDateTime이 ISO-8601 문자열로 직렬화된다")
        void dateTimeFormat() throws JsonProcessingException {
            var event = new OrderPlacedEvent(UUID.randomUUID(), 1L, List.of(), 0L, "CARD");

            String json = mapper.writeValueAsString(event);

            // ISO-8601 형식 확인 (예: "2026-03-31T12:00:00")
            assertThat(json).containsPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        }
    }

    @Nested
    @DisplayName("OrderConfirmedEvent 직렬화/역직렬화")
    class OrderConfirmedEventTest {

        @Test
        @DisplayName("직렬화 후 역직렬화하면 원본과 동일하다")
        void serializeAndDeserialize() throws JsonProcessingException {
            var event = new OrderConfirmedEvent(UUID.randomUUID());

            String json = mapper.writeValueAsString(event);
            var deserialized = mapper.readValue(json, OrderConfirmedEvent.class);

            assertThat(deserialized).isEqualTo(event);
        }
    }

    @Nested
    @DisplayName("OrderCancelledEvent 직렬화/역직렬화")
    class OrderCancelledEventTest {

        @Test
        @DisplayName("직렬화 후 역직렬화하면 원본과 동일하다")
        void serializeAndDeserialize() throws JsonProcessingException {
            var event = new OrderCancelledEvent(UUID.randomUUID(), "재고 부족");

            String json = mapper.writeValueAsString(event);
            var deserialized = mapper.readValue(json, OrderCancelledEvent.class);

            assertThat(deserialized).isEqualTo(event);
            assertThat(deserialized.reason()).isEqualTo("재고 부족");
        }

        @Test
        @DisplayName("reason이 null이어도 정상 처리된다")
        void nullReason() throws JsonProcessingException {
            var event = new OrderCancelledEvent(UUID.randomUUID(), null);

            String json = mapper.writeValueAsString(event);
            var deserialized = mapper.readValue(json, OrderCancelledEvent.class);

            assertThat(deserialized.reason()).isNull();
        }
    }

    // ──────────────────────────────────────────────
    // 결제 이벤트
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("PaymentCompletedEvent 직렬화/역직렬화")
    class PaymentCompletedEventTest {

        @Test
        @DisplayName("직렬화 후 역직렬화하면 원본과 동일하다")
        void serializeAndDeserialize() throws JsonProcessingException {
            var items = List.of(
                    new StockItemPayload(1L, 2),
                    new StockItemPayload(2L, 1)
            );
            var event = new PaymentCompletedEvent(UUID.randomUUID(), UUID.randomUUID(), 1L, 25000L, items);

            String json = mapper.writeValueAsString(event);
            var deserialized = mapper.readValue(json, PaymentCompletedEvent.class);

            assertThat(deserialized).isEqualTo(event);
            assertThat(deserialized.aggregateId()).isEqualTo(event.paymentId());
            assertThat(deserialized.orderId()).isEqualTo(event.orderId());
        }
    }

    @Nested
    @DisplayName("PaymentFailedEvent 직렬화/역직렬화")
    class PaymentFailedEventTest {

        @Test
        @DisplayName("직렬화 후 역직렬화하면 원본과 동일하다")
        void serializeAndDeserialize() throws JsonProcessingException {
            var event = new PaymentFailedEvent(UUID.randomUUID(), UUID.randomUUID(), "잔액 부족");

            String json = mapper.writeValueAsString(event);
            var deserialized = mapper.readValue(json, PaymentFailedEvent.class);

            assertThat(deserialized).isEqualTo(event);
            assertThat(deserialized.reason()).isEqualTo("잔액 부족");
        }
    }

    @Nested
    @DisplayName("PaymentCancelRequestedEvent 직렬화/역직렬화")
    class PaymentCancelRequestedEventTest {

        @Test
        @DisplayName("직렬화 후 역직렬화하면 원본과 동일하다")
        void serializeAndDeserialize() throws JsonProcessingException {
            var event = new PaymentCancelRequestedEvent(UUID.randomUUID(), UUID.randomUUID(), "재고 차감 실패");

            String json = mapper.writeValueAsString(event);
            var deserialized = mapper.readValue(json, PaymentCancelRequestedEvent.class);

            assertThat(deserialized).isEqualTo(event);
        }
    }

    @Nested
    @DisplayName("PaymentCancelledEvent 직렬화/역직렬화")
    class PaymentCancelledEventTest {

        @Test
        @DisplayName("직렬화 후 역직렬화하면 원본과 동일하다")
        void serializeAndDeserialize() throws JsonProcessingException {
            var event = new PaymentCancelledEvent(UUID.randomUUID(), UUID.randomUUID());

            String json = mapper.writeValueAsString(event);
            var deserialized = mapper.readValue(json, PaymentCancelledEvent.class);

            assertThat(deserialized).isEqualTo(event);
        }
    }

    // ──────────────────────────────────────────────
    // 재고 이벤트
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("StockDeductedEvent 직렬화/역직렬화")
    class StockDeductedEventTest {

        @Test
        @DisplayName("직렬화 후 역직렬화하면 원본과 동일하다")
        void serializeAndDeserialize() throws JsonProcessingException {
            var items = List.of(
                    new StockItemPayload(1L, 2),
                    new StockItemPayload(2L, 3)
            );
            var event = new StockDeductedEvent(UUID.randomUUID(), items);

            String json = mapper.writeValueAsString(event);
            var deserialized = mapper.readValue(json, StockDeductedEvent.class);

            assertThat(deserialized).isEqualTo(event);
            assertThat(deserialized.items()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("StockDeductionFailedEvent 직렬화/역직렬화")
    class StockDeductionFailedEventTest {

        @Test
        @DisplayName("직렬화 후 역직렬화하면 원본과 동일하다")
        void serializeAndDeserialize() throws JsonProcessingException {
            var items = List.of(
                    new StockFailureItemPayload(1L, 10, 3),
                    new StockFailureItemPayload(2L, 5, 0)
            );
            var event = new StockDeductionFailedEvent(UUID.randomUUID(), items, "재고 부족");

            String json = mapper.writeValueAsString(event);
            var deserialized = mapper.readValue(json, StockDeductionFailedEvent.class);

            assertThat(deserialized).isEqualTo(event);
            assertThat(deserialized.items()).hasSize(2);
            assertThat(deserialized.reason()).isEqualTo("재고 부족");
        }

        @Test
        @DisplayName("미지 필드가 포함된 JSON도 오류 없이 역직렬화된다 (Forward Compatibility)")
        void unknownFieldIgnored() throws JsonProcessingException {
            var event = new StockDeductionFailedEvent(UUID.randomUUID(), List.of(), "재고 부족");
            String json = mapper.writeValueAsString(event);
            String jsonWithUnknown = json.substring(0, json.length() - 1) + ",\"futureField\":123}";

            assertThatNoException().isThrownBy(() ->
                    mapper.readValue(jsonWithUnknown, StockDeductionFailedEvent.class));
        }
    }
}

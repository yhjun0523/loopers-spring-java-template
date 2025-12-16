package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.config.IntegrationTestBase;
import com.loopers.domain.event.order.OrderCompletedEvent;
import com.loopers.domain.eventhandled.EventHandledRepository;
import com.loopers.domain.productmetrics.ProductMetrics;
import com.loopers.domain.productmetrics.ProductMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OrderEventConsumerTest extends IntegrationTestBase {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ProductMetricsRepository productMetricsRepository;

    @Autowired
    private EventHandledRepository eventHandledRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ORDER_TOPIC = "order-events";
    private static final Long TEST_ORDER_ID = 100L;
    private static final String TEST_USER_ID = "user456";
    private static final Long PRODUCT_A_ID = 1L;
    private static final Long PRODUCT_B_ID = 2L;

    @BeforeEach
    void setup() {
        productMetricsRepository.deleteAll(); // Clean up before each test
        eventHandledRepository.deleteAll(); // Clean up before each test
        redisTemplate.delete(redisTemplate.keys("product:detail:*")); // Clean up Redis cache
    }

    @Test
    @DisplayName("새로운 주문 완료 이벤트가 metrics에 정상 반영되고 멱등 처리된다")
    void testConsume_NewOrderCompletedEvent_ShouldUpdateMetricsAndBeIdempotent() throws JsonProcessingException {
        // Given
        String eventId = UUID.randomUUID().toString();
        OrderCompletedEvent.OrderItemDto itemA = new OrderCompletedEvent.OrderItemDto(
            PRODUCT_A_ID, "Product A", BigDecimal.valueOf(1000), 2);
        OrderCompletedEvent.OrderItemDto itemB = new OrderCompletedEvent.OrderItemDto(
            PRODUCT_B_ID, "Product B", BigDecimal.valueOf(2000), 1);
        OrderCompletedEvent event = OrderCompletedEvent.of(
            TEST_ORDER_ID, TEST_USER_ID, List.of(itemA, itemB), BigDecimal.valueOf(4000), BigDecimal.valueOf(4000));
        String eventPayload = objectMapper.writeValueAsString(event);

        // Populate some dummy cache for product A and B
        redisTemplate.opsForValue().set("product:detail:" + PRODUCT_A_ID + ":user001", "dummy_data_A");
        redisTemplate.opsForValue().set("product:detail:" + PRODUCT_B_ID + ":user002", "dummy_data_B");
        assertThat(redisTemplate.keys("product:detail:" + PRODUCT_A_ID + ":*")).isNotEmpty();
        assertThat(redisTemplate.keys("product:detail:" + PRODUCT_B_ID + ":*")).isNotEmpty();

        // When - Send the same event twice
        kafkaTemplate.send(ORDER_TOPIC, event.getAggregateId(), eventPayload);
        kafkaTemplate.send(ORDER_TOPIC, event.getAggregateId(), eventPayload);

        // Then - Verify that metrics are updated only once and cache is evicted
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<ProductMetrics> metricsAOpt = productMetricsRepository.findByProductId(PRODUCT_A_ID);
            assertThat(metricsAOpt).isPresent();
            assertThat(metricsAOpt.get().getSalesCount()).isEqualTo(2);

            Optional<ProductMetrics> metricsBOpt = productMetricsRepository.findByProductId(PRODUCT_B_ID);
            assertThat(metricsBOpt).isPresent();
            assertThat(metricsBOpt.get().getSalesCount()).isEqualTo(1);

            assertThat(eventHandledRepository.existsByEventId(eventId)).isTrue();

            // Verify cache eviction
            assertThat(redisTemplate.keys("product:detail:" + PRODUCT_A_ID + ":*")).isEmpty();
            assertThat(redisTemplate.keys("product:detail:" + PRODUCT_B_ID + ":*")).isEmpty();
        });
    }

    @Test
    @DisplayName("오래된 주문 완료 이벤트는 스킵되고 metrics에 반영되지 않으며 캐시도 무효화되지 않는다")
    void testConsume_OutdatedOrderCompletedEvent_ShouldBeSkipped() throws JsonProcessingException, InterruptedException {
        // Given - Send a newer event first
        ZonedDateTime newerTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        String newerEventId = UUID.randomUUID().toString();
        OrderCompletedEvent.OrderItemDto itemA = new OrderCompletedEvent.OrderItemDto(
            PRODUCT_A_ID, "Product A", BigDecimal.valueOf(1000), 2);
        OrderCompletedEvent newerEvent = OrderCompletedEvent.of(
            TEST_ORDER_ID, TEST_USER_ID, List.of(itemA), BigDecimal.valueOf(2000), BigDecimal.valueOf(2000)
        );
        newerEvent = new OrderCompletedEvent(
            newerEventId, newerEvent.getEventType(), newerEvent.orderId(), newerEvent.userId(),
            newerEvent.orderItems(), newerEvent.totalAmount(), newerEvent.finalAmount(), newerTime
        ); // Override occurredAt
        String newerEventPayload = objectMapper.writeValueAsString(newerEvent);
        kafkaTemplate.send(ORDER_TOPIC, newerEvent.getAggregateId(), newerEventPayload);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<ProductMetrics> metricsAOpt = productMetricsRepository.findByProductId(PRODUCT_A_ID);
            assertThat(metricsAOpt).isPresent();
            assertThat(metricsAOpt.get().getSalesCount()).isEqualTo(2);
            assertThat(eventHandledRepository.existsByEventId(newerEventId)).isTrue();
        });

        // Add some dummy cache for product A
        redisTemplate.opsForValue().set("product:detail:" + PRODUCT_A_ID + ":user001", "dummy_data_A");
        assertThat(redisTemplate.keys("product:detail:" + PRODUCT_A_ID + ":*")).isNotEmpty();

        // Add a small delay
        TimeUnit.MILLISECONDS.sleep(100);

        // When - Send an older event
        ZonedDateTime olderTime = newerTime.minusMinutes(5);
        String olderEventId = UUID.randomUUID().toString();
        OrderCompletedEvent olderEvent = OrderCompletedEvent.of(
            TEST_ORDER_ID, TEST_USER_ID, List.of(itemA), BigDecimal.valueOf(2000), BigDecimal.valueOf(2000)
        );
        olderEvent = new OrderCompletedEvent(
            olderEventId, olderEvent.getEventType(), olderEvent.orderId(), olderEvent.userId(),
            olderEvent.orderItems(), olderEvent.totalAmount(), olderEvent.finalAmount(), olderTime
        ); // Override occurredAt
        String olderEventPayload = objectMapper.writeValueAsString(olderEvent);
        kafkaTemplate.send(ORDER_TOPIC, olderEvent.getAggregateId(), olderEventPayload);

        // Then - Verify that the metrics remain unchanged and cache is not evicted
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<ProductMetrics> metricsAOpt = productMetricsRepository.findByProductId(PRODUCT_A_ID);
            assertThat(metricsAOpt).isPresent();
            assertThat(metricsAOpt.get().getSalesCount()).isEqualTo(2); // Still 2, not 4
            assertThat(eventHandledRepository.existsByEventId(olderEventId)).isTrue(); // Older event still handled

            // Cache should NOT be evicted by the older event
            assertThat(redisTemplate.keys("product:detail:" + PRODUCT_A_ID + ":*")).isNotEmpty();
        });
    }
}

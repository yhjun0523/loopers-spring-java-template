package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.config.IntegrationTestBase;
import com.loopers.domain.event.catalog.ProductLikeToggledEvent;
import com.loopers.domain.eventhandled.EventHandledRepository;
import com.loopers.domain.productmetrics.ProductMetrics;
import com.loopers.domain.productmetrics.ProductMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class CatalogEventConsumerTest extends IntegrationTestBase {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ProductMetricsRepository productMetricsRepository;

    @Autowired
    private EventHandledRepository eventHandledRepository;

    private static final String CATALOG_TOPIC = "catalog-events";
    private static final Long TEST_PRODUCT_ID = 1L;
    private static final String TEST_USER_ID = "user123";

    @BeforeEach
    void setup() {
        productMetricsRepository.deleteAll(); // Clean up before each test
        eventHandledRepository.deleteAll(); // Clean up before each test
    }

    @Test
    @DisplayName("새로운 좋아요 이벤트가 metrics에 정상 반영되고 멱등 처리된다")
    void testConsume_NewLikeEvent_ShouldUpdateMetricsAndBeIdempotent() throws JsonProcessingException {
        // Given
        String eventId = UUID.randomUUID().toString();
        ProductLikeToggledEvent event = new ProductLikeToggledEvent(
            eventId,
            "ProductLikeAdded",
            TEST_PRODUCT_ID,
            TEST_USER_ID,
            true,
            LocalDateTime.now()
        );
        String eventPayload = objectMapper.writeValueAsString(event);

        // When - Send the same event twice
        kafkaTemplate.send(CATALOG_TOPIC, event.getAggregateId(), eventPayload);
        kafkaTemplate.send(CATALOG_TOPIC, event.getAggregateId(), eventPayload);

        // Then - Verify that metrics are updated only once
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<ProductMetrics> metricsOpt = productMetricsRepository.findByProductId(TEST_PRODUCT_ID);
            assertThat(metricsOpt).isPresent();
            assertThat(metricsOpt.get().getLikeCount()).isEqualTo(1);
            assertThat(eventHandledRepository.existsByEventId(eventId)).isTrue();
        });
    }

    @Test
    @DisplayName("오래된 좋아요 이벤트는 스킵되고 metrics에 반영되지 않는다")
    void testConsume_OutdatedLikeEvent_ShouldBeSkipped() throws JsonProcessingException, InterruptedException {
        // Given - Send a newer event first
        LocalDateTime newerTime = LocalDateTime.now();
        String newerEventId = UUID.randomUUID().toString();
        ProductLikeToggledEvent newerEvent = new ProductLikeToggledEvent(
            newerEventId,
            "ProductLikeAdded",
            TEST_PRODUCT_ID,
            TEST_USER_ID,
            true,
            newerTime
        );
        String newerEventPayload = objectMapper.writeValueAsString(newerEvent);
        kafkaTemplate.send(CATALOG_TOPIC, newerEvent.getAggregateId(), newerEventPayload);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<ProductMetrics> metricsOpt = productMetricsRepository.findByProductId(TEST_PRODUCT_ID);
            assertThat(metricsOpt).isPresent();
            assertThat(metricsOpt.get().getLikeCount()).isEqualTo(1);
            assertThat(eventHandledRepository.existsByEventId(newerEventId)).isTrue();
        });

        // Add a small delay to ensure updatedAt changes if processed very quickly
        TimeUnit.MILLISECONDS.sleep(100);

        // When - Send an older event
        LocalDateTime olderTime = newerTime.minusMinutes(5);
        String olderEventId = UUID.randomUUID().toString();
        ProductLikeToggledEvent olderEvent = new ProductLikeToggledEvent(
            olderEventId,
            "ProductLikeAdded",
            TEST_PRODUCT_ID,
            TEST_USER_ID,
            true,
            olderTime
        );
        String olderEventPayload = objectMapper.writeValueAsString(olderEvent);
        kafkaTemplate.send(CATALOG_TOPIC, olderEvent.getAggregateId(), olderEventPayload);

        // Then - Verify that the metrics remain unchanged (older event was skipped)
        // Wait a bit to ensure the consumer had a chance to process the older event
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<ProductMetrics> metricsOpt = productMetricsRepository.findByProductId(TEST_PRODUCT_ID);
            assertThat(metricsOpt).isPresent();
            assertThat(metricsOpt.get().getLikeCount()).isEqualTo(1); // Still 1, not 2
            // The older event should also be marked as handled to prevent reprocessing
            assertThat(eventHandledRepository.existsByEventId(olderEventId)).isTrue();
        });
    }

    @Test
    @DisplayName("좋아요 제거 이벤트가 metrics에 정상 반영되고 멱등 처리된다")
    void testConsume_LikeRemovedEvent_ShouldUpdateMetricsAndBeIdempotent() throws JsonProcessingException {
        // Given - Add a like first
        String initialEventId = UUID.randomUUID().toString();
        ProductLikeToggledEvent initialEvent = new ProductLikeToggledEvent(
            initialEventId,
            "ProductLikeAdded",
            TEST_PRODUCT_ID,
            TEST_USER_ID,
            true,
            LocalDateTime.now().minusHours(1)
        );
        kafkaTemplate.send(CATALOG_TOPIC, initialEvent.getAggregateId(), objectMapper.writeValueAsString(initialEvent));
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<ProductMetrics> metricsOpt = productMetricsRepository.findByProductId(TEST_PRODUCT_ID);
            assertThat(metricsOpt).isPresent();
            assertThat(metricsOpt.get().getLikeCount()).isEqualTo(1);
        });

        // When - Send remove event twice
        String removeEventId = UUID.randomUUID().toString();
        ProductLikeToggledEvent removeEvent = new ProductLikeToggledEvent(
            removeEventId,
            "ProductLikeRemoved",
            TEST_PRODUCT_ID,
            TEST_USER_ID,
            false,
            LocalDateTime.now()
        );
        String removeEventPayload = objectMapper.writeValueAsString(removeEvent);
        kafkaTemplate.send(CATALOG_TOPIC, removeEvent.getAggregateId(), removeEventPayload);
        kafkaTemplate.send(CATALOG_TOPIC, removeEvent.getAggregateId(), removeEventPayload);

        // Then - Verify that like count is decremented only once
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<ProductMetrics> metricsOpt = productMetricsRepository.findByProductId(TEST_PRODUCT_ID);
            assertThat(metricsOpt).isPresent();
            assertThat(metricsOpt.get().getLikeCount()).isEqualTo(0);
            assertThat(eventHandledRepository.existsByEventId(removeEventId)).isTrue();
        });
    }
}

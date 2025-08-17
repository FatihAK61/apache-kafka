# Spring Boot Apache Kafka Entegrasyonu

Bu proje, Spring Boot ile Apache Kafka'nın nasıl entegre edileceğini gösteren kapsamlı bir örnek API'dir. Producer ve Consumer yapıları, konfigürasyonlar ve best practice'ler içermektedir.

## 📋 İçindekiler

- [Gereksinimler](#gereksinimler)
- [Kurulum](#kurulum)
- [Kafka Konfigürasyonu](#kafka-konfigürasyonu)
- [Producer Yapısı](#producer-yapısı)
- [Consumer Yapısı](#consumer-yapısı)
- [API Endpoints](#api-endpoints)
- [Örnek Kullanım](#örnek-kullanım)
- [Hata Yönetimi](#hata-yönetimi)
- [Monitoring](#monitoring)
- [Best Practices](#best-practices)

## 🛠 Gereksinimler

- Java 17 veya üzeri
- Spring Boot 3.x
- Apache Kafka 2.8+
- Maven 3.6+
- Docker (opsiyonel, Kafka için)

## 📦 Kurulum

### 1. Bağımlılıklar

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-json</artifactId>
    </dependency>
</dependencies>
```

### 2. Kafka Kurulumu (Docker ile)

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

## ⚙️ Kafka Konfigürasyonu

### application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      batch-size: 16384
      linger-ms: 1
      buffer-memory: 33554432
    consumer:
      group-id: my-consumer-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        spring.json.trusted.packages: "com.example.model"
    listener:
      ack-mode: manual_immediate
```

### Java Konfigürasyonu

```java
@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.model");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
```

## 📤 Producer Yapısı

### Message Model

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String id;
    private String name;
    private String email;
    private String action;
    private LocalDateTime timestamp;
}
```

### Producer Service

```java
@Service
@Slf4j
public class KafkaProducerService {

    private static final String USER_EVENTS_TOPIC = "user-events";
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserEvent(UserEvent userEvent) {
        try {
            userEvent.setTimestamp(LocalDateTime.now());
            
            ListenableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(USER_EVENTS_TOPIC, userEvent.getId(), userEvent);
            
            future.addCallback(
                result -> log.info("User event sent successfully: {}", userEvent.getId()),
                failure -> log.error("Failed to send user event: {}", userEvent.getId(), failure)
            );
        } catch (Exception e) {
            log.error("Error sending user event", e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }

    public void sendMessage(String topic, String key, Object message) {
        kafkaTemplate.send(topic, key, message);
        log.info("Message sent to topic: {} with key: {}", topic, key);
    }
}
```

## 📥 Consumer Yapısı

```java
@Service
@Slf4j
public class KafkaConsumerService {

    @KafkaListener(topics = "user-events", groupId = "user-events-group")
    public void consumeUserEvent(@Payload UserEvent userEvent,
                                @Header KafkaHeaders headers,
                                Acknowledgment acknowledgment) {
        try {
            log.info("Received user event: {}", userEvent);
            
            // İş mantığı burada işlenir
            processUserEvent(userEvent);
            
            // Manuel acknowledgment
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing user event: {}", userEvent, e);
            // Hata durumunda mesaj tekrar işlenebilir veya DLQ'ya gönderilebilir
        }
    }

    @KafkaListener(topics = "notifications", groupId = "notification-group")
    public void consumeNotification(@Payload String message,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received notification from topic: {}, partition: {}, offset: {}, message: {}", 
                topic, partition, offset, message);
    }

    private void processUserEvent(UserEvent userEvent) {
        // Kullanıcı eventi işleme mantığı
        switch (userEvent.getAction()) {
            case "CREATE":
                log.info("Processing user creation: {}", userEvent.getName());
                break;
            case "UPDATE":
                log.info("Processing user update: {}", userEvent.getName());
                break;
            case "DELETE":
                log.info("Processing user deletion: {}", userEvent.getName());
                break;
            default:
                log.warn("Unknown action: {}", userEvent.getAction());
        }
    }
}
```

## 🌐 API Endpoints

### User Controller

```java
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody CreateUserRequest request) {
        try {
            UserEvent userEvent = new UserEvent(
                UUID.randomUUID().toString(),
                request.getName(),
                request.getEmail(),
                "CREATE",
                LocalDateTime.now()
            );
            
            kafkaProducerService.sendUserEvent(userEvent);
            return ResponseEntity.ok("User creation event sent successfully");
            
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(500).body("Error processing request");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable String id, 
                                           @RequestBody UpdateUserRequest request) {
        UserEvent userEvent = new UserEvent(
            id,
            request.getName(),
            request.getEmail(),
            "UPDATE",
            LocalDateTime.now()
        );
        
        kafkaProducerService.sendUserEvent(userEvent);
        return ResponseEntity.ok("User update event sent successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable String id) {
        UserEvent userEvent = new UserEvent(
            id,
            null,
            null,
            "DELETE",
            LocalDateTime.now()
        );
        
        kafkaProducerService.sendUserEvent(userEvent);
        return ResponseEntity.ok("User deletion event sent successfully");
    }
}
```

### Message Controller

```java
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody SendMessageRequest request) {
        kafkaProducerService.sendMessage(
            request.getTopic(), 
            request.getKey(), 
            request.getMessage()
        );
        return ResponseEntity.ok("Message sent successfully");
    }
}
```

## 🚀 Örnek Kullanım

### Kullanıcı Oluşturma

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com"
  }'
```

### Mesaj Gönderme

```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "notifications",
    "key": "user-123",
    "message": "Welcome to our platform!"
  }'
```

## 🔧 Hata Yönetimi

### Global Error Handler

```java
@Component
@Slf4j
public class KafkaErrorHandler implements ConsumerAwareListenerErrorHandler {

    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception,
                            Consumer<?, ?> consumer) {
        log.error("Error in Kafka listener: {}", exception.getMessage(), exception);
        
        // Hata durumunda özel işlemler yapılabilir
        // Örnek: DLQ'ya mesaj gönderme, alerting, vb.
        
        return null;
    }
}
```

### Retry Konfigürasyonu

```java
@Bean
public RetryTemplate retryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();
    
    FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
    backOffPolicy.setBackOffPeriod(1000L);
    retryTemplate.setBackOffPolicy(backOffPolicy);
    
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(3);
    retryTemplate.setRetryPolicy(retryPolicy);
    
    return retryTemplate;
}
```

## 📊 Monitoring

### Health Check

```java
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Health health() {
        try {
            // Kafka bağlantısını test et
            kafkaTemplate.getProducerFactory().createProducer().partitionsFor("health-check");
            return Health.up().withDetail("kafka", "Available").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("kafka", "Unavailable").build();
        }
    }
}
```

### Metrics

```java
@Component
public class KafkaMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter messagesSent;
    private final Counter messagesReceived;

    public KafkaMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.messagesSent = Counter.builder("kafka.messages.sent")
            .description("Number of messages sent to Kafka")
            .register(meterRegistry);
        this.messagesReceived = Counter.builder("kafka.messages.received")
            .description("Number of messages received from Kafka")
            .register(meterRegistry);
    }

    public void incrementMessagesSent() {
        messagesSent.increment();
    }

    public void incrementMessagesReceived() {
        messagesReceived.increment();
    }
}
```

## 🏆 Best Practices

### 1. Topic Naming Convention
- Çevire göre prefix kullanın: `dev-`, `prod-`
- Anlamlı isimler: `user-events`, `order-notifications`
- Kebab-case kullanın

### 2. Serialization
- JSON serializasyon için trusted packages belirtin
- Schema evolution için Avro kullanmayı düşünün
- Backward compatibility'yi koruyun

### 3. Error Handling
- Dead Letter Queue (DLQ) kullanın
- Retry mekanizması implement edin
- İdempotent consumer yazın

### 4. Performance
- Batch size'ı optimize edin
- Compression kullanın (gzip, snappy)
- Partition sayısını doğru belirleyin

### 5. Security
- SSL/TLS encryption kullanın
- SASL authentication aktifleştirin
- ACL'ler ile authorization yapın

### 6. Monitoring
- Lag monitoring
- Throughput metrics
- Error rate tracking
- Consumer group health

## 🔗 Faydalı Linkler

- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Kafka Best Practices](https://kafka.apache.org/documentation/#bestpractices)

## 📄 Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakınız.

## 🤝 Katkıda Bulunma

1. Fork edin
2. Feature branch oluşturun (`git checkout -b feature/AmazingFeature`)
3. Commit edin (`git commit -m 'Add some AmazingFeature'`)
4. Push edin (`git push origin feature/AmazingFeature`)
5. Pull Request oluşturun

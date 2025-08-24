# Spring Boot Apache Kafka Integration

A comprehensive Spring Boot application demonstrating Apache Kafka integration with producer and consumer
implementations, configuration management.

## 🚀 Features

- **Producer Service**: Send messages to Kafka topics with error handling
- **Consumer Service**: Listen and process messages from Kafka topics
- **REST API**: HTTP endpoints for message publishing
- **Configuration Management**: Customizable Kafka settings
- **Health Checks**: Kafka connection monitoring

## 📋 Prerequisites

- Java 21+
- Apache Kafka 2.8+
- Maven 3.6+
- Docker (optional, for running Kafka locally)

## 🛠️ Installation

### 1. Clone the Repository

```bash
git clone https://github.com/FatihAK61/apache-kafka.git
cd apache-kafka
```

### 2. Start Kafka with Docker (Optional)

```bash
# Create docker-compose.yml
services:
  kafka:
    image: confluentinc/cp-kafka:latest
    container_name: kafka
    ports:
      - "9092:9092"
      - "9093:9093"
    environment:
      # KRaft mode konfigürasyonu
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_NODE_ID: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka:9093'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_LISTENERS: PLAINTEXT://kafka:29092,CONTROLLER://kafka:9093,PLAINTEXT_HOST://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_INTER_BROKER_LISTENER_NAME: 'PLAINTEXT'

      # Cluster konfigürasyonu
      KAFKA_CLUSTER_ID: 'MkU3OEVBNTcwNTJENDM2Qk'
      CLUSTER_ID: 'MkU3OEVBNTcwNTJENDM2Qk'

      # Log konfigürasyonu
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0

      # Topic yönetimi
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
      KAFKA_DELETE_TOPIC_ENABLE: 'true'

      # Log retention ayarları
      KAFKA_LOG_RETENTION_HOURS: 168
      KAFKA_LOG_SEGMENT_BYTES: 1073741824
      KAFKA_LOG_RETENTION_CHECK_INTERVAL_MS: 300000

      # JVM heap ayarları
      KAFKA_HEAP_OPTS: "-Xmx1G -Xms1G"

    volumes:
      - kafka-data:/var/lib/kafka/data
      - kafka-logs:/var/lib/kafka/logs
    networks:
      - kafka-network
    healthcheck:
      test: [ "CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092 || exit 1" ]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    restart: unless-stopped

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    depends_on:
      kafka:
        condition: service_healthy
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local-kraft
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
      KAFKA_CLUSTERS_0_PROPERTIES_SECURITY_PROTOCOL: PLAINTEXT
      DYNAMIC_CONFIG_ENABLED: 'true'
    networks:
      - kafka-network
    restart: unless-stopped

volumes:
  kafka-data:
    driver: local
  kafka-logs:
    driver: local

networks:
  kafka-network:
    driver: bridge


# Start services
docker-compose up -d
```

### 3. Build and Run

```bash
./mvnw clean install
./mvnw spring-boot:run
```

## ⚙️ Configuration

### Application Properties

```yaml
spring.application.name=apache-kafka
server.port=8081
spring.kafka.consumer.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=myGroup
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.producer.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
```

## 📤 Producer Usage

### Send Message via REST API

```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "user-events",
    "key": "user-123",
    "message": {
      "id": "123",
      "name": "John Doe",
      "action": "CREATE"
    }
  }'
```

### Producer Service Example

```java

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessage(String topic, String key, Object message) {
        kafkaTemplate.send(topic, key, message)
                .addCallback(
                        result -> log.info("Message sent successfully"),
                        failure -> log.error("Failed to send message", failure)
                );
    }
}
```

## 📥 Consumer Usage

### Consumer Service Example

```java

@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "user-events", groupId = "user-events-group")
    public void consumeUserEvent(@Payload Object message,
                                 Acknowledgment acknowledgment) {
        try {
            // Process message
            log.info("Received message: {}", message);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }
}
```

## 🌐 API Endpoints

| Method | Endpoint             | Description                 |
|--------|----------------------|-----------------------------|
| GET    | `/api/messages/send` | Send message to Kafka topic |
| POST   | `/api/messages/send` | Send message to Kafka topic |
| GET    | `/actuator/health`   | Application health check    |

## 🔧 Key Components

### 1. Kafka Configuration

- **Producer Factory**: Configures message serialization and delivery settings
- **Consumer Factory**: Sets up message deserialization and processing options
- **Kafka Template**: Provides high-level operations for sending messages

### 2. Error Handling

- **Retry Logic**: Automatic retry for failed message delivery
- **Dead Letter Queue**: Handle messages that cannot be processed
- **Acknowledgment**: Manual message acknowledgment for better control

### 3. Health Monitoring

- **Health Indicators**: Monitor Kafka connection status
- **Metrics**: Track message throughput and error rates

## 📊 Monitoring

The application includes health checks accessible at:

- `http://localhost:8080/actuator/health`

## 🛡️ Best Practices

1. **Serialization**: Use JSON serialization for flexible message formats
2. **Error Handling**: Implement proper retry mechanisms and error logging
3. **Acknowledgment**: Use manual acknowledgment for critical message processing
4. **Topic Naming**: Use descriptive, consistent topic names
5. **Consumer Groups**: Organize consumers in logical groups for scaling

## 🚨 Troubleshooting

### Common Issues

1. **Connection Failed**: Ensure Kafka is running on `localhost:9092`
2. **Serialization Errors**: Verify serializer configuration matches message types
3. **Consumer Lag**: Check consumer group performance and scaling

### Logs

```bash
# View application logs
./mvnw spring-boot:run

# Check Kafka logs (if running with Docker)
docker-compose logs kafka
```

## 📚 Dependencies

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
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🔗 Useful Links

- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Boot Reference](https://spring.io/projects/spring-boot)

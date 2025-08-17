# Spring Boot Apache Kafka Integration

A comprehensive Spring Boot application demonstrating Apache Kafka integration with producer and consumer implementations, configuration management, and REST API endpoints.

## 🚀 Features

- **Producer Service**: Send messages to Kafka topics with error handling
- **Consumer Service**: Listen and process messages from Kafka topics
- **REST API**: HTTP endpoints for message publishing
- **Configuration Management**: Customizable Kafka settings
- **Error Handling**: Robust error handling and retry mechanisms
- **Health Checks**: Kafka connection monitoring

## 📋 Prerequisites

- Java 21+
- Apache Kafka 2.8+
- Maven 3.6+
- Docker (optional, for running Kafka locally)

## 🛠️ Installation

### 1. Clone the Repository
```bash
git clone <repository-url>
cd apache-kafka
```

### 2. Start Kafka with Docker (Optional)
```bash
# Create docker-compose.yml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:latest
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

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
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
    consumer:
      group-id: my-consumer-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
    listener:
      ack-mode: manual_immediate
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

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/messages/send` | Send message to Kafka topic |
| GET | `/actuator/health` | Application health check |

### Request/Response Examples

**Send Message:**
```json
// Request
{
  "topic": "notifications",
  "key": "notify-001",
  "message": "Hello Kafka!"
}

// Response
{
  "status": "success",
  "message": "Message sent successfully"
}
```

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

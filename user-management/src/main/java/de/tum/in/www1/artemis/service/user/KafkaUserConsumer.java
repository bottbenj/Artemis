package de.tum.in.www1.artemis.service.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.www1.artemis.config.KafkaProperties;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.service.dto.KafkaUserGroupDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
public class KafkaUserConsumer {
    private final Logger log = LoggerFactory.getLogger(KafkaUserConsumer.class);

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private static final String TOPIC_DELETE_USER_GROUP = "topic_delete_user_group";
    private static final String TOPIC_ADD_USER_TO_GROUP = "topic_add_user_to_group";
    private static final String TOPIC_REMOVE_USER_FROM_GROUP = "topic_remove_user_to_group";

    private final KafkaProperties kafkaProperties;

    private KafkaConsumer<String, String> kafkaConsumer;

    private UserService userService;

    private UserRepository userRepository;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public KafkaUserConsumer(KafkaProperties kafkaProperties, UserService userService, UserRepository userRepository) {
        this.kafkaProperties = kafkaProperties;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void start() {

        log.debug("Kafka consumer starting...");
        this.kafkaConsumer = new KafkaConsumer<>(kafkaProperties.getConsumerProps());
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        kafkaConsumer.subscribe(Arrays.asList(TOPIC_DELETE_USER_GROUP, TOPIC_ADD_USER_TO_GROUP, TOPIC_REMOVE_USER_FROM_GROUP));
        log.debug("Kafka consumer started");

        executorService.execute(() -> {
            try {
                while (!closed.get()) {
                    ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(3));
                    for (ConsumerRecord<String, String> record : records) {
                        log.debug("Consumed message in {} : {}", record.topic(), record.value());
                        log.info("Consumed message in {} : {}", record.topic(), record.value());

                        switch (record.topic()) {
                            case TOPIC_DELETE_USER_GROUP -> userService.deleteGroup(record.value());
                            case TOPIC_ADD_USER_TO_GROUP -> {
                                KafkaUserGroupDTO data = readUserGroupValue(record.value());
                                User user = userRepository.getUserByLoginElseThrow(data.getUserLogin());
                                log.debug("User {}", user);
                                log.info("User {}", user);
                                userService.addUserToGroup(user, data.getGroupName(), data.getRole());
                            }
                            case TOPIC_REMOVE_USER_FROM_GROUP -> {
                                KafkaUserGroupDTO data = readUserGroupValue(record.value());
                                User user = userRepository.getUserByLoginElseThrow(data.getUserLogin());
                                log.debug("User {}", user);
                                log.info("User {}", user);
                                userService.removeUserFromGroup(user, data.getGroupName(), data.getRole());
                            }
                        }

                    }
                }
                kafkaConsumer.commitSync();
            } catch (WakeupException e) {
                // Ignore exception if closing
                if (!closed.get()) throw e;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                log.info("Kafka consumer close");
                kafkaConsumer.close();
            }
        });

    }

    public KafkaConsumer<String, String> getKafkaConsumer() {
        return kafkaConsumer;
    }

    public void shutdown() {
        log.info("Shutdown Kafka consumer");
        closed.set(true);
        kafkaConsumer.wakeup();
    }

    private KafkaUserGroupDTO readUserGroupValue(String value) {
        ObjectMapper objectMapper = new ObjectMapper();
        KafkaUserGroupDTO userGroupDTO = null;
        try {
            userGroupDTO = objectMapper.readValue(value, KafkaUserGroupDTO.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return userGroupDTO;
    }
}

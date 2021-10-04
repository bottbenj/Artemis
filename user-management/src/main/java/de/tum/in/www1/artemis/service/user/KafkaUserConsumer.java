package de.tum.in.www1.artemis.service.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.www1.artemis.config.KafkaProperties;
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
import java.util.Collections;
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

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public KafkaUserConsumer(KafkaProperties kafkaProperties, UserService userService) {
        this.kafkaProperties = kafkaProperties;
        this.userService = userService;
    }

    @PostConstruct
    public void start() {

        log.info("Kafka consumer starting...");
        this.kafkaConsumer = new KafkaConsumer<>(kafkaProperties.getConsumerProps());
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        kafkaConsumer.subscribe(Arrays.asList(TOPIC_DELETE_USER_GROUP, TOPIC_ADD_USER_TO_GROUP, TOPIC_REMOVE_USER_FROM_GROUP));
        log.info("Kafka consumer started");

        executorService.execute(() -> {
            try {
                while (!closed.get()) {
                    ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(3));
                    for (ConsumerRecord<String, String> record : records) {
                        log.info("Consumed message in {} : {}", record.key(), record.value());

                        switch (record.key()) {
                            case TOPIC_DELETE_USER_GROUP -> userService.deleteGroup(record.value());
                            case TOPIC_ADD_USER_TO_GROUP -> {
                                KafkaUserGroupDTO data = readUserGroupValue((record.value()));
                                userService.addUserToGroup(data.getUser(), data.getGroupName());
                            }
                            case TOPIC_REMOVE_USER_FROM_GROUP -> {
                                KafkaUserGroupDTO data = readUserGroupValue((record.value()));
                                userService.removeUserFromGroup(data.getUser(), data.getGroupName());
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

    public KafkaConsumer<String, String> getKafkaConsumer() {
        return kafkaConsumer;
    }

    public void shutdown() {
        log.info("Shutdown Kafka consumer");
        closed.set(true);
        kafkaConsumer.wakeup();
    }
}

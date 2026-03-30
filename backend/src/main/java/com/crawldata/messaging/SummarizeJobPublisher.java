package com.crawldata.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SummarizeJobPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.summarize.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.summarize.routing-key}")
    private String routingKey;

    public SummarizeJobPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(Long articleId) {
        var message = new SummarizeJobMessage(articleId, 0);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("Published summarize job for articleId={}", articleId);
    }
}

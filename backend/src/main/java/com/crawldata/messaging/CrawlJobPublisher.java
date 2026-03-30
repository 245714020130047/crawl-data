package com.crawldata.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CrawlJobPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.crawl.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.crawl.routing-key}")
    private String routingKey;

    public CrawlJobPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(Long sourceId) {
        var message = new CrawlJobMessage(sourceId, 0);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("Published crawl job for sourceId={}", sourceId);
    }
}

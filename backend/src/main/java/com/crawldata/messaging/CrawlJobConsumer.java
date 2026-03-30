package com.crawldata.messaging;

import com.crawldata.crawler.CrawlerWorker;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CrawlJobConsumer {

    private final CrawlerWorker crawlerWorker;

    @Value("${app.rabbitmq.crawl.max-retries}")
    private int maxRetries;

    @RabbitListener(queues = "${app.rabbitmq.crawl.queue}",
                    containerFactory = "rabbitListenerContainerFactory")
    public void consume(CrawlJobMessage message,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws Exception {
        try {
            crawlerWorker.execute(message.getSourceId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Crawl job failed for sourceId={}, retry={}: {}",
                    message.getSourceId(), message.getRetryCount(), ex.getMessage());
            if (message.getRetryCount() >= maxRetries) {
                log.warn("Max retries reached, sending to DLQ for sourceId={}", message.getSourceId());
                channel.basicReject(deliveryTag, false);  // route to DLQ via dead-letter exchange
            } else {
                channel.basicNack(deliveryTag, false, false);  // route to retry queue
            }
        }
    }
}

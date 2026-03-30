package com.crawldata.messaging;

import com.crawldata.ai.SummarizationService;
import com.crawldata.repository.ArticleRepository;
import com.crawldata.repository.SummarizationConfigRepository;
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
public class SummarizeJobConsumer {

    private final ArticleRepository articleRepository;
    private final SummarizationService summarizationService;
    private final SummarizationConfigRepository configRepository;

    @Value("${app.rabbitmq.summarize.max-retries}")
    private int maxRetries;

    @RabbitListener(queues = "${app.rabbitmq.summarize.queue}",
                    containerFactory = "rabbitListenerContainerFactory")
    public void consume(SummarizeJobMessage message,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws Exception {
        var config = configRepository.findFirstByOrderByIdAsc().orElse(null);
        if (config != null && !config.getIsEnabled()) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            var article = articleRepository.findById(message.getArticleId()).orElseThrow();
            summarizationService.summarize(article);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Summarize job failed for articleId={}, retry={}: {}",
                    message.getArticleId(), message.getRetryCount(), ex.getMessage());
            if (message.getRetryCount() >= maxRetries) {
                channel.basicReject(deliveryTag, false);
            } else {
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }
}

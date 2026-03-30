package com.crawldata.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.crawl.exchange}") private String crawlExchange;
    @Value("${app.rabbitmq.crawl.queue}")    private String crawlQueue;
    @Value("${app.rabbitmq.crawl.retry-queue}") private String crawlRetryQueue;
    @Value("${app.rabbitmq.crawl.dlq}")      private String crawlDlq;
    @Value("${app.rabbitmq.crawl.routing-key}") private String crawlRoutingKey;
    @Value("${app.rabbitmq.crawl.retry-ttl-ms}") private long crawlRetryTtl;

    @Value("${app.rabbitmq.summarize.exchange}") private String summarizeExchange;
    @Value("${app.rabbitmq.summarize.queue}")    private String summarizeQueue;
    @Value("${app.rabbitmq.summarize.retry-queue}") private String summarizeRetryQueue;
    @Value("${app.rabbitmq.summarize.dlq}")      private String summarizeDlq;
    @Value("${app.rabbitmq.summarize.routing-key}") private String summarizeRoutingKey;
    @Value("${app.rabbitmq.summarize.retry-ttl-ms}") private long summarizeRetryTtl;

    // ─── Exchanges ────────────────────────────────────────────────────────────
    @Bean TopicExchange crawlExchange() { return new TopicExchange(crawlExchange, true, false); }
    @Bean TopicExchange summarizeExchange() { return new TopicExchange(summarizeExchange, true, false); }

    // ─── Crawl pipeline queues ────────────────────────────────────────────────
    @Bean
    Queue crawlJobQueue() {
        return QueueBuilder.durable(crawlQueue)
                .withArgument("x-dead-letter-exchange", crawlExchange)
                .withArgument("x-dead-letter-routing-key", crawlRetryQueue)
                .build();
    }

    @Bean
    Queue crawlRetryQueue() {
        return QueueBuilder.durable(crawlRetryQueue)
                .withArgument("x-dead-letter-exchange", crawlExchange)
                .withArgument("x-dead-letter-routing-key", crawlRoutingKey)
                .withArgument("x-message-ttl", crawlRetryTtl)
                .build();
    }

    @Bean Queue crawlDlq() { return QueueBuilder.durable(crawlDlq).build(); }

    @Bean Binding crawlJobBinding()   { return BindingBuilder.bind(crawlJobQueue()).to(crawlExchange()).with(crawlRoutingKey); }
    @Bean Binding crawlRetryBinding() { return BindingBuilder.bind(crawlRetryQueue()).to(crawlExchange()).with(crawlRetryQueue); }
    @Bean Binding crawlDlqBinding()   { return BindingBuilder.bind(crawlDlq()).to(crawlExchange()).with(crawlDlq); }

    // ─── Summarize pipeline queues ────────────────────────────────────────────
    @Bean
    Queue summarizeJobQueue() {
        return QueueBuilder.durable(summarizeQueue)
                .withArgument("x-dead-letter-exchange", summarizeExchange)
                .withArgument("x-dead-letter-routing-key", summarizeRetryQueue)
                .build();
    }

    @Bean
    Queue summarizeRetryQueue() {
        return QueueBuilder.durable(summarizeRetryQueue)
                .withArgument("x-dead-letter-exchange", summarizeExchange)
                .withArgument("x-dead-letter-routing-key", summarizeRoutingKey)
                .withArgument("x-message-ttl", summarizeRetryTtl)
                .build();
    }

    @Bean Queue summarizeDlq() { return QueueBuilder.durable(summarizeDlq).build(); }

    @Bean Binding summarizeJobBinding()   { return BindingBuilder.bind(summarizeJobQueue()).to(summarizeExchange()).with(summarizeRoutingKey); }
    @Bean Binding summarizeRetryBinding() { return BindingBuilder.bind(summarizeRetryQueue()).to(summarizeExchange()).with(summarizeRetryQueue); }
    @Bean Binding summarizeDlqBinding()   { return BindingBuilder.bind(summarizeDlq()).to(summarizeExchange()).with(summarizeDlq); }

    // ─── Message converter + template ─────────────────────────────────────────
    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        var template = new RabbitTemplate(cf);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory cf) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(messageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(5);
        return factory;
    }
}

package com.crawldata.crawler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private final RedisTemplate<String, Object> redisTemplate;

    private String key() {
        return "crawled_urls:" + LocalDate.now();
    }

    public boolean isDuplicate(String url) {
        Boolean isMember = redisTemplate.opsForSet().isMember(key(), url);
        return Boolean.TRUE.equals(isMember);
    }

    public void markSeen(String url) {
        String k = key();
        redisTemplate.opsForSet().add(k, url);
        redisTemplate.expire(k, Duration.ofHours(25));  // keep slightly beyond 24 hours
    }
}

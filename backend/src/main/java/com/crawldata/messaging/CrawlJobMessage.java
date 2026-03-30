package com.crawldata.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrawlJobMessage implements Serializable {
    private Long sourceId;
    private int  retryCount;
}

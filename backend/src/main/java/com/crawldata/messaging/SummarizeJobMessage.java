package com.crawldata.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummarizeJobMessage implements Serializable {
    private Long articleId;
    private int  retryCount;
}

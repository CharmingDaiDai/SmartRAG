package com.mtmn.smartrag.model.dto;

import dev.langchain4j.data.embedding.Embedding;
import lombok.Data;

import java.util.Map;

/**
 * 向量项 DTO
 */
@Data
public class VectorItem {
    private String id;
    private Embedding embedding;
    private String content;
    private Map<String, Object> metadata;
}

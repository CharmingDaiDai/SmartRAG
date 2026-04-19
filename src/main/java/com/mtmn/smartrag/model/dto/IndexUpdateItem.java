package com.mtmn.smartrag.model.dto;

import dev.langchain4j.data.embedding.Embedding;
import lombok.Data;

import java.util.Map;

/**
 * 索引更新项 DTO
 */
@Data
public class IndexUpdateItem {
    private String id;
    private Embedding embedding;
    private String content;
    private Map<String, Object> metadata;
    private UpdateType updateType;

    public enum UpdateType {
        ADD, UPDATE, DELETE
    }
}

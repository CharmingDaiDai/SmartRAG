package com.mtmn.smartrag.common;

import lombok.Data;

import java.util.*;

/**
 * @author charmingdaidai
 */
@Data
public class MyNode {
    private String id;
    private String pageContent;
    private int level;
    private String title;
    private String parentId;
    private List<String> children;
    private String blockNumber;
    private Map<String, Object> metadata;

    public MyNode(String pageContent, int level, String title) {
        this(pageContent, level, title, null);
    }

    public MyNode(String pageContent, int level, String title, String blockNumber) {
        this.id = UUID.randomUUID().toString();
        this.pageContent = pageContent;
        this.level = level;
        this.title = title;
        this.children = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.blockNumber = blockNumber;
    }

    public void addChild(String childId) {
        this.children.add(childId);
    }
}
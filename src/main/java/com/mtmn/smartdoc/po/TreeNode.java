package com.mtmn.smartdoc.po;

import com.mtmn.smartdoc.enums.TreeNodeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TreeNode 树节点实体类（HisemRAG）
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tree_nodes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_kb_node", columnNames = {"kb_id", "node_id"})
        },
        indexes = {
                @Index(name = "idx_kb_id", columnList = "kb_id"),
                @Index(name = "idx_document_id", columnList = "document_id"),
                @Index(name = "idx_parent", columnList = "parent_node_id"),
                @Index(name = "idx_level", columnList = "level"),
                @Index(name = "idx_node_type", columnList = "node_type")
        }
)
public class TreeNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(name = "parent_node_id")
    private String parentNodeId;

    @Column(name = "title_path", length = 1000, nullable = false)
    private String titlePath;

    @Column(name = "block_index")
    private Integer blockIndex;

    @Column(nullable = false)
    private Integer level;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false)
    private TreeNodeType nodeType;

    @Column(name = "key_knowledge", columnDefinition = "TEXT")
    private String keyKnowledge;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_fragments", columnDefinition = "JSON")
    private String contentFragments;

    @Column(name = "children_ids", columnDefinition = "JSON")
    private String childrenIds;

    @Column(name = "vector_ids", columnDefinition = "JSON")
    private String vectorIds;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
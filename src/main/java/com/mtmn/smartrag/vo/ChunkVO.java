package com.mtmn.smartrag.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chunk 内容块 VO
 *
 * @author charmingdaidai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkVO {

    private Long id;
    private Long kbId;
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    /** HiSem-Fast 关键知识点（JSON 数组字符串） */
    private String keyKnowledge;
    /** HiSem-Fast 摘要 */
    private String summary;
    private String strategyType;
}

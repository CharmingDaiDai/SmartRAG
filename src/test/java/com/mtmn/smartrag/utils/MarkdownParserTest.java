package com.mtmn.smartrag.utils;

import com.mtmn.smartrag.common.MyNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MarkdownParserTest {

    @Test
    void shouldAttachSkippedLevelHeadingToNearestHigherLevelParent() {
        String markdown = "# A\n\n### C\n";

        Map.Entry<MyNode, Map<String, MyNode>> result =
                MarkdownParser.parseMarkdownStructure(markdown, "Doc", null);
        Map<String, MyNode> nodes = result.getValue();

        MyNode child = findSingleByTitle(nodes, "C");
        MyNode parent = requireNodeById(nodes, child.getParentId());

        assertEquals("A", parent.getTitle());
        assertEquals(3, child.getLevel());
    }

    @Test
    void shouldKeepDuplicateTitleParentingStable() {
        String markdown = "# Intro\n## Overview\n# Methods\n## Overview\n### Details\n";

        Map.Entry<MyNode, Map<String, MyNode>> result =
                MarkdownParser.parseMarkdownStructure(markdown, "Doc", null);
        Map<String, MyNode> nodes = result.getValue();

        MyNode details = findSingleByTitle(nodes, "Details");
        MyNode detailsParent = requireNodeById(nodes, details.getParentId());
        MyNode detailsGrandParent = requireNodeById(nodes, detailsParent.getParentId());

        assertEquals("Overview", detailsParent.getTitle());
        assertEquals("Methods", detailsGrandParent.getTitle());
    }

    @Test
    void shouldParseHeadingWithUpToThreeLeadingSpaces() {
        String markdown = "# Top\n   ## Child\n    ### Not Heading\n";

        Map.Entry<MyNode, Map<String, MyNode>> result =
                MarkdownParser.parseMarkdownStructure(markdown, "Doc", null);
        Map<String, MyNode> nodes = result.getValue();

        MyNode child = findSingleByTitle(nodes, "Child");
        MyNode parent = requireNodeById(nodes, child.getParentId());

        assertEquals("Top", parent.getTitle());
        assertTrue(child.getPageContent().contains("    ### Not Heading"));
        assertTrue(findByTitle(nodes, "Not Heading").isEmpty());
    }

    @Test
    void shouldIgnoreHeadingsInsideFencedCodeBlocks() {
        String markdown = "# Top\n```java\n## Fake\n```\n## Real\n";

        Map.Entry<MyNode, Map<String, MyNode>> result =
                MarkdownParser.parseMarkdownStructure(markdown, "Doc", null);
        Map<String, MyNode> nodes = result.getValue();

        assertTrue(findByTitle(nodes, "Fake").isEmpty());

        MyNode top = findSingleByTitle(nodes, "Top");
        MyNode real = findSingleByTitle(nodes, "Real");
        MyNode realParent = requireNodeById(nodes, real.getParentId());

        assertEquals("Top", realParent.getTitle());
        assertTrue(top.getPageContent().contains("## Fake"));
    }

    @Test
    void shouldTreatOverMaxLevelHeadingsAsContent() {
        String markdown = "# A\n## B\n### C\n#### D\n";

        Map.Entry<MyNode, Map<String, MyNode>> result =
                MarkdownParser.parseMarkdownStructure(markdown, "Doc", 2);
        Map<String, MyNode> nodes = result.getValue();

        assertTrue(findByTitle(nodes, "C").isEmpty());
        assertTrue(findByTitle(nodes, "D").isEmpty());

        MyNode b = findSingleByTitle(nodes, "B");
        assertTrue(b.getPageContent().contains("### C"));
        assertTrue(b.getPageContent().contains("#### D"));
    }

    @Test
    void shouldKeepChunkChildIdsAndLevelsConsistentAfterSplit() {
        String markdown = "# Section\n" + "A".repeat(5000);

        Map.Entry<MyNode, Map<String, MyNode>> result =
                MarkdownProcessor.parseMarkdownContent(markdown, "Doc", null, 500);
        Map<String, MyNode> nodes = result.getValue();

        MyNode section = findSingleByTitle(nodes, "Section");
        assertFalse(section.getChildren().isEmpty(), "Section should be split into chunk children");

        for (String childId : section.getChildren()) {
            assertTrue(nodes.containsKey(childId), "Child id must exist in nodes map: " + childId);
            MyNode child = nodes.get(childId);

            assertEquals(section.getId(), child.getParentId());
            assertEquals(section.getLevel(), child.getLevel());
            assertEquals(Boolean.TRUE, child.getMetadata().get("chunk_child"));
            assertEquals(section.getLevel(), child.getMetadata().get("original_level"));
        }
    }

    private static List<MyNode> findByTitle(Map<String, MyNode> nodes, String title) {
        return nodes.values().stream()
                .filter(node -> title.equals(node.getTitle()))
                .toList();
    }

    private static MyNode findSingleByTitle(Map<String, MyNode> nodes, String title) {
        List<MyNode> matched = findByTitle(nodes, title);
        assertEquals(1, matched.size(), "Expected exactly one node with title: " + title);
        return matched.get(0);
    }

    private static MyNode requireNodeById(Map<String, MyNode> nodes, String nodeId) {
        assertNotNull(nodeId);
        MyNode node = nodes.get(nodeId);
        assertNotNull(node, "Node not found for id: " + nodeId);
        return node;
    }
}

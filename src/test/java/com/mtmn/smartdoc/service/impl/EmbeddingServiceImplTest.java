//package com.mtmn.smartdoc.service.impl;
//
//import com.mtmn.smartdoc.model.client.EmbeddingClient;
//import com.mtmn.smartdoc.model.factory.ModelFactory;
//import dev.langchain4j.data.embedding.Embedding;
//import dev.langchain4j.data.segment.TextSegment;
//import dev.langchain4j.model.embedding.EmbeddingModel;
//import dev.langchain4j.model.output.Response;
//import dev.langchain4j.store.embedding.EmbeddingStore;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Arrays;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
///**
// * EmbeddingServiceImpl 单元测试
// *
// * @author charmingdaidai
// * @version 1.0
// * @date 2025/11/20
// */
//@ExtendWith(MockitoExtension.class)
//class EmbeddingServiceImplTest {
//
//    @Mock
//    private ModelFactory modelFactory;
//
//    @Mock
//    private EmbeddingClient mockEmbeddingClient;
//
//    @InjectMocks
//    private EmbeddingServiceImpl embeddingService;
//
//    @BeforeEach
//    void setUp() {
//        // 默认行为
//        lenient().when(modelFactory.createDefaultEmbeddingClient()).thenReturn(mockEmbeddingClient);
//        lenient().when(modelFactory.createEmbeddingClient(anyString())).thenReturn(mockEmbeddingClient);
//        lenient().when(mockEmbeddingClient.getDimension()).thenReturn(1024);
//    }
//
//    @Test
//    void testCreateEmbeddingModel_WithoutModelId() {
//        // Given
//        List<Float> mockVector = createMockVector(1024);
//        lenient().when(mockEmbeddingClient.embed(anyString())).thenReturn(mockVector);
//
//        // When
//        EmbeddingModel result = embeddingService.createEmbeddingModel();
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.dimension()).isEqualTo(1024);
//        verify(modelFactory).createDefaultEmbeddingClient();
//    }
//
//    @Test
//    void testCreateEmbeddingModel_WithModelId() {
//        // Given
//        String modelId = "xinference-bge-m3";
//        List<Float> mockVector = createMockVector(1024);
//        lenient().when(mockEmbeddingClient.embed(anyString())).thenReturn(mockVector);
//
//        // When
//        EmbeddingModel result = embeddingService.createEmbeddingModel(modelId);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.dimension()).isEqualTo(1024);
//        verify(modelFactory).createEmbeddingClient(modelId);
//    }
//
//    @Test
//    void testCreateEmbeddingModel_WithEmptyModelId_ShouldUseDefault() {
//        // When
//        EmbeddingModel result = embeddingService.createEmbeddingModel("");
//
//        // Then
//        assertThat(result).isNotNull();
//        verify(modelFactory).createDefaultEmbeddingClient();
//    }
//
//    @Test
//    void testCreateEmbeddingModel_EmbedSingleText() {
//        // Given
//        String text = "测试文本";
//        List<Float> mockVector = createMockVector(1024);
//        lenient().when(mockEmbeddingClient.embed(text)).thenReturn(mockVector);
//
//        // When
//        EmbeddingModel model = embeddingService.createEmbeddingModel();
//        Response<Embedding> response = model.embed(text);
//
//        // Then
//        assertThat(response).isNotNull();
//        assertThat(response.content()).isNotNull();
//        assertThat(response.content().vector()).hasSize(1024);
//        verify(mockEmbeddingClient).embed(text);
//    }
//
//    @Test
//    void testCreateEmbeddingModel_EmbedTextSegment() {
//        // Given
//        TextSegment segment = TextSegment.from("测试文本段");
//        List<Float> mockVector = createMockVector(1024);
//        lenient().when(mockEmbeddingClient.embed(anyString())).thenReturn(mockVector);
//
//        // When
//        EmbeddingModel model = embeddingService.createEmbeddingModel();
//        Response<Embedding> response = model.embed(segment);
//
//        // Then
//        assertThat(response).isNotNull();
//        assertThat(response.content()).isNotNull();
//        verify(mockEmbeddingClient).embed("测试文本段");
//    }
//
//    @Test
//    void testCreateEmbeddingModel_EmbedBatch() {
//        // Given
//        List<TextSegment> segments = Arrays.asList(
//            TextSegment.from("文本1"),
//            TextSegment.from("文本2"),
//            TextSegment.from("文本3")
//        );
//
//        List<List<Float>> mockVectors = Arrays.asList(
//            createMockVector(1024),
//            createMockVector(1024),
//            createMockVector(1024)
//        );
//
//        lenient().when(mockEmbeddingClient.embedBatch(anyList())).thenReturn(mockVectors);
//
//        // When
//        EmbeddingModel model = embeddingService.createEmbeddingModel();
//        Response<List<Embedding>> response = model.embedAll(segments);
//
//        // Then
//        assertThat(response).isNotNull();
//        assertThat(response.content()).hasSize(3);
//        verify(mockEmbeddingClient).embedBatch(Arrays.asList("文本1", "文本2", "文本3"));
//    }
//
//    @Test
//    void testCreateDocumentVectors_WithDefaultModel() {
//        // Given
//        String content = "这是一段测试文本内容。\n\n这是第二段。\n\n这是第三段。";
//        setupMockEmbedding();
//
//        // When
//        EmbeddingStore<Embedding> result = embeddingService.createDocumentVectors(content);
//
//        // Then
//        assertThat(result).isNotNull();
//        verify(modelFactory).createDefaultEmbeddingClient();
//        verify(mockEmbeddingClient, atLeastOnce()).embedBatch(anyList());
//    }
//
//    @Test
//    void testCreateDocumentVectors_WithSpecifiedModel() {
//        // Given
//        String content = "测试文本内容";
//        String modelId = "qwen@text-embedding-v4";
//        setupMockEmbedding();
//
//        // When
//        EmbeddingStore<Embedding> result = embeddingService.createDocumentVectors(content, modelId);
//
//        // Then
//        assertThat(result).isNotNull();
//        verify(modelFactory).createEmbeddingClient(modelId);
//    }
//
//    @Test
//    void testCreateDocumentVectors_WithNullModelId_ShouldUseDefault() {
//        // Given
//        String content = "测试文本";
//        setupMockEmbedding();
//
//        // When
//        EmbeddingStore<Embedding> result = embeddingService.createDocumentVectors(content, null);
//
//        // Then
//        assertThat(result).isNotNull();
//        verify(modelFactory).createDefaultEmbeddingClient();
//    }
//
//    @Test
//    void testCreateDocumentVectors_LongContent_ShouldBatchProcess() {
//        // Given
//        StringBuilder longContent = new StringBuilder();
//        for (int i = 0; i < 100; i++) {
//            longContent.append("这是第").append(i).append("段内容。\n\n");
//        }
//        setupMockEmbedding();
//
//        // When
//        EmbeddingStore<Embedding> result = embeddingService.createDocumentVectors(longContent.toString());
//
//        // Then
//        assertThat(result).isNotNull();
//        // 验证批量处理被调用
//        verify(mockEmbeddingClient, atLeastOnce()).embedBatch(anyList());
//    }
//
//    @Test
//    void testEmbeddingModelAdapter_Dimension() {
//        // Given
//        when(mockEmbeddingClient.getDimension()).thenReturn(768);
//
//        // When
//        EmbeddingModel model = embeddingService.createEmbeddingModel();
//
//        // Then
//        assertThat(model.dimension()).isEqualTo(768);
//    }
//
//    /**
//     * 创建模拟向量
//     */
//    private List<Float> createMockVector(int dimension) {
//        Float[] vector = new Float[dimension];
//        for (int i = 0; i < dimension; i++) {
//            vector[i] = (float) Math.random();
//        }
//        return Arrays.asList(vector);
//    }
//
//    /**
//     * 设置模拟嵌入响应
//     */
//    private void setupMockEmbedding() {
//        List<List<Float>> mockBatchVectors = Arrays.asList(
//            createMockVector(1024),
//            createMockVector(1024),
//            createMockVector(1024),
//            createMockVector(1024),
//            createMockVector(1024)
//        );
//
//        when(mockEmbeddingClient.embedBatch(anyList())).thenReturn(mockBatchVectors);
//        when(mockEmbeddingClient.embed(anyString())).thenReturn(createMockVector(1024));
//    }
//}
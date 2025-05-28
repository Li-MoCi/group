package com.warmer.web.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.warmer.web.service.LLMService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper; // <--- 新增导入
import java.util.List; // <--- 新增导入
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/llm")
public class LLMController {

    private static final Logger logger = LoggerFactory.getLogger(LLMController.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper(); // <--- 创建一个静态实例以复用

    @Autowired
    private LLMService llmService;

    /**
     * 从文本中提取知识图谱数据（节点和链接）
     */
    @PostMapping("/extract")
    public Map<String, Object> extractTriples(@RequestBody Map<String, String> payload) {
        String text = payload.get("text");
        // String domainId = payload.get("domainId"); // 如果需要

        logger.info("LLMController: Received text for extraction: '{}'", text);
        // logger.info("LLMController: Received text: '{}', domainId: '{}'", text,
        // domainId);

        Map<String, Object> result = llmService.extractTriples(text);

        Object nodesObject = result.get("nodes");
        Object linksObject = result.get("links");
        int nodesCount = 0;
        int linksCount = 0;

        if (nodesObject instanceof List) {
            nodesCount = ((List<?>) nodesObject).size();
        }
        if (linksObject instanceof List) {
            linksCount = ((List<?>) linksObject).size();
        }

        logger.info("LLMController: Result from LLMService (nodes: {}, links: {})", nodesCount, linksCount);

        if (logger.isDebugEnabled()) {
            try {
                logger.debug("LLMController: Full result from LLMService: {}", jsonMapper.writeValueAsString(result));
            } catch (JsonProcessingException e) {
                logger.warn("LLMController: Could not serialize result for debug logging", e);
            }
        }
        return result;
    }

    /**
     * 处理自然语言查询 (此方法保持不变)
     */
    @PostMapping("/query")
    public String handleQuery(@RequestBody Map<String, Object> request) throws JsonProcessingException {
        String query = (String) request.get("query");
        Map<String, Object> graphContext = (Map<String, Object>) request.get("graphContext");
        logger.info("LLMController: Received query: {}", query);
        return llmService.generateAnswer(query, graphContext);
    }

}
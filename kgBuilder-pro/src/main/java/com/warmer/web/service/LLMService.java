package com.warmer.web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.*;
import java.util.*;
import java.util.stream.Collectors; // 新增导入

// SLF4J Imports for Logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.api.url}")
    private String apiUrl;

    @Value("${llm.api.model}")
    private String modelName;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LLMService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 从文本中提取知识图谱数据（节点和链接）
     * 
     * @param text 输入文本
     * @return 包含nodes和links的Map
     */
    public Map<String, Object> extractTriples(String text) { // <--- 修改返回类型
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content",
                        "你是一个知识图谱专家，负责从文本中提取实体和关系。请以JSON数组格式返回三元组列表，其中每个三元组对象应包含以下键：'head' (字符串，头实体名称), 'relation' (字符串，关系名称), 'tail' (字符串，尾实体名称)。如果识别出实体类型，请同时提供 'headType' (字符串) 和 'tailType' (字符串)。确保返回的JSON内容是纯粹的JSON数组，不要包含任何markdown或其他包裹字符。例如：[{\"head\": \"A\", \"relation\": \"rel\", \"tail\": \"B\"}]"),
                Map.of("role", "user", "content", "从以下文本中提取实体和关系，以三元组形式返回：\\n" + text)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        logger.info("LLMService: Sending request to LLM API URL: {}", apiUrl);
        logger.info("LLMService: Request Authorization Header present: {}",
                headers.getFirst("Authorization") != null && !headers.getFirst("Authorization").isEmpty());

        if (logger.isDebugEnabled()) {
            try {
                logger.debug("LLMService: Request Body: {}", objectMapper.writeValueAsString(requestBody));
            } catch (JsonProcessingException e) {
                logger.warn("LLMService: Could not serialize request body for debug logging", e);
            }
        }

        Map<String, Object> emptyGraph = new HashMap<>();
        emptyGraph.put("nodes", new ArrayList<>());
        emptyGraph.put("links", new ArrayList<>());

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(apiUrl, requestEntity, String.class);
            logger.info("LLMService: Received response from LLM API. Status Code: {}", responseEntity.getStatusCode());
            String responseBody = responseEntity.getBody();

            if (logger.isDebugEnabled()) {
                logger.debug("LLMService: Raw response body from LLM API: {}", responseBody);
            } else if (responseBody != null) {
                logger.info(
                        "LLMService: Raw response body from LLM API (length {} chars). Check DEBUG log for full body.",
                        responseBody.length());
            } else {
                logger.info("LLMService: Raw response body from LLM API is null.");
            }

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                return parseTriplesAndConvertToGraph(responseBody); // <--- 调用新的转换方法
            } else {
                logger.error(
                        "LLMService: LLM API call failed with status code: {}. Response body (first 500 chars): {}",
                        responseEntity.getStatusCode(),
                        (responseBody != null && responseBody.length() > 500) ? responseBody.substring(0, 500)
                                : responseBody);
                return emptyGraph; // 返回空图结构
            }
        } catch (Exception e) {
            logger.error("LLMService: Exception during LLM API call to URL: {}. Exception Type: {}, Message: {}",
                    apiUrl, e.getClass().getName(), e.getMessage(), e);
            return emptyGraph; // 返回空图结构
        }
    }

    /**
     * 生成图谱查询的自然语言回答
     * 
     * @param query        用户查询
     * @param graphContext 图谱上下文
     * @return 生成的回答
     */
    public String generateAnswer(String query, Map<String, Object> graphContext) throws JsonProcessingException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", "你是一个知识图谱问答助手，负责回答用户关于图谱的问题。"),
                Map.of("role", "user", "content", String.format("基于以下图谱信息回答问题：\\n%s\\n\\n问题：%s",
                        objectMapper.writeValueAsString(graphContext), query))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            return parseAnswer(response.getBody());
        } catch (Exception e) {
            logger.error("LLMService: Exception during LLM generateAnswer call to URL: {}. ", apiUrl, e);
            return "抱歉，处理您的问题时出现错误。";
        }
    }

    /**
     * 清理LLM返回内容中的Markdown代码块标记
     */
    private String cleanupLLMContent(String content) {
        if (content == null)
            return null;
        String cleanedContent = content.trim();
        // 移除 ```json ... ``` 结构
        if (cleanedContent.startsWith("```json") && cleanedContent.endsWith("```")) {
            cleanedContent = cleanedContent.substring(7, cleanedContent.length() - 3).trim();
        }
        // 移除 ``` ... ``` 结构
        else if (cleanedContent.startsWith("```") && cleanedContent.endsWith("```")) {
            cleanedContent = cleanedContent.substring(3, cleanedContent.length() - 3).trim();
        }
        return cleanedContent;
    }

    /**
     * 解析LLM返回的三元组，并将其转换为图谱的nodes和links结构
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseTriplesAndConvertToGraph(String responseBody) {
        Map<String, Object> graph = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        graph.put("nodes", nodes);
        graph.put("links", links);

        if (responseBody == null || responseBody.trim().isEmpty()) {
            logger.warn("LLMService: parseTriplesAndConvertToGraph received null or empty response body.");
            return graph; // 返回空图结构
        }
        try {
            logger.debug("LLMService: Attempting to parse response body for triples: {}", responseBody);
            Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                    new TypeReference<Map<String, Object>>() {
                    });

            Object choicesObject = responseMap.get("choices");
            if (!(choicesObject instanceof List)) {
                logger.warn("LLMService: 'choices' field is not a List or is missing. Response map: {}", responseMap);
                return graph;
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) choicesObject;

            if (choices.isEmpty()) {
                logger.warn("LLMService: 'choices' array is empty. Response map: {}", responseMap);
                return graph;
            }

            Map<String, Object> firstChoice = choices.get(0);
            if (firstChoice == null) {
                logger.warn("LLMService: First choice in 'choices' array is null.");
                return graph;
            }

            Object messageObject = firstChoice.get("message");
            if (!(messageObject instanceof Map)) {
                logger.warn("LLMService: 'message' field in first choice is not a Map or is missing. First choice: {}",
                        firstChoice);
                return graph;
            }
            Map<String, Object> message = (Map<String, Object>) messageObject;

            Object contentObject = message.get("content");
            if (!(contentObject instanceof String) || ((String) contentObject).trim().isEmpty()) {
                logger.warn(
                        "LLMService: 'content' field in message is not a non-empty String or is missing. Message: {}",
                        message);
                return graph;
            }
            String rawContent = ((String) contentObject).trim();
            logger.debug("LLMService: Raw extracted content for triple parsing: {}", rawContent);

            String cleanedContent = cleanupLLMContent(rawContent);
            logger.info("LLMService: Cleaned content for triple parsing: {}", cleanedContent);

            if (cleanedContent == null || cleanedContent.isEmpty()) {
                logger.warn("LLMService: Cleaned content is empty after attempting to remove markdown.");
                return graph;
            }

            List<Map<String, String>> triplesList;
            try {
                // 尝试直接解析为 List<Map<String, String>>
                if (cleanedContent.startsWith("[") && cleanedContent.endsWith("]")) {
                    triplesList = objectMapper.readValue(cleanedContent,
                            new TypeReference<List<Map<String, String>>>() {
                            });
                } else {
                    // 如果不是直接的JSON数组，记录警告并返回空图
                    logger.warn("LLMService: Cleaned content is not a JSON array. Content: {}", cleanedContent);
                    return graph;
                }

            } catch (JsonProcessingException jsonEx) {
                logger.error(
                        "LLMService: Failed to parse cleaned 'content' as JSON List<Map<String, String>>. Cleaned Content: {}",
                        cleanedContent, jsonEx);
                return graph;
            }

            if (triplesList == null || triplesList.isEmpty()) {
                logger.info("LLMService: Parsed triples list is null or empty from content: {}", cleanedContent);
                return graph;
            }

            logger.info("LLMService: Successfully parsed {} triples from LLM content.", triplesList.size());

            // 将三元组转换为 nodes 和 links
            Set<String> nodeNames = new HashSet<>(); // 用于基于名称跟踪已创建的节点，以避免重复
            // 使用 Map 来存储 name 到 id 的映射，以便快速查找
            Map<String, String> nodeNameToIdMap = new HashMap<>();
            int nodeIdCounter = 1; // 用于生成简单递增ID的备用方案

            for (Map<String, String> triple : triplesList) {
                String headName = triple.get("head");
                String tailName = triple.get("tail");
                String relationName = triple.get("relation");
                String headType = triple.getOrDefault("headType", "未知类型");
                String tailType = triple.getOrDefault("tailType", "未知类型");

                if (headName == null || tailName == null || relationName == null || headName.trim().isEmpty()
                        || tailName.trim().isEmpty() || relationName.trim().isEmpty()) {
                    logger.warn("LLMService: Skipping invalid triple due to null or empty head/tail/relation: {}",
                            triple);
                    continue;
                }
                headName = headName.trim();
                tailName = tailName.trim();
                relationName = relationName.trim();

                String headNodeId;
                if (nodeNameToIdMap.containsKey(headName)) {
                    headNodeId = nodeNameToIdMap.get(headName);
                } else {
                    headNodeId = "llm_node_" + nodeIdCounter++;
                    nodeNameToIdMap.put(headName, headNodeId);

                    Map<String, Object> headNode = new HashMap<>();
                    headNode.put("id", headNodeId);
                    headNode.put("name", headName);
                    headNode.put("label", headName);
                    headNode.put("type", headType);
                    headNode.put("properties", Map.of("type", headType, "description", "由LLM生成", "source", "llm"));
                    nodes.add(headNode);
                    nodeNames.add(headName); // 仍用nodeNames跟踪，尽管ID现在用Map管理
                }

                String tailNodeId;
                if (nodeNameToIdMap.containsKey(tailName)) {
                    tailNodeId = nodeNameToIdMap.get(tailName);
                } else {
                    tailNodeId = "llm_node_" + nodeIdCounter++;
                    nodeNameToIdMap.put(tailName, tailNodeId);

                    Map<String, Object> tailNode = new HashMap<>();
                    tailNode.put("id", tailNodeId);
                    tailNode.put("name", tailName);
                    tailNode.put("label", tailName);
                    tailNode.put("type", tailType);
                    tailNode.put("properties", Map.of("type", tailType, "description", "由LLM生成", "source", "llm"));
                    nodes.add(tailNode);
                    nodeNames.add(tailName);
                }

                Map<String, Object> link = new HashMap<>();
                link.put("id", "llm_link_" + headNodeId + "_" + tailNodeId + "_" + relationName.hashCode() + "_"
                        + UUID.randomUUID().toString().substring(0, 4));
                link.put("source", headNodeId);
                link.put("target", tailNodeId);
                link.put("name", relationName);
                link.put("label", relationName);
                link.put("properties", Map.of("description", "由LLM生成", "source", "llm"));
                links.add(link);
            }
            logger.info("LLMService: Converted to {} nodes and {} links.", nodes.size(), links.size());

        } catch (JsonProcessingException e) {
            logger.error("LLMService: JsonProcessingException while parsing outer LLM response. Response body: {}",
                    responseBody, e);
        } catch (Exception e) {
            logger.error("LLMService: Unexpected exception while parsing LLM response. Response body: {}", responseBody,
                    e);
        }
        return graph;
    }

    /**
     * 解析LLM返回的回答 (此方法保持不变)
     */
    @SuppressWarnings("unchecked")
    private String parseAnswer(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            logger.warn("LLMService: parseAnswer received null or empty response body.");
            return "无法解析回答：响应为空。";
        }
        try {
            logger.debug("LLMService: Attempting to parse response body for answer: {}", responseBody);
            Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                    new TypeReference<Map<String, Object>>() {
                    });
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                if (firstChoice != null) {
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    if (message != null && message.get("content") instanceof String) {
                        return (String) message.get("content");
                    } else {
                        logger.warn(
                                "LLMService: 'content' field in message is not a String or is missing for answer. Message: {}",
                                message);
                    }
                } else {
                    logger.warn("LLMService: First choice in 'choices' array is null for answer.");
                }
            } else {
                logger.warn("LLMService: 'choices' array was null or empty for answer. Response map: {}", responseMap);
            }
            return "无法从LLM响应中有效解析回答。";
        } catch (JsonProcessingException e) {
            logger.error("LLMService: JsonProcessingException while parsing LLM response for answer. Response body: {}",
                    responseBody, e);
            return "解析LLM回答时出现JSON处理错误。";
        } catch (Exception e) {
            logger.error("LLMService: Unexpected exception while parsing LLM response for answer. Response body: {}",
                    responseBody, e);
            return "解析LLM回答时出现未知错误。";
        }
    }
}
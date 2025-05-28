import request from "@/utils/request"; // 确保这是您项目中封装axios的正确路径

const llmApi = {
  /**
   * 调用后端LLM服务从文本中提取知识图谱数据
   * @param {Object} params 参数对象
   * @param {string} params.text 用户输入的文本
   * @param {string} [params.domainId] (可选) 当前图谱领域的ID
   * @returns {Promise} Axios Promise对象，期望返回 { nodes: [], links: [] }
   */
  extractKnowledge: params => {
    // 后端 LLMController 的 extractTriples 方法期望一个 Map<String, String> payload
    // 其中应包含 "text" 键。如果需要 domainId，也应加入。
    const payload = {
      text: params.text
    };
    if (params.domainId) {
      payload.domainId = params.domainId; // 如果后端需要，则添加 domainId
    }

    return request({
      url: "/api/llm/extract", // 后端API端点
      method: "post",
      headers: {
        "Content-Type": "application/json" // 确保后端能正确解析JSON
      },
      data: payload // 发送包含 text (和可选 domainId) 的JSON对象
    });
  }
  // 如果未来有其他LLM相关API，可以在此对象中继续添加
};

export default llmApi;

<!--
 * @Description: kgBuilder
 * @Author: tanc
 * @Date: 2021-12-26 16:50:07
 * @LastEditors: Please set LastEditors
 * @LastEditTime: 2022-03-29 11:23:25
-->
<template>
  <div class="kg-main-container">
    <KGHeader />
    <div class="kg-content-area">
      <KGBuilderEnhanced ref="kgBuilderEnhanced" />
    </div>
  </div>
</template>

<script>
import KGHeader from "@/components/KGHeader.vue";
import KGBuilderEnhanced from "@/components/KGBuilderEnhanced.vue";
import { EventBus } from "@/utils/event-bus.js"; // 确保EventBus路径正确

export default {
  name: "MainKGView", // 您可以为此视图组件指定一个新名称
  components: {
    KGHeader,
    KGBuilderEnhanced
  },
  data() {
    return {
      // 此处可以保留或添加此视图级别可能需要的任何数据
      // 但大多数图谱相关的状态和逻辑现在应由 KGBuilderEnhanced 处理
    };
  },
  mounted() {
    // 监听由Header触发的LLM图谱生成请求
    EventBus.$on("generate-graph-via-llm", this.handleLLMGenerationRequest);
  },
  beforeDestroy() {
    // 组件销毁前移除事件监听
    EventBus.$off("generate-graph-via-llm", this.handleLLMGenerationRequest);
  },
  methods: {
    handleLLMGenerationRequest(text) {
      // 调用 KGBuilderEnhanced 组件的方法来处理LLM生成的文本
      if (this.$refs.kgBuilderEnhanced) {
        this.$refs.kgBuilderEnhanced.initiateLLMPreview(text);
      } else {
        console.error("KGBuilderEnhanced component not found.");
        // 可以在这里添加用户提示，例如稍后再试
      }
    }
    // 移除所有旧的 methods，例如 initGraph, updateGraph, createNode, deleteNode, 等等。
    // 这些功能现在由 KGBuilderEnhanced.vue 内部管理。
  }
  // 移除旧的 created, filters, 等生命周期钩子和选项，除非您确认它们对于新结构仍然是必需的。
};
</script>

<style>
/* 移除所有旧的CSS规则 */

/* 为新布局添加一些基本样式 (如果需要) */
.kg-main-container {
  display: flex;
  flex-direction: column;
  height: 100vh; /* 或适应您应用整体布局的高度 */
  overflow: hidden;
}

.kg-content-area {
  flex-grow: 1; /* 使内容区域占据剩余空间 */
  position: relative; /* 如果 KGBuilderEnhanced 内部有绝对定位的元素 */
  overflow: hidden; /* 防止内部内容溢出导致滚动条 */
}

/* 您可以根据 KGHeader 和 KGBuilderEnhanced 的实际渲染情况和需求添加更多样式 */
/* 例如，如果 KGHeader 高度固定，可以为 kg-content-area 设置 calc(100vh - HeaderHeight) */
</style>

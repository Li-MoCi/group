<!--
 * @Description: file content
 * @Author: your name
 * @Date: 2022-01-01 12:53:00
 * @LastEditors: your name
 * @LastEditTime: 2022-01-01 13:43:38
-->
<template>
  <div class="menu">
    <nav class="nav" id="topnav">
      <h1 class="logo"><a href="/">֪KGBulider</a></h1>
      <div class="llm-input-area">
        <el-input
          type="textarea"
          v-model="llmInputText"
          placeholder="输入文本描述生成图谱..."
          rows="2"
          class="llm-textarea"
        ></el-input>
        <el-button
          @click="triggerLLMGeneration"
          type="primary"
          class="llm-button"
          >生成图谱</el-button
        >
      </div>
    </nav>
  </div>
</template>

<script>
import { EventBus } from "@/utils/event-bus.js"; // 假设EventBus路径

export default {
  props: {
    // data: Object, // 旧prop，移除
  },
  data() {
    return {
      // navList: [], // 移除旧的navList
      llmInputText: ""
    };
  },
  // mounted() {}, // 旧的，移除
  // components: {}, // 旧的，移除
  methods: {
    triggerLLMGeneration() {
      if (!this.llmInputText.trim()) {
        // Можно добавить $message если Element UI глобально доступен или через this.$root
        alert("请输入文本内容！"); // 简单提示，后续可优化
        return;
      }
      EventBus.$emit("generate-graph-via-llm", this.llmInputText);
      // this.llmInputText = ""; // 根据需求决定是否清空
    }
    // selectStyle(nav) {}, // 移除旧方法
    // outStyle(nav) {}, // 移除旧方法
    // changeIcon() {}, // 移除旧方法
    // searchActive() {}, // 移除旧方法
    // clickNav(nav) {}, // 移除旧方法
  }
};
</script>
<style>
#topnav_current {
  color: #00a7eb;
}
.menu {
  height: auto; /* 调整为auto以适应内容 */
  min-height: 76px;
  width: 100%;
  background-color: #000;
  padding: 10px 0; /* 增加一些内边距 */
  box-sizing: border-box;
}
.nav {
  /* height: 80px; */ /* 由内容决定高度 */
  width: 100%;
  margin: 0 auto;
  display: flex; /* 使用flex布局 */
  align-items: center; /* 垂直居中 */
  justify-content: space-between; /* 两端对齐 */
  padding: 0 20px; /* 增加左右内边距 */
  box-sizing: border-box;
}
.nav li {
  float: left;
  position: relative;
}
.nav li a {
  color: #bdbdbd;
  padding: 0 10px;
  display: inline-block;
  text-decoration: none;
}
.nav li a:hover {
  color: #fff;
}
.nav li .sub-nav {
  position: absolute;
  top: 30px;
  width: 120px;
  background: #fff;
  left: -20px; /* display: none;  */
  z-index: 9999;
}
.nav li .sub-nav li {
  clear: left;
  height: 20px;
  line-height: 35px;
  position: relative;
  width: 200px;
  padding: 5px 20px;
}
.nav li .sub-nav li a {
  font-size: 15px;
  font-weight: 400;
  color: #404040;
  line-height: 28px;
}
.nav li .sub-nav li a:hover {
  color: #000;
  border-left: 2px solid #000;
}
.a_active {
  color: #00a7eb !important;
}
.logo {
  /* float: left; */ /* 移除浮动 */
  /* margin-left: 70px; */
  /* width: 260px; */
  font-size: 26px;
  margin-right: 20px; /* 与输入区域的间隔 */
}
.logo a {
  color: #00a7eb;
  text-decoration: none;
}
.hometitle {
  font-size: 18px;
  color: #282828;
  font-weight: 600;
  margin: 0;
  text-transform: uppercase;
  padding-bottom: 15px;
  margin-bottom: 25px;
  position: relative;
}
.hometitle:after {
  content: "";
  background-color: #282828;
  left: 0;
  width: 50px;
  height: 2px;
  bottom: 0;
  position: absolute;
  -webkit-transition: 0.5s;
  -moz-transition: 0.5s;
  -ms-transition: 0.5s;
  -o-transition: 0.5s;
  transition: 0.5s;
}

.llm-input-area {
  display: flex;
  align-items: center;
  gap: 10px; /* 输入框和按钮之间的间隔 */
  flex-grow: 1; /* 占据剩余空间 */
  max-width: 700px; /* 或者一个合适的宽度 */
}

.llm-textarea {
  flex-grow: 1;
}

.llm-button {
  /* 根据需要调整按钮样式 */
}
</style>

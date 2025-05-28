<template>
  <div class="kg-builder-base">
    <!-- 主图谱容器 -->
    <div class="graph-container" ref="graphContainer">
      <div class="graph-canvas"></div>
      <div class="graph-overlay"></div>
    </div>
  </div>
</template>

<script>
import * as d3 from "d3";
import { debounce } from "lodash";

export default {
  name: "KGBuilderBase",

  data() {
    return {
      // 图谱数据
      graph: {
        nodes: [],
        links: []
      },

      // D3相关
      svg: null,
      simulation: null,
      zoom: null,

      // 配置选项
      defaultNodeStyle: {
        radius: 30,
        color: "#6fce7a",
        shape: "circle",
        strokeWidth: 2,
        strokeColor: "#fff"
      },

      defaultLinkStyle: {
        color: "#999",
        width: 1,
        opacity: 0.6,
        arrowSize: 10,
        labelSize: 12
      }
    };
  },

  mounted() {
    this.initGraph();
    this.initEvents();
  },

  methods: {
    // 初始化图谱
    initGraph() {
      const container = this.$refs.graphContainer;
      const width = container.clientWidth;
      const height = container.clientHeight;

      // 创建SVG
      this.svg = d3
        .select(container)
        .select(".graph-canvas")
        .append("svg")
        .attr("width", width)
        .attr("height", height);

      // 创建力导向图
      this.simulation = d3
        .forceSimulation()
        .force(
          "link",
          d3
            .forceLink()
            .id(d => d.id)
            .distance(100)
        )
        .force("charge", d3.forceManyBody().strength(-800))
        .force("center", d3.forceCenter(width / 2, height / 2))
        .force("collision", d3.forceCollide().radius(50));

      // 初始化缩放行为
      this.zoom = d3
        .zoom()
        .scaleExtent([0.1, 4])
        .on("zoom", this.handleZoom);

      this.svg.call(this.zoom);
    },

    // 事件处理
    initEvents() {
      window.addEventListener("resize", debounce(this.handleResize, 250));
    },

    // 更新图谱
    updateGraph() {
      // 更新节点
      const nodes = this.svg
        .selectAll(".node")
        .data(this.graph.nodes, d => d.id);

      // 节点进入
      const nodesEnter = nodes
        .enter()
        .append("g")
        .attr("class", "node")
        .call(
          d3
            .drag()
            .on("start", this.dragStarted)
            .on("drag", this.dragged)
            .on("end", this.dragEnded)
        );

      nodesEnter
        .append("circle")
        .attr("r", d => d.radius || this.defaultNodeStyle.radius)
        .attr("fill", d => d.color || this.defaultNodeStyle.color)
        .attr("stroke", d => d.strokeColor || this.defaultNodeStyle.strokeColor)
        .attr(
          "stroke-width",
          d => d.strokeWidth || this.defaultNodeStyle.strokeWidth
        );

      nodesEnter
        .append("text")
        .attr("dy", ".35em")
        .attr("text-anchor", "middle")
        .text(d => d.name)
        .attr("font-size", "12px")
        .attr("fill", "#333");

      // 节点更新
      const nodesUpdate = nodes.merge(nodesEnter);
      nodesUpdate
        .select("circle")
        .attr("r", d => d.radius || this.defaultNodeStyle.radius)
        .attr("fill", d => d.color || this.defaultNodeStyle.color);

      nodesUpdate.select("text").text(d => d.name);

      // 节点退出
      nodes.exit().remove();

      // 更新连线
      const links = this.svg
        .selectAll(".link")
        .data(this.graph.links, d => `${d.source.id}-${d.target.id}`);

      // 连线进入
      const linksEnter = links
        .enter()
        .append("g")
        .attr("class", "link");

      linksEnter
        .append("path")
        .attr("stroke", d => d.color || this.defaultLinkStyle.color)
        .attr("stroke-width", d => d.width || this.defaultLinkStyle.width)
        .attr("stroke-opacity", d => d.opacity || this.defaultLinkStyle.opacity)
        .attr("fill", "none");

      linksEnter
        .append("text")
        .attr("dy", -5)
        .attr("text-anchor", "middle")
        .text(d => d.label)
        .attr("font-size", d => d.labelSize || this.defaultLinkStyle.labelSize)
        .attr("fill", "#666");

      // 连线更新
      const linksUpdate = links.merge(linksEnter);
      linksUpdate
        .select("path")
        .attr("stroke", d => d.color || this.defaultLinkStyle.color);

      linksUpdate.select("text").text(d => d.label);

      // 连线退出
      links.exit().remove();

      // 更新力导向图
      this.simulation.nodes(this.graph.nodes);
      this.simulation.force("link").links(this.graph.links);
      this.simulation.alpha(1).restart();

      // 添加tick事件处理
      this.simulation.on("tick", () => {
        // 更新节点位置
        nodesUpdate.attr("transform", d => `translate(${d.x},${d.y})`);

        // 更新连线位置
        linksUpdate.select("path").attr("d", d => {
          const dx = d.target.x - d.source.x;
          const dy = d.target.y - d.source.y;
          const dr = Math.sqrt(dx * dx + dy * dy);
          return `M${d.source.x},${d.source.y}A${dr},${dr} 0 0,1 ${d.target.x},${d.target.y}`;
        });

        // 更新连线文本位置
        linksUpdate.select("text").attr("transform", d => {
          const dx = d.target.x - d.source.x;
          const dy = d.target.y - d.source.y;
          const angle = (Math.atan2(dy, dx) * 180) / Math.PI;
          const x = (d.source.x + d.target.x) / 2;
          const y = (d.source.y + d.target.y) / 2;
          return `translate(${x},${y}) rotate(${angle})`;
        });
      });
    },

    // 拖拽事件处理
    dragStarted(event, d) {
      if (!event.active) this.simulation.alphaTarget(0.3).restart();
      d.fx = d.x;
      d.fy = d.y;
    },

    dragged(event, d) {
      d.fx = event.x;
      d.fy = event.y;
    },

    dragEnded(event, d) {
      if (!event.active) this.simulation.alphaTarget(0);
      d.fx = null;
      d.fy = null;
    },

    // 处理缩放事件
    handleZoom(event) {
      const { transform } = event;
      this.svg.selectAll("g").attr("transform", transform);
    },

    // 处理窗口大小变化
    handleResize() {
      const container = this.$refs.graphContainer;
      const width = container.clientWidth;
      const height = container.clientHeight;

      this.svg.attr("width", width).attr("height", height);
      this.simulation
        .force("center", d3.forceCenter(width / 2, height / 2))
        .restart();
    },

    // 清理
    beforeDestroy() {
      window.removeEventListener("resize", this.handleResize);
      this.simulation.stop();
    }
  }
};
</script>

<style lang="scss" scoped>
.kg-builder-base {
  position: relative;
  width: 100%;
  height: 100%;

  .graph-container {
    width: 100%;
    height: 100%;
    background: #f5f5f5;
    position: relative;

    .graph-canvas {
      width: 100%;
      height: 100%;
    }

    .graph-overlay {
      position: absolute;
      top: 0;
      left: 0;
      pointer-events: none;
    }
  }
}

// 节点样式
.node {
  cursor: pointer;
}

// 连线样式
.link {
  stroke: #999;
  stroke-opacity: 0.6;
}
</style>

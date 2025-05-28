<template>
  <div class="kg-builder-enhanced">
    <!-- 主图谱容器 -->
    <div class="graph-container" ref="graphContainer">
      <div class="graph-canvas"></div>
      <!-- SVG will be appended here -->
      <!-- Overlay for context menus or other elements if needed -->
      <div class="graph-overlay">
        <!-- Context Menus were here, temporarily removed -->
      </div>
    </div>

    <!-- 工具栏 (保留图谱自身操作，如视图控制、样式) -->
    <div v-if="!isPreview" class="toolbar-container-enhanced">
      <div class="view-controls">
        <el-button-group>
          <el-button @click="zoomIn" icon="el-icon-zoom-in" size="mini"
            >放大</el-button
          >
          <el-button @click="zoomOut" icon="el-icon-zoom-out" size="mini"
            >缩小</el-button
          >
          <el-button @click="resetView" icon="el-icon-refresh" size="mini"
            >重置</el-button
          >
        </el-button-group>
      </div>
    </div>

    <!-- 预览遮罩层和按钮 -->
    <div v-if="isPreview" class="preview-overlay">
      <div class="preview-actions">
        <h3>图谱预览</h3>
        <p>以下是由文本生成的图谱。是否应用到当前画布？</p>
        <el-button type="primary" @click="applyPreviewGraph"
          >应用到画布</el-button
        >
        <el-button @click="cancelPreview">取消</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import * as d3 from "d3";
import { debounce } from "lodash";
import StyleTemplateManager from "./StyleTemplateManager.vue";
import GraphUtils from "../utils/GraphUtils";
import StyleUtils from "../utils/StyleUtils";
import { EventBus } from "@/utils/event-bus.js";

export default {
  name: "KGBuilderEnhanced",
  components: {
    StyleTemplateManager
  },
  props: {
    domain: {
      type: String,
      required: true
    },
    domainId: {
      type: [String, Number]
      // required: true,
    },
    initData: {
      type: Object,
      default: () => ({ nodes: [], links: [] })
    }
  },
  data() {
    return {
      graph: { nodes: [], links: [] },
      svg: null,
      simulation: null,
      zoom: null,
      nodeGroup: null,
      linkGroup: null,
      labelGroup: null,
      ringMenuGroup: null,
      selectedNodes: new Set(),
      selectedLinks: new Set(),
      defaultNodeStyle: {
        radius: 20,
        color: "#6fce7a",
        shape: "circle",
        strokeWidth: 1,
        strokeColor: "#fff"
      },
      defaultLinkStyle: {
        color: "#999",
        width: 1.5,
        opacity: 0.8,
        arrowSize: 8,
        labelSize: 10
      },
      ringMenuConfig: [
        {
          id: "edit",
          label: "编辑",
          icon: "el-icon-edit",
          action: "EDIT_NODE"
        },
        {
          id: "link",
          label: "连线",
          icon: "el-icon-share",
          action: "START_LINK"
        },
        {
          id: "expand",
          label: "展开",
          icon: "el-icon-folder-opened",
          action: "EXPAND_NODE"
        },
        {
          id: "delete",
          label: "删除",
          icon: "el-icon-delete",
          action: "DELETE_NODE"
        }
      ],
      activeNodeForForm: null,
      isAddingLink: false,
      linkSourceNodeData: null,
      activeRingMenuNodeId: null,
      contextMenuVisible: false, // Kept for future use
      contextMenuType: "",
      contextMenuPosition: { x: 0, y: 0 },
      contextMenuNode: null,
      contextMenuLink: null,
      isPreview: false,
      previewGraphData: { nodes: [], links: [] }
    };
  },
  watch: {
    initData: {
      deep: true,
      immediate: true,
      handler(newData) {
        if (newData && (newData.nodes || newData.links)) {
          this.graph.nodes = JSON.parse(JSON.stringify(newData.nodes || []));
          this.graph.links = JSON.parse(JSON.stringify(newData.links || []));
          this.selectedNodes.clear();
          this.selectedLinks.clear();
          if (this.svg) {
            this.$nextTick(() => {
              this.updateGraph();
              if (this.simulation && this.simulation.alpha() < 0.1) {
                this.simulation.alpha(0.3).restart();
              }
            });
          }
        }
      }
    }
  },
  mounted() {
    this.initGraph();
    this.initEvents();
    this.initAdvancedFeatures();
  },
  beforeDestroy() {
    document.removeEventListener("keydown", this.handleKeydown);
    window.removeEventListener("resize", this.handleResize);
    if (this.simulation) {
      this.simulation.stop();
    }
  },
  methods: {
    initGraph() {
      const container = this.$refs.graphContainer;
      if (!container) return;
      const width = container.clientWidth;
      const height = container.clientHeight;

      d3.select(container)
        .select(".graph-canvas svg")
        .remove();

      this.svg = d3
        .select(container)
        .select(".graph-canvas")
        .append("svg")
        .attr("width", width)
        .attr("height", height)
        .on("contextmenu", this.handleCanvasContextMenu) // Context menu logic can be re-enabled later
        .on("click", this.handleCanvasClick);

      this.linkGroup = this.svg.append("g").attr("class", "links");
      this.nodeGroup = this.svg.append("g").attr("class", "nodes");
      this.labelGroup = this.svg.append("g").attr("class", "labels");
      this.ringMenuGroup = this.svg.append("g").attr("class", "ring-menus");

      StyleUtils.createArrowMarker(
        this.svg,
        this.defaultLinkStyle.color,
        this.defaultLinkStyle.arrowSize
      );

      this.simulation = d3
        .forceSimulation()
        .force(
          "link",
          d3
            .forceLink()
            .id(d => d.uuid)
            .distance(100)
            .strength(0.5)
        )
        .force("charge", d3.forceManyBody().strength(-400))
        .force("center", d3.forceCenter(width / 2, height / 2))
        .force(
          "collision",
          d3
            .forceCollide()
            .radius(d => (d.r || this.defaultNodeStyle.radius) + 10)
        );

      this.zoom = d3
        .zoom()
        .scaleExtent([0.1, 4])
        .on("zoom", event => {
          const transform = event.transform;
          this.nodeGroup.attr("transform", transform);
          this.linkGroup.attr("transform", transform);
          this.labelGroup.attr("transform", transform);
          this.ringMenuGroup.attr("transform", transform);
        });
      this.svg.call(this.zoom);
      this.updateGraph();
    },

    updateGraph() {
      if (!this.svg || !this.simulation || !this.graph) return;

      const graphNodes = this.isPreview
        ? this.previewGraphData.nodes
        : this.graph.nodes;
      const graphLinks = this.isPreview
        ? this.previewGraphData.links
        : this.graph.links;

      this.simulation.nodes(graphNodes);
      this.simulation.force("link").links(graphLinks);

      const links = this.linkGroup
        .selectAll("path.link")
        .data(
          graphLinks,
          d =>
            d.uuid ||
            `${d.source.uuid || d.source}-${d.target.uuid || d.target}`
        );
      links.exit().remove();
      const linksEnter = links
        .enter()
        .append("path")
        .attr("class", "link")
        .attr("stroke", d => d.color || this.defaultLinkStyle.color)
        .attr("stroke-width", d => d.width || this.defaultLinkStyle.width)
        .attr("stroke-opacity", d => d.opacity || this.defaultLinkStyle.opacity)
        .attr("marker-end", "url(#arrow)")
        .on("contextmenu", (event, d) => this.handleLinkContextMenu(event, d))
        .on("click", (event, d) => this.handleLinkClick(event, d));

      this.linksUpdate = links
        .merge(linksEnter)
        .classed("selected", d => this.selectedLinks.has(d.uuid));

      const nodes = this.nodeGroup
        .selectAll("g.node")
        .data(graphNodes, d => d.uuid);
      nodes.exit().remove();
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
        )
        .on("click", (event, d) => this.handleNodeClick(event, d))
        .on("contextmenu", (event, d) => this.handleNodeContextMenu(event, d));

      nodesEnter
        .append("circle")
        .attr("r", d => d.r || this.defaultNodeStyle.radius)
        .attr("fill", d => d.color || this.defaultNodeStyle.color)
        .attr("stroke", d => d.strokeColor || this.defaultNodeStyle.strokeColor)
        .attr(
          "stroke-width",
          d => d.strokeWidth || this.defaultNodeStyle.strokeWidth
        );

      this.nodesUpdate = nodes.merge(nodesEnter);
      this.nodesUpdate
        .select("circle")
        .attr("r", d => d.r || this.defaultNodeStyle.radius)
        .attr("fill", d => d.color || this.defaultNodeStyle.color)
        .classed("selected", d => this.selectedNodes.has(d.uuid))
        .classed(
          "linking-source",
          d =>
            this.isAddingLink &&
            this.linkSourceNodeData &&
            d.uuid === this.linkSourceNodeData.uuid
        );

      this.labelGroup.selectAll("text").remove();

      this.labelGroup
        .selectAll("text.node-label")
        .data(graphNodes, d => d.uuid)
        .enter()
        .append("text")
        .attr("class", "node-label")
        .attr("text-anchor", "middle")
        .attr("dy", ".35em")
        .text(d => d.name);

      this.labelGroup
        .selectAll("text.link-label")
        .data(
          graphLinks,
          d =>
            d.uuid ||
            `${d.source.uuid || d.source}-${d.target.uuid || d.target}`
        )
        .enter()
        .append("text")
        .attr("class", "link-label")
        .attr("text-anchor", "middle")
        .attr("dy", "-3px")
        .text(d => d.name || d.label);

      this.simulation.on("tick", () => {
        this.nodesUpdate.attr("transform", d => `translate(${d.x},${d.y})`);
        this.linksUpdate.attr("d", d =>
          GraphUtils.calculateCurvedPath(d.source, d.target)
        );

        this.labelGroup
          .selectAll("text.node-label")
          .attr("x", d => d.x)
          .attr("y", d => d.y);

        this.labelGroup
          .selectAll("text.link-label")
          .attr("x", d => (d.source.x + d.target.x) / 2)
          .attr("y", d => (d.source.y + d.target.y) / 2);

        if (this.activeRingMenuNodeId) {
          const node = graphNodes.find(
            n => n.uuid === this.activeRingMenuNodeId
          );
          if (node) this.renderNodeRingMenus(node);
          else this.clearRingMenus();
        } else {
          this.clearRingMenus();
        }
      });
      if (this.simulation.alpha() < 0.1) this.simulation.alpha(0.3).restart(); // Ensure simulation runs
    },

    dragStarted(event, d) {
      if (!event.active) this.simulation.alphaTarget(0.3).restart();
      d.fx = d.x;
      d.fy = d.y;
      this.clearRingMenus();
      this.closeContextMenu();
    },
    dragged(event, d) {
      d.fx = event.x;
      d.fy = event.y;
    },
    dragEnded(event, d) {
      if (!event.active) this.simulation.alphaTarget(0);
      // this.$emit('node-dragged', { uuid: d.uuid, fx: d.fx, fy: d.fy }); // Emit if parent needs to save coordinates
    },

    handleNodeClick(event, d) {
      event.stopPropagation();
      this.closeContextMenu();

      if (this.isAddingLink) {
        if (
          this.linkSourceNodeData &&
          this.linkSourceNodeData.uuid !== d.uuid
        ) {
          this.$emit("create-link-request", {
            sourceId: this.linkSourceNodeData.uuid,
            targetId: d.uuid
          });
          this.isAddingLink = false;
          this.linkSourceNodeData = null;
          this.nodesUpdate.selectAll("circle").classed("linking-source", false);
        } else if (
          this.linkSourceNodeData &&
          this.linkSourceNodeData.uuid === d.uuid
        ) {
          this.$message.info("不能连接节点到自身。");
        }
        return;
      }

      if (this.activeRingMenuNodeId === d.uuid) {
        this.clearRingMenus();
      } else {
        this.activeRingMenuNodeId = d.uuid;
        this.renderNodeRingMenus(d);
      }
    },

    handleLinkClick(event, d) {
      event.stopPropagation();
      this.closeContextMenu();
      this.clearRingMenus();
      if (this.selectedLinks.has(d.uuid)) {
        this.selectedLinks.delete(d.uuid);
      } else {
        this.selectedLinks.clear();
        this.selectedLinks.add(d.uuid);
      }
      this.updateGraph();
      console.log("Selected link:", d);
    },

    handleCanvasClick() {
      this.clearRingMenus();
      this.closeContextMenu();
      this.selectedNodes.clear();
      this.selectedLinks.clear();
      this.isAddingLink = false;
      this.linkSourceNodeData = null;
      this.nodesUpdate.selectAll("circle").classed("linking-source", false);
      this.updateGraph();
    },

    renderNodeRingMenus(nodeData) {
      this.ringMenuGroup.selectAll("*").remove();
      if (
        !nodeData ||
        !this.activeRingMenuNodeId ||
        nodeData.uuid !== this.activeRingMenuNodeId
      ) {
        return;
      }
      const menuRadius = (nodeData.r || this.defaultNodeStyle.radius) + 30;
      const numItems = this.ringMenuConfig.length;
      const angleStep = (2 * Math.PI) / numItems;

      this.ringMenuConfig.forEach((item, i) => {
        const angle = i * angleStep - Math.PI / 2;
        const x = nodeData.x + menuRadius * Math.cos(angle);
        const y = nodeData.y + menuRadius * Math.sin(angle);
        const menuItem = this.ringMenuGroup
          .append("g")
          .attr("class", "ring-menu-item")
          .attr("transform", `translate(${x},${y})`)
          .on("click", event => {
            event.stopPropagation();
            this.handleRingMenuClick(item.action, nodeData);
          });
        menuItem
          .append("circle")
          .attr("r", 18)
          .attr("fill", "#fff")
          .attr("stroke", "#ccc");
        menuItem
          .append("text")
          .attr("text-anchor", "middle")
          .attr("dy", ".35em")
          .text(item.label)
          .style("font-size", "10px");
      });
    },
    clearRingMenus() {
      this.activeRingMenuNodeId = null;
      this.ringMenuGroup.selectAll("*").remove();
    },

    handleRingMenuClick(action, nodeData) {
      this.clearRingMenus();
      switch (action) {
        case "EDIT_NODE":
          this.$emit("edit-node-form-request", nodeData);
          break;
        case "DELETE_NODE":
          this.$emit("delete-node-request", nodeData.uuid);
          break;
        case "START_LINK":
          this.isAddingLink = true;
          this.linkSourceNodeData = nodeData;
          this.nodesUpdate.selectAll("circle").classed("linking-source", false);
          const activeNodeCircle = this.nodesUpdate
            .filter(d => d.uuid === nodeData.uuid)
            .select("circle");
          if (activeNodeCircle)
            activeNodeCircle.classed("linking-source", true);
          this.$message.info(
            `开始连线，起点: ${nodeData.name}。请点击目标节点。`
          );
          break;
        case "EXPAND_NODE":
          this.$emit("expand-node-request", nodeData.uuid);
          break;
        default:
          console.warn("Unknown ring menu action:", action);
      }
    },

    handleCanvasContextMenu(event) {
      event.preventDefault();
      this.clearRingMenus();
      // Future: enable actual context menu logic here
      // this.contextMenuPosition = { x: event.offsetX, y: event.offsetY };
      // this.contextMenuType = 'blank';
      // this.contextMenuVisible = true;
      this.$message.info("画布右键菜单（暂未实现具体操作）");
    },
    handleNodeContextMenu(event, d) {
      event.preventDefault();
      event.stopPropagation();
      if (
        this.activeRingMenuNodeId === d.uuid &&
        this.ringMenuGroup.selectAll("*").size() > 0
      ) {
        // If ring menu is already visible for this node, do nothing or hide it
        // this.clearRingMenus();
      } else {
        this.clearRingMenus(); // Clear any other ring menu
        this.activeRingMenuNodeId = d.uuid;
        this.renderNodeRingMenus(d); // Show ring menu as primary node interaction
      }
    },
    handleLinkContextMenu(event, d) {
      event.preventDefault();
      event.stopPropagation();
      this.clearRingMenus();
      // Future: enable actual context menu logic here
      // this.contextMenuPosition = { x: event.offsetX, y: event.offsetY };
      // this.contextMenuLink = d;
      // this.contextMenuType = 'link';
      // this.contextMenuVisible = true;
      this.$message.info(
        `关系 ${d.name || d.label} 右键菜单（暂未实现具体操作）`
      );
    },
    closeContextMenu() {
      // Kept for future use
      this.contextMenuVisible = false;
    },
    handleContextMenuAction(action, data) {
      // Kept for future use
      this.closeContextMenu();
      // Handle actions when context menus are re-enabled
    },

    initiateLLMPreview(text) {
      this.$emit("llm-extract-request", text);
    },
    showLLMPreview(triples) {
      const { nodes: llmNodes, links: llmLinks } = this.triplesToGraph(triples);
      this.previewGraphData.nodes = llmNodes;
      this.previewGraphData.links = llmLinks;
      this.isPreview = true;
      this.updateGraph();
    },
    triplesToGraph(triples) {
      const nodes = new Map();
      const links = [];
      let idCounter = Date.now();

      (triples || []).forEach(triple => {
        let headNode = nodes.get(triple.head);
        if (!headNode) {
          headNode = {
            uuid: `${idCounter++}_${triple.head.replace(/\s+/g, "_")}`,
            name: triple.head,
            r: this.defaultNodeStyle.radius,
            color: this.defaultNodeStyle.color
          };
          nodes.set(triple.head, headNode);
        }
        let tailNode = nodes.get(triple.tail);
        if (!tailNode) {
          tailNode = {
            uuid: `${idCounter++}_${triple.tail.replace(/\s+/g, "_")}`,
            name: triple.tail,
            r: this.defaultNodeStyle.radius,
            color: this.defaultNodeStyle.color
          };
          nodes.set(triple.tail, tailNode);
        }
        links.push({
          uuid: `${idCounter++}_link`,
          source: headNode,
          target: tailNode,
          name: triple.relation,
          label: triple.relation
        });
      });
      return { nodes: Array.from(nodes.values()), links };
    },
    applyPreviewGraph() {
      this.graph.nodes = JSON.parse(
        JSON.stringify(this.previewGraphData.nodes)
      );
      this.graph.links = JSON.parse(
        JSON.stringify(this.previewGraphData.links)
      );
      this.isPreview = false;
      this.updateGraph();
      this.$message.success("LLM图谱已应用");
      this.$emit("llm-graph-applied", {
        domain: this.domain,
        domainId: this.domainId,
        nodes: this.graph.nodes,
        links: this.graph.links
      });
    },
    cancelPreview() {
      this.isPreview = false;
      this.updateGraph();
    },

    addOrUpdateNode(nodeData) {
      const existingNodeIndex = this.graph.nodes.findIndex(
        n => n.uuid === nodeData.uuid
      );
      if (existingNodeIndex > -1) {
        const existingNode = this.graph.nodes[existingNodeIndex];
        const updatedNode = {
          ...existingNode,
          ...nodeData,
          fx: nodeData.fx !== undefined ? nodeData.fx : existingNode.fx,
          fy: nodeData.fy !== undefined ? nodeData.fy : existingNode.fy
        };
        this.graph.nodes.splice(existingNodeIndex, 1, updatedNode);
      } else {
        this.graph.nodes.push({ ...this.defaultNodeStyle, ...nodeData });
      }
      this.updateGraph();
    },
    removeNodeAndAssociatedLinks(nodeId) {
      this.graph.nodes = this.graph.nodes.filter(n => n.uuid !== nodeId);
      this.graph.links = this.graph.links.filter(l => {
        // Check if source or target is an object with uuid or a direct string/number id
        const sourceId =
          typeof l.source === "object" ? l.source.uuid : l.source;
        const targetId =
          typeof l.target === "object" ? l.target.uuid : l.target;
        return sourceId !== nodeId && targetId !== nodeId;
      });
      this.updateGraph();
    },
    updateLinkLabel(linkUuid, newName) {
      const link = this.graph.links.find(l => l.uuid === linkUuid);
      if (link) {
        link.name = newName;
        link.label = newName;
      }
      this.updateGraph();
    },
    addLink(linkData) {
      const sourceNode = this.graph.nodes.find(
        n => n.uuid === (linkData.source?.uuid || linkData.sourceId)
      );
      const targetNode = this.graph.nodes.find(
        n => n.uuid === (linkData.target?.uuid || linkData.targetId)
      );
      if (sourceNode && targetNode) {
        this.graph.links.push({
          uuid: linkData.uuid || `${Date.now()}_link`, // Ensure UUID
          source: sourceNode,
          target: targetNode,
          name: linkData.name || linkData.ship,
          label: linkData.label || linkData.ship
        });
        this.updateGraph();
      } else {
        console.error(
          "Cannot add link, source or target node not found",
          linkData
        );
        this.$message.error("添加连线失败：未找到源节点或目标节点。");
      }
    },
    mergeGraphData(newNodes, newLinks) {
      if (newNodes && newNodes.length > 0) {
        newNodes.forEach(newNode => {
          if (!this.graph.nodes.find(n => n.uuid === newNode.uuid)) {
            this.graph.nodes.push({ ...this.defaultNodeStyle, ...newNode });
          } else {
            const existing = this.graph.nodes.find(
              n => n.uuid === newNode.uuid
            );
            Object.assign(existing, newNode);
          }
        });
      }
      if (newLinks && newLinks.length > 0) {
        newLinks.forEach(newLink => {
          if (!this.graph.links.find(l => l.uuid === newLink.uuid)) {
            const sourceNode = this.graph.nodes.find(
              n => n.uuid === (newLink.source.uuid || newLink.source)
            );
            const targetNode = this.graph.nodes.find(
              n => n.uuid === (newLink.target.uuid || newLink.target)
            );
            if (sourceNode && targetNode) {
              this.graph.links.push({
                ...newLink,
                source: sourceNode,
                target: targetNode,
                name: newLink.name || newLink.label // Ensure name for display
              });
            }
          } else {
            const existingLink = this.graph.links.find(
              l => l.uuid === newLink.uuid
            );
            Object.assign(existingLink, newLink);
            if (!existingLink.name && existingLink.label)
              existingLink.name = existingLink.label; // Ensure name
          }
        });
      }
      this.updateGraph();
    },
    startLinkingFromNode(nodeData) {
      this.isAddingLink = true;
      this.linkSourceNodeData = nodeData;
      this.nodesUpdate.selectAll("circle").classed("linking-source", false);
      const activeNodeCircle = this.nodesUpdate
        .filter(d => d.uuid === nodeData.uuid)
        .select("circle");
      if (activeNodeCircle) activeNodeCircle.classed("linking-source", true);
      this.$message.info(`开始连线，起点: ${nodeData.name}。请点击目标节点。`);
    },
    zoomIn() {
      if (this.svg && this.zoom)
        this.svg
          .transition()
          .duration(750)
          .call(this.zoom.scaleBy, 1.2);
    },
    zoomOut() {
      if (this.svg && this.zoom)
        this.svg
          .transition()
          .duration(750)
          .call(this.zoom.scaleBy, 0.8);
    },
    resetView() {
      if (this.svg && this.zoom)
        this.svg
          .transition()
          .duration(750)
          .call(this.zoom.transform, d3.zoomIdentity);
    },
    initEvents() {
      document.addEventListener("keydown", this.handleKeydown);
      window.addEventListener("resize", this.handleResize);
    },
    initAdvancedFeatures() {
      /* For D3 filters, etc. if needed */
    },
    handleKeydown(event) {
      if (event.key === "Escape") {
        if (this.isAddingLink) {
          this.isAddingLink = false;
          this.linkSourceNodeData = null;
          if (this.nodesUpdate)
            this.nodesUpdate
              .selectAll("circle")
              .classed("linking-source", false);
          this.$message.info("取消连线。");
        }
        this.clearRingMenus();
        this.closeContextMenu();
      }
    },
    handleResize: debounce(function() {
      if (this.$refs.graphContainer) {
        // Ensure container exists before re-init
        this.initGraph();
      }
    }, 250),
    toggleNodeCollapse(nodeData) {
      this.$emit("toggle-node-collapse-request", nodeData.uuid);
    }
  }
};
</script>

<style lang="scss" scoped>
.kg-builder-enhanced {
  position: relative;
  width: 100%;
  height: 100%;
  background-color: #f8f9fa;
}
.graph-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  .graph-canvas {
    width: 100%;
    height: 100%;
    svg {
      display: block;
    }
  }
  .graph-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    & > * {
      pointer-events: auto;
    }
  }
}
.toolbar-container-enhanced {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  gap: 10px;
}
// Define global styles for .node g.node and .link path.link in a <style> tag not scoped, or use ::v-deep if necessary
// For scoped styles, D3 might not pick them up easily unless classes are on the SVG elements it creates.
// The following are illustrative.
::v-deep g.node {
  cursor: pointer;
  circle {
    transition: fill 0.2s ease, r 0.2s ease;
    stroke-width: 1px;
    stroke: #fff;
    &:hover {
      filter: brightness(1.1);
    }
  }
  &.selected circle {
    stroke: #007bff !important;
    stroke-width: 2.5px !important;
  }
  & .linking-source {
    // Class for circle when it's a linking source
    stroke: #ffc107 !important;
    stroke-width: 2.5px !important;
  }
}
::v-deep text.node-label {
  font-size: 10px;
  font-family: Arial, sans-serif;
  fill: #333;
  pointer-events: none;
  -webkit-user-select: none; /* Safari */
  -ms-user-select: none; /* IE 10+ */
  user-select: none; /* Standard syntax */
}
::v-deep path.link {
  fill: none;
  transition: stroke 0.2s ease, stroke-width 0.2s ease;
  &.selected {
    stroke: #007bff !important;
    stroke-width: 2.5px !important;
  }
}
::v-deep text.link-label {
  font-size: 9px;
  font-family: Arial, sans-serif;
  fill: #444;
  pointer-events: none;
  -webkit-user-select: none;
  -ms-user-select: none;
  user-select: none;
}
.ring-menu-item {
  cursor: pointer;
  circle {
    stroke: #aaa;
    stroke-width: 1px;
    fill: #f9f9f9;
    fill-opacity: 0.95;
    transition: fill 0.2s;
    &:hover {
      fill: #e0e0e0;
    }
  }
  text {
    pointer-events: none;
    font-size: 10px;
    fill: #333;
  }
}
.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  .preview-actions {
    background-color: white;
    padding: 30px 40px;
    border-radius: 8px;
    text-align: center;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    h3 {
      margin-top: 0;
      margin-bottom: 15px;
      color: #333;
    }
    p {
      margin-bottom: 25px;
      color: #555;
    }
    .el-button + .el-button {
      margin-left: 15px;
    }
  }
}
</style>

export default {
  data() {
    return {
      // 样式配置
      styleDialogVisible: false,
      styleDialogTitle: "",
      currentStyleTarget: null,

      // 选中状态
      selectedNodes: new Set(),
      selectedLinks: new Set()
    };
  },

  methods: {
    // 处理节点样式
    handleNodeStyle(command) {
      this.styleDialogTitle = "节点样式设置";
      this.currentStyleTarget = "node";
      this.styleDialogVisible = true;
    },

    // 处理连线样式
    handleLinkStyle(command) {
      this.styleDialogTitle = "关系样式设置";
      this.currentStyleTarget = "link";
      this.styleDialogVisible = true;
    },

    // 应用节点样式
    applyNodeStyle(style) {
      this.selectedNodes.forEach(nodeId => {
        const node = this.graph.nodes.find(n => n.id === nodeId);
        if (node) {
          Object.assign(node, style);
        }
      });
      this.updateGraph();
    },

    // 应用连线样式
    applyLinkStyle(style) {
      this.selectedLinks.forEach(linkId => {
        const [sourceId, targetId] = linkId.split("-");
        const link = this.graph.links.find(
          l => l.source.id === sourceId && l.target.id === targetId
        );
        if (link) {
          Object.assign(link, style);
        }
      });
      this.updateGraph();
    },

    // 选择节点
    selectNode(node, event) {
      if (event.ctrlKey || event.metaKey) {
        // 多选模式
        if (this.selectedNodes.has(node.id)) {
          this.selectedNodes.delete(node.id);
        } else {
          this.selectedNodes.add(node.id);
        }
      } else {
        // 单选模式
        this.selectedNodes.clear();
        this.selectedNodes.add(node.id);
      }
      this.updateGraph();
    },

    // 选择连线
    selectLink(link, event) {
      const linkId = `${link.source.id}-${link.target.id}`;
      if (event.ctrlKey || event.metaKey) {
        // 多选模式
        if (this.selectedLinks.has(linkId)) {
          this.selectedLinks.delete(linkId);
        } else {
          this.selectedLinks.add(linkId);
        }
      } else {
        // 单选模式
        this.selectedLinks.clear();
        this.selectedLinks.add(linkId);
      }
      this.updateGraph();
    },

    // 清除选择
    clearSelection() {
      this.selectedNodes.clear();
      this.selectedLinks.clear();
      this.updateGraph();
    },

    // 更新节点样式
    updateNodeStyles() {
      this.svg
        .selectAll(".node")
        .classed("selected", d => this.selectedNodes.has(d.id));
    },

    // 更新连线样式
    updateLinkStyles() {
      this.svg
        .selectAll(".link")
        .classed("selected", d =>
          this.selectedLinks.has(`${d.source.id}-${d.target.id}`)
        );
    }
  },

  watch: {
    selectedNodes: {
      handler() {
        this.updateNodeStyles();
      },
      deep: true
    },
    selectedLinks: {
      handler() {
        this.updateLinkStyles();
      },
      deep: true
    }
  }
};

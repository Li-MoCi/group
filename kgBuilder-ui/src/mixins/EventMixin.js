export default {
  data() {
    return {
      // 快捷键说明
      shortcutsVisible: false,
      // 拖拽模式
      dragMode: false
    };
  },

  methods: {
    // 初始化事件监听
    initEventListeners() {
      // 键盘事件
      document.addEventListener("keydown", this.handleKeydown);
      document.addEventListener("keyup", this.handleKeyup);

      // 鼠标事件
      this.svg.on("click", this.handleSvgClick);
      this.svg.on("contextmenu", this.handleContextMenu);
    },

    // 清理事件监听
    cleanupEventListeners() {
      document.removeEventListener("keydown", this.handleKeydown);
      document.removeEventListener("keyup", this.handleKeyup);
    },

    // 处理键盘按下
    handleKeydown(event) {
      // 空格：进入拖拽模式
      if (event.code === "Space") {
        event.preventDefault();
        this.enterDragMode();
      }

      // Ctrl + Z：撤销
      if (event.ctrlKey && event.code === "KeyZ") {
        event.preventDefault();
        this.undo();
      }

      // Delete：删除选中
      if (event.code === "Delete") {
        event.preventDefault();
        this.deleteSelected();
      }

      // Ctrl + G：创建分组
      if (event.ctrlKey && event.code === "KeyG") {
        event.preventDefault();
        this.createGroup();
      }

      // Ctrl + F：折叠/展开
      if (event.ctrlKey && event.code === "KeyF") {
        event.preventDefault();
        this.toggleFolding();
      }

      // Esc：清除选择
      if (event.code === "Escape") {
        event.preventDefault();
        this.clearSelection();
      }
    },

    // 处理键盘释放
    handleKeyup(event) {
      // 空格释放：退出拖拽模式
      if (event.code === "Space") {
        event.preventDefault();
        this.exitDragMode();
      }
    },

    // 处理画布点击
    handleSvgClick(event) {
      // 如果点击的是画布空白处，清除选择
      if (event.target.tagName === "svg") {
        this.clearSelection();
      }
    },

    // 处理右键菜单
    handleContextMenu(event) {
      event.preventDefault();
      // 显示自定义上下文菜单
    },

    // 进入拖拽模式
    enterDragMode() {
      this.dragMode = true;
      this.svg.style("cursor", "grab");
    },

    // 退出拖拽模式
    exitDragMode() {
      this.dragMode = false;
      this.svg.style("cursor", "default");
    },

    // 显示快捷键说明
    showShortcuts() {
      this.shortcutsVisible = true;
    },

    // 删除选中的节点和连线
    deleteSelected() {
      // 删除选中的连线
      this.graph.links = this.graph.links.filter(link => {
        const linkId = `${link.source.id}-${link.target.id}`;
        return !this.selectedLinks.has(linkId);
      });

      // 删除选中的节点
      this.graph.nodes = this.graph.nodes.filter(node => {
        return !this.selectedNodes.has(node.id);
      });

      // 清除选择
      this.clearSelection();

      // 更新图谱
      this.updateGraph();
    },

    // 创建分组
    createGroup() {
      if (this.selectedNodes.size < 2) {
        this.$message.warning("请选择至少两个节点进行分组");
        return;
      }

      const groupId = `group_${Date.now()}`;
      this.graph.groups.push({
        id: groupId,
        nodes: Array.from(this.selectedNodes),
        collapsed: false
      });

      this.updateGraph();
    },

    // 折叠/展开
    toggleFolding() {
      // 处理选中节点/分组的折叠展开
      this.selectedNodes.forEach(nodeId => {
        const group = this.graph.groups.find(g => g.nodes.includes(nodeId));
        if (group) {
          group.collapsed = !group.collapsed;
        }
      });

      this.updateGraph();
    }
  },

  mounted() {
    this.initEventListeners();
  },

  beforeDestroy() {
    this.cleanupEventListeners();
  }
};

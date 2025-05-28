export default {
  data() {
    return {
      isFullscreen: false,
      zoomScale: 1
    };
  },

  methods: {
    // 放大
    zoomIn() {
      this.zoom.scaleBy(this.svg.transition().duration(750), 1.2);
      this.zoomScale *= 1.2;
    },

    // 缩小
    zoomOut() {
      this.zoom.scaleBy(this.svg.transition().duration(750), 0.8);
      this.zoomScale *= 0.8;
    },

    // 重置视图
    resetView() {
      this.svg
        .transition()
        .duration(750)
        .call(this.zoom.transform, d3.zoomIdentity);
      this.zoomScale = 1;
    },

    // 切换全屏
    toggleFullscreen() {
      if (!this.isFullscreen) {
        this.showFullscreen();
      } else {
        this.exitFullscreen();
      }
    },

    // 进入全屏
    showFullscreen() {
      const element = this.$refs.graphContainer;
      if (element.requestFullscreen) {
        element.requestFullscreen();
      } else if (element.webkitRequestFullscreen) {
        element.webkitRequestFullscreen();
      } else if (element.mozRequestFullScreen) {
        element.mozRequestFullScreen();
      } else if (element.msRequestFullscreen) {
        element.msRequestFullscreen();
      }
      this.isFullscreen = true;
      this.$nextTick(() => {
        this.handleResize();
      });
    },

    // 退出全屏
    exitFullscreen() {
      if (document.exitFullscreen) {
        document.exitFullscreen();
      } else if (document.webkitExitFullscreen) {
        document.webkitExitFullscreen();
      } else if (document.mozCancelFullScreen) {
        document.mozCancelFullScreen();
      } else if (document.msExitFullscreen) {
        document.msExitFullscreen();
      }
      this.isFullscreen = false;
      this.$nextTick(() => {
        this.handleResize();
      });
    }
  },

  mounted() {
    // 监听全屏变化事件
    document.addEventListener("fullscreenchange", this.handleFullscreenChange);
    document.addEventListener(
      "webkitfullscreenchange",
      this.handleFullscreenChange
    );
    document.addEventListener(
      "mozfullscreenchange",
      this.handleFullscreenChange
    );
    document.addEventListener(
      "MSFullscreenChange",
      this.handleFullscreenChange
    );
  },

  beforeDestroy() {
    // 移除全屏变化事件监听
    document.removeEventListener(
      "fullscreenchange",
      this.handleFullscreenChange
    );
    document.removeEventListener(
      "webkitfullscreenchange",
      this.handleFullscreenChange
    );
    document.removeEventListener(
      "mozfullscreenchange",
      this.handleFullscreenChange
    );
    document.removeEventListener(
      "MSFullscreenChange",
      this.handleFullscreenChange
    );
  }
};

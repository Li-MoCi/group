import * as d3 from "d3";

export default {
  // 颜色方案
  colorSchemes: {
    default: ["#6fce7a", "#ff8373", "#f9c62c", "#a5ca34", "#70d3bd", "#ea91b0"],
    warm: ["#ff7f0e", "#ffbb78", "#ff9896", "#d62728", "#e377c2", "#f7b6d2"],
    cool: ["#1f77b4", "#aec7e8", "#17becf", "#9edae5", "#7f7f7f", "#c7c7c7"],
    green: ["#2ca02c", "#98df8a", "#a1d99b", "#74c476", "#41ab5d", "#238b45"]
  },

  // 创建颜色比例尺
  createColorScale(domain, scheme = "default") {
    return d3
      .scaleOrdinal()
      .domain(domain)
      .range(this.colorSchemes[scheme]);
  },

  // 根据节点类型获取颜色
  getNodeColor(node, colorScale) {
    return colorScale(node.type || "default");
  },

  // 根据连线类型获取颜色
  getLinkColor(link, colorScale) {
    return colorScale(link.type || "default");
  },

  // 计算节点大小（基于度）
  calculateNodeSize(nodeId, links, minSize = 10, maxSize = 50) {
    const degree = links.filter(
      link => link.source.id === nodeId || link.target.id === nodeId
    ).length;

    return d3
      .scaleLinear()
      .domain([1, Math.max(1, d3.max(links, l => l.value) || 1)])
      .range([minSize, maxSize])(degree);
  },

  // 计算连线宽度（基于权重）
  calculateLinkWidth(link, minWidth = 1, maxWidth = 5) {
    return d3
      .scaleLinear()
      .domain([1, Math.max(1, d3.max(links, l => l.value) || 1)])
      .range([minWidth, maxWidth])(link.value || 1);
  },

  // 创建箭头标记
  createArrowMarker(svg, color = "#999", size = 10) {
    svg
      .append("defs")
      .append("marker")
      .attr("id", "arrow")
      .attr("viewBox", "0 -5 10 10")
      .attr("refX", size)
      .attr("refY", 0)
      .attr("markerWidth", size)
      .attr("markerHeight", size)
      .attr("orient", "auto")
      .append("path")
      .attr("d", "M0,-5L10,0L0,5")
      .attr("fill", color);
  },

  // 创建虚线样式
  createDashedLine(pattern = "5,5") {
    return `${pattern}`;
  },

  // 创建渐变
  createGradient(svg, id, startColor, endColor) {
    const gradient = svg
      .append("defs")
      .append("linearGradient")
      .attr("id", id)
      .attr("x1", "0%")
      .attr("y1", "0%")
      .attr("x2", "100%")
      .attr("y2", "0%");

    gradient
      .append("stop")
      .attr("offset", "0%")
      .attr("stop-color", startColor);

    gradient
      .append("stop")
      .attr("offset", "100%")
      .attr("stop-color", endColor);

    return `url(#${id})`;
  },

  // 计算文本位置
  calculateTextPosition(node, fontSize = 12) {
    return {
      x: node.x,
      y: node.y + fontSize / 2
    };
  },

  // 计算连线文本位置
  calculateLinkTextPosition(source, target) {
    return {
      x: (source.x + target.x) / 2,
      y: (source.y + target.y) / 2 - 5
    };
  },

  // 创建阴影效果
  createShadowFilter(svg, id = "drop-shadow") {
    const filter = svg
      .append("defs")
      .append("filter")
      .attr("id", id)
      .attr("height", "130%");

    filter
      .append("feGaussianBlur")
      .attr("in", "SourceAlpha")
      .attr("stdDeviation", 3)
      .attr("result", "blur");

    filter
      .append("feOffset")
      .attr("in", "blur")
      .attr("dx", 2)
      .attr("dy", 2)
      .attr("result", "offsetBlur");

    const merge = filter.append("feMerge");
    merge.append("feMergeNode").attr("in", "offsetBlur");
    merge.append("feMergeNode").attr("in", "SourceGraphic");

    return `url(#${id})`;
  },

  // 创建高亮效果
  createHighlightEffect(svg, id = "highlight") {
    const filter = svg
      .append("defs")
      .append("filter")
      .attr("id", id)
      .attr("x", "-50%")
      .attr("y", "-50%")
      .attr("width", "200%")
      .attr("height", "200%");

    filter
      .append("feGaussianBlur")
      .attr("stdDeviation", "3")
      .attr("result", "coloredBlur");

    const merge = filter.append("feMerge");
    merge.append("feMergeNode").attr("in", "coloredBlur");
    merge.append("feMergeNode").attr("in", "SourceGraphic");

    return `url(#${id})`;
  }
};

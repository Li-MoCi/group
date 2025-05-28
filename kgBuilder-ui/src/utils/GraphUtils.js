import * as d3 from "d3";

export default {
  // 计算两点之间的距离
  calculateDistance(point1, point2) {
    const dx = point2.x - point1.x;
    const dy = point2.y - point1.y;
    return Math.sqrt(dx * dx + dy * dy);
  },

  // 计算两点之间的角度
  calculateAngle(point1, point2) {
    const dx = point2.x - point1.x;
    const dy = point2.y - point1.y;
    return (Math.atan2(dy, dx) * 180) / Math.PI;
  },

  // 计算贝塞尔曲线路径 (修改为考虑节点半径的直线)
  calculateCurvedPath(source, target) {
    const sourceRadius = source.r || 30; // 假设节点有r属性，默认30
    const targetRadius = target.r || 30; // 假设节点有r属性，默认30

    let dx = target.x - source.x;
    let dy = target.y - source.y;
    let dr = Math.sqrt(dx * dx + dy * dy);

    if (dr === 0) {
      // 节点完全重合
      return `M${source.x},${source.y}L${target.x},${target.y}`;
    }

    // 如果距离小于半径和，为避免反向或重叠，直接连接中心点或返回一个短线段
    if (dr < sourceRadius + targetRadius) {
      // Option 1: Connect centers if too close for proper edge calculation
      // return `M${source.x},${source.y}L${target.x},${target.y}`;
      // Option 2: Attempt to draw a very short line if possible, or just centers
      // For simplicity, let's just connect centers if they are overlapping significantly.
      // A more robust solution might hide the link or draw a special marker.
      return `M${source.x},${source.y}A${dr},${dr} 0 0,1 ${target.x},${target.y}`; // Fallback to original arc if too close
    }

    const s_prime_x = source.x + (dx / dr) * sourceRadius;
    const s_prime_y = source.y + (dy / dr) * sourceRadius;
    const t_prime_x = target.x - (dx / dr) * targetRadius;
    const t_prime_y = target.y - (dy / dr) * targetRadius;

    // 更新 dx, dy, dr 以供弧线路径使用 (如果仍要用弧线)
    // dx = t_prime_x - s_prime_x;
    // dy = t_prime_y - s_prime_y;
    // dr = Math.sqrt(dx * dx + dy * dy);
    // if (dr === 0) return `M${s_prime_x},${s_prime_y}L${t_prime_x},${t_prime_y}`; // Should not happen if radii are handled

    // return `M${s_prime_x},${s_prime_y}A${dr},${dr} 0 0,1 ${t_prime_x},${t_prime_y}`; // 弧线用修正点
    return `M${s_prime_x},${s_prime_y}L${t_prime_x},${t_prime_y}`; // 改为直线
  },

  // 计算分组的包围路径
  calculateGroupPath(nodes, padding = 20) {
    if (!nodes || nodes.length === 0) return "";

    const x = nodes.map(n => n.x);
    const y = nodes.map(n => n.y);
    const minX = Math.min(...x) - padding;
    const minY = Math.min(...y) - padding;
    const maxX = Math.max(...x) + padding;
    const maxY = Math.max(...y) + padding;

    return `M${minX},${minY}L${maxX},${minY}L${maxX},${maxY}L${minX},${maxY}Z`;
  },

  // 创建力导向图配置
  createForceSimulation(width, height) {
    return d3
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
  },

  // 计算节点的相邻节点
  getNeighbors(nodeId, links) {
    const neighbors = new Set();
    links.forEach(link => {
      if (link.source.id === nodeId) {
        neighbors.add(link.target.id);
      }
      if (link.target.id === nodeId) {
        neighbors.add(link.source.id);
      }
    });
    return Array.from(neighbors);
  },

  // 计算最短路径
  findShortestPath(startId, endId, links) {
    const graph = new Map();

    // 构建邻接表
    links.forEach(link => {
      const sourceId = link.source.id;
      const targetId = link.target.id;

      if (!graph.has(sourceId)) {
        graph.set(sourceId, []);
      }
      if (!graph.has(targetId)) {
        graph.set(targetId, []);
      }

      graph.get(sourceId).push(targetId);
      graph.get(targetId).push(sourceId);
    });

    // BFS查找最短路径
    const queue = [[startId]];
    const visited = new Set([startId]);

    while (queue.length > 0) {
      const path = queue.shift();
      const currentId = path[path.length - 1];

      if (currentId === endId) {
        return path;
      }

      const neighbors = graph.get(currentId) || [];
      for (const neighborId of neighbors) {
        if (!visited.has(neighborId)) {
          visited.add(neighborId);
          queue.push([...path, neighborId]);
        }
      }
    }

    return null;
  },

  // 计算节点的度
  calculateDegree(nodeId, links) {
    return links.filter(
      link => link.source.id === nodeId || link.target.id === nodeId
    ).length;
  },

  // 计算图的连通分量
  findConnectedComponents(nodes, links) {
    const visited = new Set();
    const components = [];

    nodes.forEach(node => {
      if (!visited.has(node.id)) {
        const component = new Set();
        this.dfs(node.id, links, visited, component);
        components.push(Array.from(component));
      }
    });

    return components;
  },

  // 深度优先搜索
  dfs(nodeId, links, visited, component) {
    visited.add(nodeId);
    component.add(nodeId);

    const neighbors = this.getNeighbors(nodeId, links);
    neighbors.forEach(neighborId => {
      if (!visited.has(neighborId)) {
        this.dfs(neighborId, links, visited, component);
      }
    });
  }
};

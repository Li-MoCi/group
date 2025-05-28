import request from "@/utils/request";

export const styleTemplateApi = {
  // 获取所有样式模板
  getTemplates() {
    return request({
      url: "/api/style-templates",
      method: "get"
    });
  },

  // 创建新模板
  createTemplate(data) {
    return request({
      url: "/api/style-templates",
      method: "post",
      data
    });
  },

  // 更新模板
  updateTemplate(id, data) {
    return request({
      url: `/api/style-templates/${id}`,
      method: "put",
      data
    });
  },

  // 删除模板
  deleteTemplate(id) {
    return request({
      url: `/api/style-templates/${id}`,
      method: "delete"
    });
  }
};

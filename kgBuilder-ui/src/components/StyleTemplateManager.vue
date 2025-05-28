<template>
  <div class="style-template-manager">
    <el-dialog
      title="样式模板管理"
      :visible.sync="dialogVisible"
      width="60%"
      :before-close="handleClose"
    >
      <div class="template-list">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="节点样式" name="node">
            <div class="template-actions">
              <el-button
                type="primary"
                size="small"
                @click="createTemplate('node')"
              >
                新建模板
              </el-button>
            </div>
            <el-table :data="nodeTemplates" style="width: 100%">
              <el-table-column prop="name" label="模板名称" width="180">
              </el-table-column>
              <el-table-column label="预览" width="120">
                <template slot-scope="scope">
                  <div
                    class="template-preview"
                    :style="getNodePreviewStyle(scope.row)"
                  ></div>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述">
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template slot-scope="scope">
                  <el-button size="mini" @click="applyTemplate(scope.row)"
                    >应用</el-button
                  >
                  <el-button
                    size="mini"
                    type="primary"
                    @click="editTemplate(scope.row)"
                    >编辑</el-button
                  >
                  <el-button
                    size="mini"
                    type="danger"
                    @click="deleteTemplate(scope.row)"
                    >删除</el-button
                  >
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="关系样式" name="relationship">
            <div class="template-actions">
              <el-button
                type="primary"
                size="small"
                @click="createTemplate('relationship')"
              >
                新建模板
              </el-button>
            </div>
            <el-table :data="relationshipTemplates" style="width: 100%">
              <el-table-column prop="name" label="模板名称" width="180">
              </el-table-column>
              <el-table-column label="预览" width="120">
                <template slot-scope="scope">
                  <div
                    class="template-preview"
                    :style="getRelationshipPreviewStyle(scope.row)"
                  ></div>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述">
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template slot-scope="scope">
                  <el-button size="mini" @click="applyTemplate(scope.row)"
                    >应用</el-button
                  >
                  <el-button
                    size="mini"
                    type="primary"
                    @click="editTemplate(scope.row)"
                    >编辑</el-button
                  >
                  <el-button
                    size="mini"
                    type="danger"
                    @click="deleteTemplate(scope.row)"
                    >删除</el-button
                  >
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 模板编辑对话框 -->
      <el-dialog
        :title="editingTemplate ? '编辑模板' : '新建模板'"
        :visible.sync="editDialogVisible"
        width="50%"
        append-to-body
      >
        <el-form :model="templateForm" ref="templateForm" label-width="100px">
          <el-form-item label="模板名称" prop="name">
            <el-input v-model="templateForm.name"></el-input>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input
              type="textarea"
              v-model="templateForm.description"
            ></el-input>
          </el-form-item>

          <!-- 节点样式表单 -->
          <template v-if="activeTab === 'node'">
            <el-form-item label="节点大小">
              <el-slider
                v-model="templateForm.style.radius"
                :min="10"
                :max="50"
              ></el-slider>
            </el-form-item>
            <el-form-item label="填充颜色">
              <el-color-picker
                v-model="templateForm.style.fill"
              ></el-color-picker>
            </el-form-item>
            <el-form-item label="边框颜色">
              <el-color-picker
                v-model="templateForm.style.stroke"
              ></el-color-picker>
            </el-form-item>
            <el-form-item label="边框宽度">
              <el-slider
                v-model="templateForm.style.strokeWidth"
                :min="1"
                :max="5"
              ></el-slider>
            </el-form-item>
          </template>

          <!-- 关系样式表单 -->
          <template v-if="activeTab === 'relationship'">
            <el-form-item label="线条颜色">
              <el-color-picker
                v-model="templateForm.style.color"
              ></el-color-picker>
            </el-form-item>
            <el-form-item label="线条宽度">
              <el-slider
                v-model="templateForm.style.width"
                :min="1"
                :max="5"
              ></el-slider>
            </el-form-item>
            <el-form-item label="线条类型">
              <el-select v-model="templateForm.style.lineType">
                <el-option label="实线" value="solid"></el-option>
                <el-option label="虚线" value="dashed"></el-option>
                <el-option label="点线" value="dotted"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="箭头大小">
              <el-slider
                v-model="templateForm.style.arrowSize"
                :min="5"
                :max="15"
              ></el-slider>
            </el-form-item>
          </template>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button @click="editDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="saveTemplate">确 定</el-button>
        </div>
      </el-dialog>
    </el-dialog>
  </div>
</template>

<script>
import { styleTemplateApi } from "@/api/styleTemplate";

export default {
  name: "StyleTemplateManager",
  data() {
    return {
      dialogVisible: false,
      editDialogVisible: false,
      activeTab: "node",
      editingTemplate: null,
      nodeTemplates: [],
      relationshipTemplates: [],
      templateForm: {
        name: "",
        description: "",
        style: {}
      },
      loading: false
    };
  },
  methods: {
    handleClose(done) {
      this.$confirm("确认关闭？")
        .then(_ => {
          done();
        })
        .catch(_ => {});
    },
    async loadTemplates() {
      try {
        this.loading = true;
        const response = await styleTemplateApi.getTemplates();
        if (response.code === 200) {
          const templates = response.data;
          this.nodeTemplates = templates.filter(t => t.type === "node");
          this.relationshipTemplates = templates.filter(
            t => t.type === "relationship"
          );
        }
      } catch (error) {
        this.$message.error("加载模板失败");
        console.error("Failed to load templates:", error);
      } finally {
        this.loading = false;
      }
    },
    createTemplate(type) {
      this.editingTemplate = null;
      this.templateForm = {
        name: "",
        description: "",
        style:
          type === "node"
            ? {
                radius: 30,
                fill: "#6fce7a",
                stroke: "#ffffff",
                strokeWidth: 2
              }
            : {
                color: "#999999",
                width: 1,
                lineType: "solid",
                arrowSize: 10
              }
      };
      this.editDialogVisible = true;
    },
    editTemplate(template) {
      this.editingTemplate = template;
      this.templateForm = JSON.parse(JSON.stringify(template));
      this.editDialogVisible = true;
    },
    async saveTemplate() {
      try {
        const templateData = {
          ...this.templateForm,
          type: this.activeTab
        };

        if (this.editingTemplate) {
          // 更新模板
          await styleTemplateApi.updateTemplate(
            this.editingTemplate.id,
            templateData
          );
        } else {
          // 创建新模板
          await styleTemplateApi.createTemplate(templateData);
        }

        await this.loadTemplates();
        this.editDialogVisible = false;
        this.$message.success("保存成功");
      } catch (error) {
        this.$message.error("保存失败");
        console.error("Failed to save template:", error);
      }
    },
    async deleteTemplate(template) {
      try {
        await this.$confirm("此操作将永久删除该模板, 是否继续?", "提示", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        });

        await styleTemplateApi.deleteTemplate(template.id);
        await this.loadTemplates();
        this.$message.success("删除成功");
      } catch (error) {
        if (error !== "cancel") {
          this.$message.error("删除失败");
          console.error("Failed to delete template:", error);
        }
      }
    },
    applyTemplate(template) {
      this.$emit("apply-template", {
        type: this.activeTab,
        template
      });
    },
    getNodePreviewStyle(template) {
      return {
        width: "30px",
        height: "30px",
        borderRadius: "50%",
        backgroundColor: template.style.fill,
        border: `${template.style.strokeWidth}px solid ${template.style.stroke}`
      };
    },
    getRelationshipPreviewStyle(template) {
      return {
        width: "100%",
        height: "2px",
        backgroundColor: template.style.color,
        borderStyle: template.style.lineType
      };
    },
    show() {
      this.dialogVisible = true;
      this.loadTemplates();
    }
  }
};
</script>

<style lang="scss" scoped>
.style-template-manager {
  .template-list {
    margin-bottom: 20px;
  }

  .template-actions {
    margin-bottom: 15px;
  }

  .template-preview {
    margin: 5px;
  }

  .el-dialog__body {
    padding: 20px;
  }
}
</style>

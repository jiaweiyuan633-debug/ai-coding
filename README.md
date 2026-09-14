# AI Coding — AI 零代码应用生成平台

> 用一句话描述需求，AI 帮你生成完整可运行的网页应用：智能生成 → 实时预览 → 对话修改 → 版本回滚 → 一键部署分享。

**技术标签**：`LangChain4j` `LangGraph4j` `Spring Boot 3` `Spring Cloud Alibaba + Dubbo` `Redis + Caffeine 多级缓存` `SSE 响应式编程` `Prometheus 监控` `多租户` `设计模式` `Vue 3` `DeepSeek`

## ✨ 核心功能

| 功能 | 说明 |
| ---- | ---- |
| 🤖 智能代码生成 | 输入需求，AI 自动路由生成类型（HTML 网页 / Vue 应用等），工具调用逐文件生成，SSE 流式实时可见 |
| 🧩 透明工作流 | LangGraph4j 编排：智能路由 → 生成 → 质量门禁 → 自动修复（≤3 轮）→ 截图封面，节点状态实时推送 |
| 🖥 实时预览 | 生成结果即时在线预览，边生成边看 |
| 💬 对话式修改 | 多轮对话上下文记忆，说一句改一句 |
| 🕰 版本时光机 | 每次修改自动快照，版本树任意回滚（借鉴 v0） |
| 🎯 可视化编辑 | 预览页选中元素，局部精准修改（借鉴 lovable） |
| 🚀 一键部署分享 | 静态托管 + deployKey，生成可访问链接 |
| 🛍 模板广场 | 精选应用 / 模板市场，一键 Remix 二次创作 |
| 📊 用量中心 | 模型调用 token / 耗时 / 次数全量计量，多租户配额 |

## 🏗 架构演进

单体起步（快速验证核心闭环）→ 微服务化（企业级架构）：

```
单体：ai-coding-backend（user / app / ai / deploy 分包）
  ↓
微服务：gateway / user / app / ai / deploy / screenshot + Nacos + Dubbo 3 + Sentinel
       + Redis + Caffeine 多级缓存 + Prometheus / Grafana 监控
```

## 🚀 快速开始（随阶段推进持续完善）

```bash
# 后端（阶段 2 起）
cd ai-coding-backend && mvn spring-boot:run

# 前端（阶段 5 起）
cd ai-coding-frontend && npm install && npm run dev

# 一键部署全家桶（阶段 7 起）
docker compose up -d
```

## 📂 目录结构

```
├── docs/                  # 项目文档（计划书 / 竞品调研 / 架构设计 / 部署手册）
├── ai-coding-backend/     # 后端（单体 → 微服务演进）
├── ai-coding-frontend/    # Vue 3 前端
├── sql/                   # 建表脚本
├── docker/                # Docker Compose 与监控配置
└── README.md
```

## 🗺 里程碑

- [x] 阶段 0：环境与仓库初始化
- [x] 阶段 1：项目计划书 + 竞品调研报告
- [x] 阶段 2：单体核心闭环（用户 / 应用 / AI 路由 / SSE 流式生成 / 对话记忆）
- [x] 阶段 3：LangGraph4j 工作流 + 版本时光机 + 多模型路由 + 用量计量
- [x] 阶段 4：一键部署分享 + 截图封面 + 模板广场
- [ ] 阶段 5：Vue 3 前端全套
- [ ] 阶段 6：微服务化（Nacos / Dubbo / Gateway / 多级缓存 / 监控）
- [ ] 阶段 7：Docker Compose 上线 + 端到端冒烟测试

## 📚 文档

- [项目计划书](docs/01-项目计划书.md)
- [竞品调研报告](docs/02-竞品调研报告.md)
- [架构设计](docs/03-架构设计.md)
- [部署手册](docs/04-部署手册.md)

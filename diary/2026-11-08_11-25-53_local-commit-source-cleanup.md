# 城镇与温度修复的本地提交整理

- Time: `2026-11-08 11:25:53 +08:00`
- Author: `Codex; OpenAI GPT-6.1; local Git delivery agent`
- Status: `completed` for source selection and local commit preparation
- Scope: 两个仓库的待提交源码、正式资源、文档、验证入口与本地产物排除

## Completed

- 按用户要求整理全部待提交内容，标题为“修复城镇和温度模拟系统问题”，正文使用1、2、3点说明修复。
- 保留城镇同步/仓库协议、温度模拟/红外/装备供热、世界生成及研究界面的源码、正式配置、测试与现有文档记录，包含此前已安排的废弃JUnit删除和IDE文件清理。
- 配套仓库保留游戏KubeJS、矿床正式数据生成器`Scripts/worldgen/deposits/export_deposits.py`及其输入、必要的密度生成验证。主仓库保留现用温度回归入口。
- 用户询问脚本用途后，明确排除不参与当前运行或导出的旧群系/峡谷/固定样板原型及旧验收驱动；其源码和采样结果仍保留在本地，由Git忽略规则排除。

## Decisions

- 缓存、日志、压缩采样、截图和临时构建产物不进入提交；正式游戏NBT/JSON资源、作者记录和精简验证摘要不按扩展名误删。
- 两个Git仓库分别提交，只做本地提交。没有删除本地试验目录，也没有改动只读design目录。
- 本轮属于提交整理，不新增游戏行为修改；文档影响为本条交付记录和工具忽略边界。

## Validation

- 按两个仓库分别检查暂存范围、生成文件排除和提交结果。
- 既有装备/体温验证为30/30 Forge入口，详情见[装备验证记录](../docs/climate/validation/equipment-heating-2026-11-08.json)；本轮不把该结果扩展为全部待提交系统已统一验收。

## Remaining

- None for local source selection; prior system validation limits remain in their owning documents.

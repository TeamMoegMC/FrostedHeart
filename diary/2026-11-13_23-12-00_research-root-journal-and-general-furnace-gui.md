# 札记一级退出与通用控温炉原生GUI

- Time: `2026-11-13 23:12:00 +08:00`
- Author: `Codex / OpenAI；实现与验收`
- Status: `completed`（服务端16项、最终构建、实际客户端检查与Playtest部署完成）
- Scope: DrawDeskScreen/KnowledgeLayer导航、控温炉原生Screen和槽位、通用ControlledFiringRecipe、双语设备文案。

## Completed

- 札记主页面后退关闭容器回世界，不再露出旧绘图台；桌面选择不增加一层返回。札记Esc先于搜索框处理，详情/风道/抄录等子页面仍逐级返回。旧画布/档案后退回札记，原按钮可主动进入旧功能。
- ControlledFurnaceScreen改为AbstractContainerScreen和MenuScreens原生注册。276×232 GUI像素，复用MC原版demo_background底板、熔炉槽位/箭头和原版按钮；背包改27格九列加9快捷栏。槽位索引不变，开始仍走捕获当前模式/目标的专用包，服务端同台交错比较保留。
- 删除配方只允许耐火黏土/木炭/耐火砖且产量等于原料的限制，原料/燃料/成功/失败产物按配方独立配置；不新增正式实验内容，不改变现有温度范围或Design授权。试样写绑定改按本炉成功温窗判断，同物品失败输出也不会变成成功试样。
- 设备状态和JEI失败文案泛化，材料量/温窗由JEI提供；更新living docs和玩家导航说明。

## Validation

- M5/M6设备共16项必需GameTest通过，日志 `build/research-machine-gui-20261113-validation.log`；含一项新增普通黏土/煤烧制陶制品、独立成功/失败数量、另一个experimentID、失败同物品不绑样、观察完成解锁生产和原生槽位映射的完整服务端回归。
- 首轮compileJava、jar/reobfJar通过；补充搜索框Esc优先级后最终包通过（3m12s），日志 `build/research-machine-gui-20261113-final-build.log`。正式交付/视觉基线为14,913,033字节，已核对Native Screen继承与最终Esc字节码。
- Playtest无额外helper，以离线M5Validation打开既有 `m6-brief-1794485184508` 测试世界，真实原生GUI、9列背包、调温1100→1150和试验→生产模式通过。截图在 `build/research-machine-ui-20261113/`：10为生产模式，16为搜索框实际输入z并有焦点，17为一次Esc直接回世界；19/20为抄录返回札记，21/22为旧画布返回札记。查看实际画面，不将错误鼠标尝试/最小化窗口的旧画面当成功证据。
- 使用Windows窗口消息，CUI点击进一步使用系统鼠标事件；测试世界用setblock/tp/time准备条件，不是物理手工或完整生存验收。所有准备命令只在专用测试世界；初期用户短暂操作窗口后重新明确切到该测试世界。旧M5自动驱动未使用。
- 用户保存退出后先备份和部署，配置/按键及存档目录元数据核对不变；测试结束正常保存并关闭自有进程1280，退出日志已归档为 `client-validation.log`。用户随后自行重新启动24560，未中断新进程。最后尝试缩短两条可选按钮文案未再部署，已撤回，交付保持实际验收的功能与资源版本；构建目录恢复为与已安装的视觉基线同一JAR。
- 旧包与部署清单在 `build/prism-backups/2026-11-13-general-furnace-root-journal/`。TWR源码和原有暂存项未改，未操作Development/runClient。

## Remaining

- 未在客户端重走整条研究和完整试烧，新通用配方由真实服务器回归覆盖；生产/取消等状态机沿用原规则。旧M5自动客户端驱动针对旧CUI，不能直接作为本轮原生GUI证据。

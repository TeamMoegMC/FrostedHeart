# CUI 控制桥动态增删子元素

- 作者：Codex
- 日期：2026-11-26
- 实现：cui-remove selector 定位任意深度元素并从直接父 UILayer.remove，返回移除前 debug 信息和父元素信息；根或不支持操作的节点报错。UILayer.remove 移除直接子节点，清除 attachedScrollbar 引用，失效内容尺寸缓存，调用 onClosed。
- 实现：cui-add JSON 指定 selector（目标父节点）、class（元素全限定类名）、args（不含自动注入的父节点）。只构造 UIElement 子类，在所选 UILayer 内 add 后返回实际 debug 信息，不 refresh 或重新布局。
- 构造器：支持非 public；parameterTypes 为完整签名（含首参父节点类型），支持 primitive 与 Java 数组类型名，精确查找。未提供时按签名字典序依次尝试参数数量一致且首参兼容的构造器；其余参数按声明泛型类型使用 Gson 反序列化，全部失败返回候选错误。
- 文档：同步 Scripts/agent_client.py 和 docs/chorda-cui.md，说明参数签名、失败行为、不自动布局与增删后 selector 有效性。
- 验证：compileAgentBridgeJava --offline -I Scripts/dev_client_bridge.init.gradle 通过；临时 Java 验证提取实际构造/移除源码并使用本地 Gson 2.10.1，覆盖父参注入、私有构造器、泛型集合、重载失败回退、显式签名、primitive 数组、错误类/参数数量/null/签名、直接移除和回调/缓存/滚动条清理，全部通过；临时文件已清理；git diff --check 通过。
- 限制：本轮未启动客户端验证新增命令。候选构造器自身副作用不自动撤销，构造失败不会执行 add。

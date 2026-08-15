# MetalFX + 超分 / 超帧开发步骤

> 目标：在 Metal 基础渲染后端基本完成的前提下，利用公共仓库免费的 GitHub Actions，把 MetalFX 超分辨率和超帧（帧生成）拆分为可验证的里程碑。
>
> 约定：一个 GitHub Actions workflow 主要对应一个步骤（里程碑 M）。每个里程碑必须先通过自动化逻辑测试和构建测试，再进入需要 macOS / Apple Silicon GPU 的调试或验收环节。

## 里程碑总表

| 里程碑 | GitHub Actions 步骤 | 主要目标 | 逻辑测试 | 构建 / 静态检查 | Debug / 硬件验证 | 完成标准 |
|---|---|---|---|---|---|---|
| **M0** | baseline | 固定当前 Metal 后端基线，确保后续改动可回归 | Frame Graph、资源生命周期、渲染契约、现有单元测试 | Java 25、Gradle、Swift 原生库、生产 JAR 隔离检查 | 在 macOS Apple Silicon 上完成一次基线启动和渲染 | 基线构建通过，已有 Metal 路径无回归 |
| **M1** | metalfx-capability | 建立 MetalFX 构建能力、设备能力和版本协商 | 能力位、设备探测、系统版本、设备不支持时的降级路径 | 检查 MetalFX 符号、Swift 导出接口、Java FFM 声明一致性 | 输出设备能力日志；验证不支持 MetalFX 时仍可使用普通 Metal | 能可靠判断 Spatial、Temporal、Frame Generation 是否可用 |
| **M2** | metalfx-spatial | 接入 MetalFX Spatial 超分 | 输入尺寸、输出尺寸、缩放比例、颜色格式、纹理状态和生命周期 | 编译 MetalFX Spatial 路径；运行无 GPU 依赖的参数和状态机测试 | Apple Silicon 真机画面、分辨率切换、窗口变化、关闭/开启回退 | Spatial 超分可开关，画面正确，失败时自动回退 |
| **M3** | metalfx-temporal | 接入 MetalFX Temporal 超分 | 深度、运动矢量、抖动矩阵、历史帧失效和重建逻辑 | 验证 Temporal 输入资源绑定及 Native Bridge ABI | 移动镜头、快速转身、区块加载、UI 和透明物体场景 | Temporal 超分稳定运行，无明显历史帧污染 |
| **M4** | motion-depth-contract | 建立超分 / 超帧共用的运动与深度契约 | 运动矢量方向、坐标系、深度范围、反转 Z、帧序和尺寸匹配 | Render Contract、pass manifest、attachment binding 检查 | 记录并对比真实帧的颜色、深度、运动矢量附件 | 所有 MetalFX 输入都能被追踪、验证和诊断 |
| **M5** | frame-generation-core | 接入 MetalFX 超帧（Frame Generation）核心链路 | 输入帧 / 输出帧顺序、present 时序、重复调用、丢帧和回退 | 编译 Frame Generation native path；验证接口版本和生命周期 | Apple Silicon 真机验证生成帧、延迟、窗口最小化和暂停恢复 | 超帧可独立开关，异常时不会阻塞或破坏主渲染 |
| **M6** | frame-generation-motion | 完善超帧所需的运动数据和动态对象处理 | 玩家、实体、方块、粒子、UI 的运动分类与遮罩规则 | 运行运动捕获、对象姿态和 reactive mask 测试 | 快速移动、转视角、透明物体、粒子、手部 HUD 场景 | 主要运动场景无严重重影、撕裂或错误插帧 |
| **M7** | iris-sodium-integration | 将超分 / 超帧接入 Iris 和 Sodium 的实际渲染流程 | shader pass、阴影、后处理、区块渲染、重载和资源重建测试 | Fabric Mixin、Iris/Sodium 类路径编译与生产 JAR 检查 | 使用真实 Iris shader pack、区块加载和高负载场景 | 不破坏普通 Metal、Iris 和 Sodium 的兼容路径 |
| **M8** | debug-diagnostics | 完善可观测性和问题定位能力 | 失败状态、能力降级、资源不匹配、帧序错误的错误分类测试 | 日志格式、诊断 manifest、artifact 大小和保留策略检查 | 自动上传失败日志、Render Contract、截图和性能数据 | 任一失败都能定位到阶段、pass、资源或 native 接口 |
| **M9** | performance-acceptance | 性能、稳定性和最终验收 | 长时间运行、重复启停、分辨率切换、配置组合矩阵 | 完整构建、测试、生产 JAR 隔离和发布前检查 | Apple Silicon 真机 FPS、帧时间、延迟、显存和功耗采样 | 达到预设画质、稳定性和性能目标，可发布实验版本 |

## 每个 GitHub Actions 的统一结构

每个 workflow 建议按照以下顺序执行：

1. **准备环境**
   - Checkout 当前提交
   - 校验 Gradle Wrapper
   - 配置 Java 25
   - 在 macOS workflow 中选择 Xcode / SDK

2. **逻辑测试**
   - 运行纯 JVM 单元测试
   - 测试能力协商、状态机、尺寸计算、资源生命周期和回退逻辑
   - 测试 Render Contract 和 pass manifest

3. **构建与静态检查**
   - 编译 Java 和 Swift
   - 检查 Java FFM 与 Swift C ABI
   - 构建生产 JAR
   - 确认 validation-only 代码没有进入生产 JAR

4. **Debug**
   - 打开必要的 Metal debug / shader validation
   - 输出结构化日志
   - 失败时上传日志、manifest、截图和诊断报告 artifact

5. **硬件验证**
   - 只在确实需要 GPU 的里程碑中执行
   - 明确区分 hosted macOS runner 的编译结果和 Apple Silicon 真机验收结果
   - 对不支持 MetalFX 的环境验证正常回退

6. **结果归档**
   - 保存测试报告
   - 保存失败诊断资料
   - 不把临时大文件、原始 trace 或构建缓存无限期保留

## Actions 使用原则

- 每个 M 使用独立 workflow，避免一个大 workflow 失败后无法判断具体阶段。
- Pull Request 运行 M0、逻辑测试、构建测试和可执行的无 GPU 测试。
- push 到 master 运行完整编译和回归测试。
- tag 发布时运行发布前验收，并生成构建 artifact。
- GPU 依赖测试必须标记清楚：它们是 hosted runner 验证还是真机验证。
- 后续里程碑只在前置里程碑通过后继续，避免在超分基础未稳定时直接调试超帧。
- 每个 M 都必须保留可复现的失败信息，不能只返回一个 exit code。

## 推荐的依赖关系

~~~text
M0 baseline
  └─ M1 MetalFX capability
      └─ M2 Spatial Upscaling
          └─ M3 Temporal Upscaling
              └─ M4 Motion / Depth Contract
                  └─ M5 Frame Generation Core
                      └─ M6 Frame Generation Motion
                          └─ M7 Iris / Sodium Integration
                              └─ M8 Debug Diagnostics
                                  └─ M9 Performance Acceptance
~~~

## 当前优先级

1. 先完成 **M1**，把 MetalFX 能力探测、版本协商和降级路径做可靠。
2. 再完成 **M2 Spatial 超分**，先获得一条稳定、可关闭、可回退的超分路径。
3. 然后完成 **M3 Temporal 超分**，解决深度、运动矢量和历史帧问题。
4. 在 **M4** 固化运动 / 深度契约后，再进入 **M5 超帧**。
5. 超帧稳定后，最后处理 Iris / Sodium 实际场景、诊断和性能验收。

<p align="center">
  <img src=".github/icon.png" width="96" alt="DFRoot 图标" />
</p>

# DFRoot Shizuku

DFRoot Shizuku 是基于 [diabl0w/DFRoot](https://github.com/diabl0w/DFRoot) 的 Android 临时 Root（系统管理员权限）工具，提供 Shizuku 环境准备、SU 管理器识别、中文界面和全屏日志。Root 仅在本次开机有效，设备完整重启后需要重新提权。

**手动提权需要先启动并授权 Shizuku。** 当前已同步上游 v4.7，设计与实现依据见[修改说明](修改说明.md)。

[下载 APK](https://github.com/kun520ovo/DFRoot-Shizuku/releases/latest) · [使用条件](#使用条件) · [使用步骤](#使用步骤) · [常见问题](#常见问题) · [项目结构](#项目结构) · [开发文档](#开发文档)

## 应用分工

| 应用 | 作用 |
|---|---|
| DFRoot Shizuku | 检查运行条件、执行临时提权，并显示结果与日志。 |
| Shizuku | 提供操作临时系统设置所需的权限；服务运行和 Root 生效是不同状态。 |
| SU 管理器 | 提供 ksud 程序，并在 Root 生效后管理应用授权。 |

部分设备的 init 日志设置会影响提权触发。手动提权时，本应用通过 Shizuku 按需临时调整并校验 `log.tag.init`，再执行提权，结束后尝试恢复并校验原值。

## 使用条件

| 项目 | 要求 |
|---|---|
| 本项目版本 | `5.3-cn`，应用包名为 `df.root.shizuku.cn`。 |
| 设备 | ARM64，Android 12L / API 32 或更高版本。 |
| 内核 | 命中下方支持的 KMI；厂商固件和安全补丁也会影响可用性。 |
| Shizuku | 手动提权需 13 或以上版本，服务已启动并授权本应用。 |
| SU 管理器 | 兼容 KernelSU，提供可识别的 ksud 程序；支持 `libksud.so` 和符合检查条件的 SukiSU 随机库名。 |

- Shizuku：[官方下载](https://shizuku.rikka.app/download/)与[启动指南](https://shizuku.rikka.app/guide/setup/)。
- SU 管理器：[KernelSU](https://github.com/tiann/KernelSU/releases/latest)、[KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next/releases/latest)、[SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) 等；以本应用实际识别结果为准。

### 支持的内核

KMI 标识内核所属的 Android 分支与内核主次版本，当前内置以下 8 个条目：

| KMI | KMI |
|---|---|
| `android12-5.10` | `android13-5.10` |
| `android13-5.15` | `android14-5.15` |
| `android14-6.1` | `android15-6.6` |
| `android16-6.12` | `android17-6.18` |

以应用显示的“匹配 KMI”为准，不能仅根据系统 Android 版本判断。例如，Android 16 系统使用 `6.6.89-android15` 内核时，对应 `android15-6.6`。绿色 KMI 和列表中的 `✓` 表示内核匹配检查通过；Root 是否生效仍需在执行后到管理器确认。

## 使用步骤

1. **安装应用。** 安装 Releases 提供的本项目已签名 APK、Shizuku 和一个兼容的 SU 管理器。以下步骤按全新安装说明；如需卸载旧版，先完成正在进行的提权和设置恢复。
2. **启动 Shizuku。** 按官方指南启动服务，确认服务正在运行。
3. **允许读取应用列表。** 系统要求此权限时，按提示授权，或点击主按钮“请先允许读取应用列表”重试；若显示“打开权限设置”，进入系统设置授权后返回。系统未要求这项运行时权限时，无需额外授权。
4. **选择 SU 管理器。** 优先保留有效的当前或已保存选择；没有有效选择时，单个可用管理器自动选中，多个时手动选择。未找到时按常见问题排查，再点击“重新读取应用列表”。
5. **核对内核。** 确认“匹配 KMI”受支持；显示“不支持”时不能启动提权。
6. **完成 Shizuku 授权。** 点击“检查 Shizuku”，允许本应用使用服务，确认服务已运行、已授权且没有待恢复设置。
7. **启动 Root。** 点击“启动 Root”，保持应用在前台，等待提权和临时设置恢复完成。
8. **确认 Root 状态。** 关闭自动软重启且应用处于前台时，会在提权成功、临时设置恢复及管理器收尾完成后尝试打开所选管理器；也可手动打开，确认 Root 状态并管理应用授权。

应用列表权限仅在系统要求时授予，Shizuku 使用权限需单独授予本应用。授权后只刷新准备状态，手动提权仍需点击“启动 Root”。

分别确认 **Root 结果、临时设置恢复和软重启结果**。若提示设置恢复或记录清理未完成，重新连接 Shizuku 并点击“检查 Shizuku”，完成恢复后再运行。

通过无线调试启动的 Shizuku，在设备完整重启后需要重新启动。

## 设置选项

| 设置 | 默认状态 | 开启后的行为 |
|---|---|---|
| 开机自启 | 关闭 | 开机后使用保存的管理器配置尝试提权，独立运行，不依赖 Shizuku，也不执行前台的临时日志准备流程。 |
| 自动软重启 | 关闭 | 对手动和开机运行生效。手动时须先确认提权成功、临时设置恢复和记录清理，再等待管理器收尾并请求软重启；手动运行期间不可修改。 |
| 禁用所有模块 | 关闭 | 下次运行时为已安装模块设置禁用标记，用于处理模块导致的异常；之后需在管理器中按需重新启用。 |

开启“自动软重启”后仍可手动启动 Root。需要 Shizuku 临时日志准备的设备，应单独确认开机自启是否可用。

## 常见问题

| 问题 | 如何处理 |
|---|---|
| Shizuku 已运行，仍提示未授权 | 回到本应用点击“检查 Shizuku”，完成对本应用的授权。 |
| 应用列表权限被拒绝，不再弹窗 | 主按钮仍显示“请先允许读取应用列表”时先点击重试；显示“打开权限设置”时，进入系统设置授权后返回。 |
| 管理器已安装，但列表为空 | 若系统要求应用列表权限，先确认已授权；管理器还需提供可识别的 ksud 程序。尝试兼容版本或其他支持的管理器后，点击“重新读取应用列表”。 |
| KMI 匹配，为什么仍失败？ | 厂商固件、安全补丁和原生执行结果仍会影响提权，查看完整日志定位失败阶段。 |
| 日志提示文件修补失败或设备不受影响 | 内核修复或厂商补丁可能导致不可用；Shizuku 临时设置无法改变这一条件。 |
| 提示 init 的 INFO 日志未启用 | 确认开发者选项中的“日志缓冲区大小”不是“关闭”，或检查 init 日志设置。手动运行会先通过 Shizuku 准备并校验；开机自启直接检查当时的日志条件，不执行这一步准备。 |
| 无法再次点击启动 Root | 检测到本次开机已有 Hook 时会阻止重复执行，先到管理器确认 Root 状态。 |
| Root 已成功，但临时设置未恢复 | 重新连接并检查 Shizuku，等待设置恢复和记录清理完成。 |
| 开启自动软重启后没有重启 | 查看日志中的 Root、恢复与重启请求状态；“已请求”不代表设备已完成重启。 |
| 安装模块后运行异常 | 下次运行前开启“禁用所有模块”，随后在管理器中排查并按需重新启用。 |

应用内日志只显示当前会话，不提供日志文件保存或自动上传功能。点击“运行日志”右侧的“全屏”，可长按选择并复制文本，通过“关闭”返回。运行信息也会写入系统 Logcat，排查时可结合原生提示中的 Logcat / dmesg。

反馈问题时，请附设备型号、Android 版本、完整内核串、所选管理器版本和相关日志。

## 项目结构

```text
仓库根目录/
├── README.md                        项目介绍与使用说明
├── AGENTS.md                        开发维护规则
├── 修改说明.md                      设计依据、实现与构建说明
├── app/
│   ├── build.gradle.kts              应用身份、版本与构建配置
│   └── src/main/
│       ├── AndroidManifest.xml      应用组件与权限声明
│       ├── java/df/root/shizuku/cn/  Java 业务代码
│       ├── aidl/df/root/shizuku/cn/  Shizuku 服务接口
│       ├── res/                     界面布局、字符串与主题资源
│       ├── assets/licenses/         依赖许可文本
│       └── jni/                     原生代码、KO 与辅助程序
├── lkm/                             内核模块源码
├── gradle/                          Wrapper、JVM 与依赖配置
├── .github/workflows/               自动构建与发布
├── build.gradle.kts                 根构建配置
├── settings.gradle.kts              Gradle 项目设置
├── Makefile                         内核与应用构建入口
├── gradlew                          Linux / macOS 构建启动脚本
├── gradlew.bat                      Windows 构建启动脚本
└── LICENSE                          上游许可文本
```

## 开发文档

应用使用 Java 与 Android XML，原生组件使用 C / 汇编，通过 Gradle 和 Android NDK 构建。

| 入口 | 内容 |
|---|---|
| [修改说明](修改说明.md) | 设计依据、关键流程、上游同步与构建方法。 |
| [AGENTS.md](AGENTS.md) | 开发维护、验证与交付约定。 |
| [app/](app/) | Android 应用、资源与原生组件。 |
| [lkm/](lkm/) | 内核模块源码。 |
| [自动构建](.github/workflows/build.yml) | GitHub Actions 构建与发布流程。 |

## 来源与致谢

- 上游项目：[diabl0w/DFRoot](https://github.com/diabl0w/DFRoot)，作者 [@diabl0w](https://github.com/diabl0w)。
- 本项目：[kun520ovo/DFRoot-Shizuku](https://github.com/kun520ovo/DFRoot-Shizuku)。
- 原始 PoC 与相关代码：[LSPosed/LSPromise](https://github.com/LSPosed/LSPromise)。
- 内核模块与相关代码：[polygraphene/DFReroot](https://github.com/polygraphene/DFReroot)。
- 非特权 XFRM socket 方法：[combeng6th/DirtyInit](https://github.com/combeng6th/DirtyInit)。
- Shizuku 接入使用 API / Provider `13.1.5`，许可文本见 [shizuku-api.txt](app/src/main/assets/licenses/shizuku-api.txt)。

上游许可证见 [LICENSE](LICENSE)。保留上游与依赖原有的作者署名和许可声明。

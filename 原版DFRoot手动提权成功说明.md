# DFRoot 重启后临时提权说明

更新日期：2026-10-13

## 适用环境

- 设备：HONOR PTP-AN10
- 系统：Android 16 / MagicOS 10
- 内核：6.6.89，匹配 `android15-6.6`
- DFRoot：4.1-cn
- Root 管理器：SukiSU Ultra 4.2.0

本报告只保留通用设备信息、临时成功操作步骤和必要的兼容性结论，不包含个人无线 ADB 地址或本机路径。

## 为什么原配置失败、临时设置后成功

原配置下，设备的全局日志属性 `persist.log.tag=S` 会使日志保持静默。DFRoot 写入 hook 后，需要等待 init 执行 INFO 日志路径来触发 hook；这条路径被静默设置跳过时，DFRoot 会停在：

```text
Triggering hook...
timeout: ERROR - check logcat & dmesg
```

临时写入 `log.tag.init=I` 后，只对当前开机周期允许 init 的 INFO 日志，DFRoot 的触发路径可以执行，提权就能继续。这里使用的是不带 `persist.` 的临时属性，重启后会自动消失；不会永久改变系统配置，也不会因为持久化日志设置影响其他软件。

## 每次重启后的操作

固定流程如下：

```text
重启 → 解锁 → 连接 ADB → 输入临时命令 → 打开 DFRoot → 点击“启动 Root” → 成功后恢复临时属性
```

### 1. 连接 ADB

在电脑打开“命令提示符”（`cmd.exe`），选择下面一种连接方式。执行 `adb devices` 后，设备状态显示为 `device` 才算连接成功。

**USB 数据线连接（推荐）**

1. 手机打开“设置 → 系统和更新 → 开发人员选项”，开启“USB 调试”。
2. 用支持数据传输的 USB 数据线连接手机和电脑。
3. 手机弹出“允许 USB 调试”时，点击“允许”。
4. 在电脑执行：

```cmd
adb devices
```

**无线调试连接**

1. 手机打开“开发人员选项 → 无线调试”。首次使用时点击“使用配对码配对设备”，记下手机显示的 IP、配对端口和配对码。
2. 在电脑执行，并按提示输入配对码：

```cmd
adb pair 手机IP:配对端口
```

3. 回到无线调试主页面，查看“IP 地址和端口”，然后执行：

```cmd
adb connect 手机IP:连接端口
adb devices
```

配对端口只用于 `adb pair`，连接端口只用于 `adb connect`。已经显示为 `device` 时，直接进入下一步；手机重启后无线端口变化时，重新执行 `adb connect`。

### 2. 写入临时日志属性

在电脑命令提示符执行：

```cmd
adb shell setprop log.tag.init I
adb shell getprop log.tag.init
```

第二条命令必须输出：

```text
I
```

这表示当前开机周期的临时设置已生效。`persist.log.tag.init` 是另一个会跨重启保留的系统属性名，不是本步骤要输入的内容。本临时方案只使用 `log.tag.init`；不要执行 `adb shell setprop persist.log.tag.init I`，因为持久化日志设置可能影响依赖系统日志级别的软件。

### 3. 执行 DFRoot

保持手机解锁，在手机上：

1. 打开 DFRoot。
2. 只点击一次“启动 Root”。
3. 等待结果完整出现。
4. 打开 SukiSU，确认 KernelSU 已加载并识别 Root。

不要在同一轮连续点击按钮。如果仍然显示 `timeout`，先检查上一步是否确实回读为 `I`，再保留完整日志。

### 4. 成功后恢复临时属性

确认 SukiSU 已识别 Root 后，在电脑命令提示符执行：

```cmd
adb shell setprop log.tag.init ""
adb shell getprop log.tag.init
```

最后一条命令应为空。这样只保留本次已经加载的 Root 状态，不让临时日志级别继续影响其他软件。

如果恢复命令没有权限或回读仍为 `I`，可以在 DFRoot 成功、SukiSU 已授权后使用 Root shell：

```cmd
adb shell su -c "setprop log.tag.init ''"
adb shell su -c "getprop log.tag.init"
```

最后一条命令应为空。

## 重要区别

- 每次重启后，Root 状态会消失，需要重新执行本说明的临时流程。
- 每次重启后，都需要重新输入 `adb shell setprop log.tag.init I`。
- 成功后可以恢复 `log.tag.init` 为空。
- 本临时方案不写入 `persist.log.tag.init`，也不需要持久化设置。
- 不要把下面这条命令作为本方案使用：

```cmd
adb shell setprop persist.log.tag.init I
```

## 最短命令流程

每次重启、ADB 已连接后，依次执行：

```cmd
adb shell setprop log.tag.init I
adb shell getprop log.tag.init
```

确认输出 `I` 后，打开 DFRoot，点击一次“启动 Root”。SukiSU 识别 Root 后，再执行：

```cmd
adb shell setprop log.tag.init ""
adb shell getprop log.tag.init
```

回读为空表示临时属性已恢复。

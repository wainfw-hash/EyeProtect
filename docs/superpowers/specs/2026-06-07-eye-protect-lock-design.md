# 护眼定时锁屏 App — 设计文档

## 概述

一款 Android 定时锁屏护眼应用。用户设定工作时长，倒计时结束后弹出提示，5 秒后自动执行系统锁屏，强制休息以保护视力。

## 技术栈

| 层面 | 技术 | 原因 |
|------|------|------|
| UI | Jetpack Compose | 现代声明式 UI，开发效率高 |
| 架构 | MVVM (ViewModel + StateFlow) | 官方推荐，状态管理清晰 |
| 后台 | Foreground Service | 前台服务保证计时不被系统杀死 |
| 锁屏 | DevicePolicyManager.lockNow() | 比 AccessibilityService 更稳定，一次授权永久有效 |
| 本地存储 | DataStore | 存储用户偏好设置 |
| 统计记录 | Room + Flow | 本地数据库存储锁屏记录，Flow 响应式读取 |

## 功能需求

### 核心功能
1. 两种模式：倒计时模式（单次）/ 循环定时模式（工作→锁屏→休息→下一轮）
2. 设置工作时长（默认 25 分钟，步长 5 分钟）
3. 循环模式支持设置休息时长（默认 5 分钟，步长 5 分钟）
4. 锁屏前弹出全屏遮罩，倒计时 5 秒后自动锁屏（不可取消）
5. 调用系统锁屏（DevicePolicyManager.lockNow()）
6. 通知栏常驻显示剩余时间
7. 记录今日锁屏次数和累计工作时长

### 用户体验
1. 主界面卡片式布局，所有操作一目了然
2. 一键开始/停止
3. 实时倒计时更新
4. 首次启动引导设备管理员权限

## 架构设计

### 应用分层

```
┌─────────────────────────────────────┐
│  UI 层 (Jetpack Compose)             │
│  ├── MainScreen       主界面          │
│  └── LockOverlay      锁屏前遮罩      │
├─────────────────────────────────────┤
│  ViewModel层                          │
│  └── MainViewModel    UI 状态管理     │
├─────────────────────────────────────┤
│  Service 层                           │
│  └── TimerService     前台服务/计时器  │
├─────────────────────────────────────┤
│  数据层 (Room + DataStore)           │
│  ├── StatDao          统计读写        │
│  └── StatRepository   数据仓库封装     │
├─────────────────────────────────────┤
│  系统层                               │
│  └── DeviceAdminReceiver  设备管理器   │
└─────────────────────────────────────┘
```

### 包结构

```
com.eye.protect/
├── MainActivity.kt
├── EyeProtectApp.kt (Application)
├── ui/
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   ├── screen/
│   │   ├── MainScreen.kt
│   │   └── LockOverlay.kt
│   └── viewmodel/
│       └── MainViewModel.kt
├── service/
│   └── TimerService.kt
├── receiver/
│   └── DeviceAdminReceiver.kt
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── LockRecord.kt
│   │   └── StatDao.kt
│   ├── repository/
│   │   └── StatRepository.kt
│   └── settings/
│       └── SettingsDataStore.kt
├── model/
│   ├── TimerMode.kt
│   └── MainUiState.kt
└── util/
    └── Constants.kt
```

## UI 设计

### 主界面

简洁卡片式布局，包含：
- 顶部：标题 + 今日统计（锁屏次数、累计时长）
- 中间：模式选择（倒计时/循环）+ 时长设置
- 中部：大启动/停止按钮 + 实时剩余时间
- 底部：设置入口

### 锁屏遮罩

全屏半透明黑色背景，中央显示：
- 护眼图标
- "该休息啦！" 提示文字
- 圆形进度条展示倒计时
- 5 秒倒计时数字

### 通知栏

常驻通知显示：
- 标题：🔒 护眼锁屏
- 内容：剩余 XX:XX
- 操作按钮：停止

## 数据流

### 倒计时流程
```
用户点击开始
    → MainViewModel 更新状态 (isRunning=true)
    → TimerService 启动 (ForegroundService)
        → CountDownTimer 每秒回调
        → 更新通知栏剩余时间
    → 倒计时归零
    → TimerService 发送广播/LiveData
    → MainActivity 收到 → 显示 LockOverlay
    → 5 秒倒计时
    → 关闭 LockOverlay
    → DevicePolicyManager.lockNow()
    → Room 写入一条锁屏记录
    → (循环模式) 休息倒计时 → 下一轮
    → (倒计时模式) 结束
```

### 统计查询
```
MainViewModel 启动时
    → 查询当日所有 LockRecord
    → count(*) → 今日锁屏次数
    → sum(workDuration) → 今日累计时长
    → 更新 UiState → Compose 自动刷新
```

## 关键实现细节

### DevicePolicyManager 锁屏

```kotlin
// 在 AndroidManifest 中声明设备管理器
// 需要在 res/xml/device_admin.xml 中定义策略

// 请求权限
val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "用于执行锁屏操作")
startActivity(intent)

// 执行锁屏 (一行代码)
devicePolicyManager.lockNow()
```

### LockOverlay 全屏覆盖

```kotlin
// 使用 Dialog 或 WindowManager
// 主题设为全屏透明，覆盖状态栏
// FLAG_NOT_TOUCHABLE 防止取消
// FLAG_KEEP_SCREEN_ON 保持亮屏
```

## 错误处理与边界情况

1. **未授权设备管理器**：启动时检测，若无权限则引导用户开启，授权前禁止开始
2. **手机重启后**：停止所有计时，恢复初始状态
3. **应用被系统杀死**：前台 Service 优先级最高，一般不会被杀；重启后服务不自动恢复
4. **倒计时时切到其他 App**：TimerService 仍在运行，通知栏可见，到时仍会锁屏
5. **屏幕已锁/正在解锁时倒计时到期**：先锁屏遮罩 → 用户点亮屏幕看到遮罩 → 5 秒后再次锁屏（已锁则无操作）
6. **循环模式暂停恢复**：单次轮次不可中断，停止按钮仅阻止下一轮

## 约束

- 最低 SDK 版本：Android 8.0 (API 26)
- 目标 SDK 版本：Android 14 (API 34)
- 需要权限：`RECEIVE_BOOT_COMPLETED`（可选）、设备管理员权限
- 不需要：无障碍服务、网络权限、定位权限

## 里程碑

1. 项目脚手架搭建（Gradle、包结构、依赖配置）
2. 核心 UI 实现（主界面、主题、设置交互）
3. TimerService + 前台通知
4. DeviceAdminReceiver + 锁屏功能
5. LockOverlay 遮罩组件
6. Room 统计记录 + 今日展示
7. 循环模式逻辑
8. 引导页/权限引导
9. 测试与打磨

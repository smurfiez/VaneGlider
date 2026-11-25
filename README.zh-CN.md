# VaneGlider

Android 上的 AirPlay 发送端 —— 把手机屏幕（和声音）投到 Mac、Apple TV 或任何
AirPlay 接收端。

本项目是 [Kite](https://github.com/itsHenry35/Kite) 的分支，修复了第三方接收端
的兼容问题、增加了手机静音，并换了新的视觉风格。

[English](README.md) | 简体中文

## 发行版

[![GitHub Release](https://img.shields.io/github/release/smurfiez/VaneGlider?style=flat-square&label=latest)](https://github.com/smurfiez/VaneGlider/releases)

## 截图

<p align="center">
  <img src="images/home.png" width="270" alt="接收端发现">
  <img src="images/settings.png" width="270" alt="设置">
</p>

## 功能

- 把手机屏幕投到 Mac、Apple TV 或任何 AirPlay 接收端
- 声音同播：长按接收端选择 画面和声音 / 仅画面 / 仅声音
- Mac 和 Apple TV 首次输入屏幕上的 PIN，之后每次连接自动完成
- 分辨率 / 帧率 / 码率可调，码率最高 200 Mbps
- 自动发现同一 Wi-Fi 下的接收端，也兼容 UxPlay 等开源接收端和 Android 投影仪
- 投屏详情（分辨率、帧率、码率）显示在常驻通知里，应用界面只保留接收端列表
- 投屏期间自动静音手机媒体音量，结束后恢复；即使进程被杀，下次启动也会恢复
- 快捷设置磁贴、一键分享日志

## 本分支的改动

- **AirPlay 与 RAOP 广告端口不同的第三方接收端。** Apple 的 AirPlay 1 镜像走
  `_raop` 广告的 RTSP 服务，而 Kite 一直连接 `_airplay` 的端口。XGIMI 等投影仪
  在该端口上只放了一个桩服务，对任何 RTSP 方法都返回空的 `200 OK`，于是
  `POST /fp-setup` 拿到零字节，投屏以 `m2: length 0, want 142` 失败。现在发现
  阶段会为同一主机采用 `_raop` 的 RTSP 端口（要求 RAOP 的 `ft` 位声明支持镜像），
  同时把重复的「仅音频」条目合并成一台设备。
- **不再双重出声。** 内录（playback capture）只是旁路取音，手机本身仍在播放
  发送出去的声音。现在只要投屏包含音频就会静音媒体音量，并持久化原始音量，
  进程被杀死后下次启动也能恢复。
- **新的视觉风格。** 以暖色墨黑为底、单一琥珀色「播出中」强调色，取代系统
  动态取色；更紧凑的字体与等宽的数字排版；各页面统一紧凑标题栏；明暗两套
  配色都经过对比度校验。
- **状态展示归位。** 状态面板只在连接、配对和失败时出现；投屏过程中常驻通知
  显示 `864 × 1920 @ 30 fps · 1.4 Mbps`，应用界面只留接收端列表和停止按钮。

## 性能

- **低延迟编码。** 编码器以实时优先级运行 High profile（CABAC、无 B 帧），
  在 Android 11 及以上启用低延迟提示；厂商编码器拒绝某个参数时逐级回退
  （High 5.2 → 4.1，再到 Baseline）。关键帧间隔 5 秒，并按需请求。
- **单一路写者。** 视频与音频各自跑在高优先级发送线程上，每个视频包以一次
  `[128 字节头 | 负载]` 写入发出，反馈心跳不会插进一帧中间。
- **加密开销更低。** 视频使用平台自带的 `ChaCha20-Poly1305`；音频加密与 ALAC
  编码复用缓冲区，不再每 8 ms 分配一次。
- **只看对的信号的拥塞控制。** 自适应码率只对写入 socket 的阻塞时间做出反应，
  CPU 峰值不会再被误判成网络拥塞。任何丢帧都会立即降码率并请求新的关键帧，
  参考帧缺失的 P 帧不会被解码。
- **Wi-Fi 不休眠、有优先级。** 投屏期间持有低延迟 Wi-Fi 锁避免省电降频，视频
  流量标记 DSCP AF41、音频标记 EF，发送缓冲区按约 125 ms 的码流大小设置，
  让背压先于延迟出现。

## 要求

- Android 10 及以上（minSdk 29）
- 一台与接收端处于同一 Wi-Fi 网络的真实设备
- AirPlay 接收端

## 构建

```sh
./gradlew :app:assembleDebug
```

Android SDK 通过 `ANDROID_HOME` 或 `local.properties` 指定。

## 发布

版本号在 `app/build.gradle.kts`（`appVersionName` 与 `versionCode`），发布标签
需要与之对应。Release 包通过环境变量签名，密钥不会进入仓库：

```sh
. ./signing.env          # VANE_KEYSTORE_PATH / VANE_KEYSTORE_PASSWORD /
                         # VANE_KEY_ALIAS / VANE_KEY_PASSWORD（已在 .gitignore）
./gradlew :app:assembleRelease
gh release create v2.0.0 app/build/outputs/apk/release/app-release.apk \
  --title "VaneGlider 2.0.0" --notes-file CHANGELOG.md
```

没有这些变量时 `assembleRelease` 仍可构建，只是包未签名。请备份
`~/.android/vaneglider-release.jks`：后续更新必须用同一密钥，丢失后用户只能
卸载重装才能升级。

## 致谢

VaneGlider 建立在多年开源 AirPlay 工作之上，也建立在 Kite 之上。感谢：

- [Kite](https://github.com/itsHenry35/Kite) —— 本分支基于的上游 Android
  AirPlay 发送端
- [doubletake](https://github.com/omarroth/doubletake) —— Go 实现的 AirPlay
  发送端；协议层的部分代码移植自该项目
- [UxPlay](https://github.com/FDH2/UxPlay) —— 开源 AirPlay 接收端；开发过程中
  的主要参考
- [pyatv](https://github.com/postlund/pyatv) —— AirPlay 2 协议参考
- [shairport-sync](https://github.com/mikebrady/shairport-sync) —— 配对与时间
  同步参考
- [openairplay](https://github.com/openairplay/airplay-spec) —— 早期协议文档

## 许可

VaneGlider 以 GNU LGPL-3.0 发布，见 [LICENSE](LICENSE)。移植代码沿用
doubletake 的许可。开源声明也可在应用内「设置 → 关于」中查看。

import type { TranslationSchema } from './types';

export const zhCn: TranslationSchema = {
  meta: {
    title: 'VaneGlider — Android AirPlay 投屏到 Mac、Apple TV 与音箱',
    description:
      '在同一 Wi-Fi 下，把 Android 屏幕与声音投到 Mac、Apple TV、投影仪或 AirPlay 音箱；无广告、无回声，免费开源。',
    ogLocale: 'zh_CN',
    switchLabel: 'English',
    switchAriaLabel: '切换到英文',
  },
  nav: {
    features: '功能亮点',
    devices: '兼容设备',
    howItWorks: '上手指南',
    help: '常见问题',
    downloadCta: '下载安装包',
    themeToggleLabel: '切换浅色 / 深色主题',
    menuOpenLabel: '打开菜单',
    menuCloseLabel: '关闭菜单',
  },
  hero: {
    pillPrefix: 'v',
    pillSuffix: ' · 免费开源 · Android 10 及以上',
    headline: '把 Android 屏幕与声音，轻松投到更大的屏幕',
    subhead:
      '同一 Wi-Fi 下，一键将手机画面和声音投射到 Mac、Apple TV、投影仪或 AirPlay 音箱——无广告、无需账号，也不会双端同时出声。',
    primaryCta: '下载安装包 (APK)',
    secondaryCta: '查看兼容设备',
    metaLine: '官方签名安装包 · 仅局域网直连 · GNU LGPL-3.0 开源',
    previewLabel: '点按体验「正在投射」状态',
    previewLivePrefix: '正在投射至',
    previewAudioLockedNote: '仅音频音箱——已自动选择「仅声音」',
    sampleReceivers: [
      {
        id: 'mac',
        name: '客厅的 Mac',
        model: 'MacBook Pro',
        type: 'mac',
        paired: true,
        audioOnly: false,
      },
      {
        id: 'tv',
        name: '卧室 Apple TV',
        model: 'Apple TV 4K',
        type: 'tv',
        paired: false,
        audioOnly: false,
      },
      {
        id: 'speaker',
        name: '厨房 HomePod',
        model: '仅音频',
        type: 'speaker',
        paired: false,
        audioOnly: true,
      },
    ],
    modes: [
      { id: 'mirror', label: '画面与声音' },
      { id: 'video', label: '仅画面' },
      { id: 'audio', label: '仅声音' },
    ],
  },
  benefits: {
    eyebrow: '为什么选择 VaneGlider',
    heading: '六个日常细节，让投屏更顺手。',
    items: [
      {
        title: '画面、声音，或两者兼顾',
        body: '点按支持镜像的设备即可同时投射画面与声音；长按 Mac、Apple TV 等设备，可自由切换「仅画面」或「仅声音」。',
        icon: 'layers',
      },
      {
        title: '不再双端出声',
        body: '投射声音时会自动静音手机媒体音量，彻底告别双端回声；结束投射后自动恢复原音量，即使应用意外崩溃也能恢复。',
        icon: 'mute',
      },
      {
        title: '自动发现接收端',
        body: '打开即自动搜索同一 Wi-Fi 下的 AirPlay 设备，支持投影仪和开源接收端。首次输入 4 位配对码后自动记住，下次直接连接。',
        icon: 'wifi',
      },
      {
        title: '适配你的屏幕与 Wi-Fi',
        body: '最高支持原生分辨率与 120 fps；码率保持「自动」即可在 Wi-Fi 拥挤时平滑自适应，兼顾清晰度与流畅度。',
        icon: 'gauge',
      },
      {
        title: '为日常使用而生',
        body: '支持下拉快捷设置磁贴一键开投；常驻通知栏实时显示分辨率、帧率与码率并可一键停止；内置 11 种界面语言。',
        icon: 'bolt',
      },
      {
        title: '隐私优先',
        body: '画面与音频全程在本地 Wi-Fi 传输，不经过云端服务器。无需注册账号、无广告、无数据追踪，遵循 GNU LGPL-3.0 开源发布。',
        icon: 'shield',
      },
    ],
  },
  castModes: {
    eyebrow: '三种投放方式',
    heading: '全屏有声、静默演示，或只播音乐——一按切换。',
    subtitle: '要投放什么内容？',
    tabs: [
      {
        id: 'mirror',
        title: '画面与声音',
        summary: '镜像手机屏幕，并把手机上播放的声音发送到接收端。',
        bestFor:
          '适合在电视、Mac 或投影仪上看视频、玩游戏或演示带声内容。手机端自动静音并在结束后恢复；若接收端不支持音频，会自动转为无声画面继续投屏。',
        triggerHint: '点按镜像接收端默认开启',
        phoneAudioBadge: '手机扬声器已静音',
      },
      {
        id: 'video',
        title: '仅画面',
        summary: '镜像手机屏幕，但不发送声音。',
        bestFor:
          '手机本地播放与音量完全不受影响——适合会议室演示文稿、展示照片，或戴着耳机听歌的同时投屏。',
        triggerHint: '长按 Mac 或 Apple TV 即可选择',
        phoneAudioBadge: '手机声音不受影响',
      },
      {
        id: 'audio',
        title: '仅声音',
        summary: '只发送手机声音，不镜像屏幕。',
        bestFor:
          '让 HomePod、AirPlay 音箱或 Mac 秒变 Android 无线扬声器。手机会自动静音，声音只从音箱传出。',
        triggerHint: '点按音箱默认开启 · 长按 Mac/Apple TV 可选',
        phoneAudioBadge: '只从音箱出声',
      },
    ],
  },
  compatibility: {
    eyebrow: '设备兼容性',
    heading: '兼容 Apple 生态、开源接收端与智能投影仪',
    subtitle: '六类接收端、三种投放方式——点按之前先看清支持情况。',
    filters: [
      { id: 'all', label: '全部' },
      { id: 'video', label: '画面与声音' },
      { id: 'audio', label: '仅音频' },
    ],
    columns: {
      receiver: '接收端',
      screenSound: '画面 + 声音',
      screenOnly: '仅画面',
      soundOnly: '仅声音',
      notes: '说明',
    },
    badges: {
      yes: '支持',
      varies: '视情况',
      no: '不支持',
    },
    rows: [
      {
        id: 'mac',
        name: 'Mac',
        category: 'video',
        screenSound: 'yes',
        screenOnly: 'yes',
        soundOnly: 'yes',
        note: '首次需要输入 4 位 PIN 配对',
      },
      {
        id: 'apple-tv',
        name: 'Apple TV',
        category: 'video',
        screenSound: 'yes',
        screenOnly: 'yes',
        soundOnly: 'yes',
        note: '首次需要输入 4 位 PIN 配对',
      },
      {
        id: 'uxplay',
        name: 'UxPlay 等开源接收端',
        category: 'video',
        screenSound: 'yes',
        screenOnly: 'yes',
        soundOnly: 'yes',
        note: '可能需要旧版配对或 AirPlay 密码',
      },
      {
        id: 'projector',
        name: 'Android 投影仪及其他 AirPlay 1 镜像接收端（如极米）',
        category: 'video',
        screenSound: 'varies',
        screenOnly: 'yes',
        soundOnly: 'varies',
        note: '取决于接收端是否声明音频支持；2.0 已修复多端口投影仪',
      },
      {
        id: 'homepod',
        name: 'HomePod 等 AirPlay 2 音箱',
        category: 'audio',
        screenSound: 'no',
        screenOnly: 'no',
        soundOnly: 'yes',
        note: '仅支持音频',
      },
      {
        id: 'raop',
        name: '经典 AirPlay（RAOP）音箱',
        category: 'audio',
        screenSound: 'no',
        screenOnly: 'no',
        soundOnly: 'yes',
        note: '仅支持音频；可能需要 AirPlay 密码',
      },
    ],
  },
  gettingStarted: {
    eyebrow: '快速上手',
    heading: '不到一分钟即可开始投屏',
    steps: [
      {
        step: '01',
        title: '安装并连入同一 Wi-Fi',
        body: '在 Mac、Apple TV、投影仪或音箱上开启 AirPlay 接收，并确保与手机连入同一 Wi-Fi。打开已签名的安装包完成安装（若系统提示请允许安装未知来源应用）。',
      },
      {
        step: '02',
        title: '点按设备并允许屏幕录制',
        body: '点按列表中的设备开始投射（长按 Mac 或 Apple TV 可选其他模式），并允许 Android 的屏幕录制授权。注意：每次投放都需要确认该弹窗（仅声音模式也不例外），因为系统内录与录屏共用同一授权。',
      },
      {
        step: '03',
        title: '输入一次 4 位配对码，随后随处控制',
        body: '首次连接 Mac 或 Apple TV 时输入屏幕上的 4 位配对码（若设了 AirPlay 密码一并输入），之后会自动记住。随时可通过应用内按钮、常驻通知或快捷设置磁贴停止。',
      },
    ],
    permissionsTitle: 'VaneGlider 为什么要申请这些权限',
    permissionsSubtitle: '用大白话说清每一个 Android 权限的用途。',
    permissions: [
      {
        name: '麦克风',
        scope: '所有版本（Android 10+）',
        reason:
          'Android 使用该权限授权「应用内声音抓取」。VaneGlider 绝不会录制麦克风输入。',
      },
      {
        name: '附近的设备',
        scope: 'Android 12+ / 13+',
        reason: 'Android 12+ 用于蓝牙发现，Android 13+ 用于搜索同一局域网下的接收端。',
      },
      {
        name: '位置信息',
        scope: '仅 Android 10–11',
        reason: '仅在 Android 10–11 上由系统要求用于蓝牙发现，不会记录或上传位置。',
      },
      {
        name: '通知',
        scope: 'Android 13+',
        reason: '用于显示正在投射的分辨率、帧率、码率以及一键停止按钮。',
      },
      {
        name: '屏幕录制',
        scope: '每次新投屏',
        reason:
          'Android 每次开启新投屏都会重新询问（仅声音模式同样需要），因为系统将屏幕与音频抓取合并在同一个授权中。',
      },
    ],
  },
  quality: {
    eyebrow: '画质设置',
    heading: '画质与流畅度，一目了然',
    subtitle: '按当前 Wi-Fi 调整画质，也可以保持「自动」让 VaneGlider 自行适配。',
    resolutionLabel: '分辨率',
    fpsLabel: '帧率',
    bitRateLabel: '码率',
    bitRateAuto: '自动',
    bitRateCustom: '自定义',
    resolutions: [
      {
        value: 'native',
        label: '原生',
        hint: '与手机屏幕逐像素一致，适合高速 5 GHz/6 GHz Wi-Fi。',
      },
      {
        value: '1080p',
        label: '1080p',
        hint: '文字清晰与流畅度的最佳平衡，适合日常投屏（默认）。',
      },
      {
        value: '720p',
        label: '720p',
        hint: '适合较旧机型、拥挤的 Wi-Fi，或需要更长续航时使用。',
      },
      {
        value: '540p',
        label: '540p',
        hint: '超轻量，适合信号较弱或拥塞的路由器。',
      },
    ],
    fpsOptions: [24, 30, 60, 90, 120],
    liveEstimateLabel: '预估码率',
    autoAdaptNote: 'Wi-Fi 拥挤时会平滑降码率',
    manualRangeNote: '手动范围为 2–200 Mbps，滑块默认从 30 Mbps 开始。',
    systemTips: [
      {
        title: '电池优化',
        status: '排除',
        body: '息屏或 VaneGlider 在后台运行时，Doze 可能让投屏卡住数秒。进入「设置 → 电池优化」，将其设为「排除」。',
      },
      {
        title: '已保存的接收端',
        body: '随时在「设置 → 已保存的接收端」查看或移除已配对设备。',
      },
      {
        title: '日志',
        body: '排查接收端问题时，可在「设置 → 日志」导出一份干净的会话日志。',
      },
    ],
  },
  faq: {
    eyebrow: '帮助与故障排查',
    heading: '常见问题与排查指南',
    searchPlaceholder: '搜索问题与答案',
    emptyResult: '没有找到相关内容——换个关键词，或到 GitHub 提交 issue。',
    items: [
      {
        id: 'no-receivers',
        q: '找不到接收端怎么办？',
        a: '确认手机与接收端连的是同一 Wi-Fi（不是访客网络），且接收端已开启 AirPlay 接收。 • 允许「附近的设备」权限（Android 13+ 用于局域网，Android 12+ 用于蓝牙）或「位置信息」（Android 10–11）。 • 检查路由器是否开启了 AP 隔离 / 客户端隔离，这会阻断局域网设备发现。',
      },
      {
        id: 'no-sound',
        q: '投屏后接收端没有声音？',
        a: '确认开始的是「画面与声音」而不是「仅画面」。 • 确认已授予「麦克风」权限（Android 用它来抓取应用内声音，绝不会录制麦克风）。 • 调高接收端音量——带声音投放期间手机媒体音量会被刻意静音。 • 注意：少数应用（如部分 DRM 流媒体或银行应用）明确禁止 Android 内录，通过 AirPlay 投放时会保持无声。',
      },
      {
        id: 'lag',
        q: '画面卡顿、延迟或频繁断开？',
        a: '在 VaneGlider 内打开「设置 → 电池优化」，设为「排除」，避免息屏或切换应用时被 Android Doze 暂停。 • 码率保持「自动」（或在设置中降低分辨率 / 帧率），并尽量靠近 Wi-Fi 路由器。',
      },
      {
        id: 'pairing',
        q: '配对码失败或之前配对过的设备连不上？',
        a: '输入 Mac 或 Apple TV 屏幕上显示的 4 位配对码（若设置了 AirPlay 密码也一并输入）。 • 若多次输错触发临时限制，可关闭再开启接收端的 AirPlay 接收（或等待约 30 分钟）后重试。 • 若 VaneGlider 提示保存的配对已失效，进入「设置 → 已保存的接收端」，忘记该设备后重新配对。',
      },
      {
        id: 'sound-only-permission',
        q: '为什么「仅声音」模式也要确认屏幕录制权限？',
        a: 'Android 将屏幕录制与系统音频抓取合并在同一个 MediaProjection 授权弹窗中，且每次开启新投屏都需要重新确认。',
      },
      {
        id: 'session-ended',
        q: '接收端主动断开，或某个设备显示不可点击？',
        a: '确认接收端已唤醒且没有其他 AirPlay 流正在占用。既未声明镜像、也未声明音频的设备会保持不可点击；标注「不支持镜像」的设备只接受声音。',
      },
      {
        id: 'kite-upgrade',
        q: '从 Kite 升级到 VaneGlider 2.0 为什么出现两个应用？',
        a: 'VaneGlider 2.0 使用独立包名（com.smurfiez.vaneglider）与签名，因此会与原版 Kite 并存安装，而不是覆盖。确认使用无误后，可卸载旧版 Kite。',
      },
      {
        id: 'report',
        q: '问题仍未解决？如何反馈？',
        a: '到 GitHub Issues（https://github.com/smurfiez/VaneGlider/issues）提交 issue，并附上「设置 → 日志」中的日志包，以及 VaneGlider 版本号和接收端型号。',
      },
    ],
  },
  download: {
    eyebrow: '下载',
    heading: '现在就开始投屏。',
    body: '下载已签名安装包，安装到 Android 10 及以上设备，即可投屏到最近的 AirPlay 接收端。',
    primaryCta: '下载安装包 (APK)',
    allReleases: '全部版本',
    kiteTitle: '从 Kite 升级？',
    kiteBody:
      'VaneGlider 2.0 使用独立包名（com.smurfiez.vaneglider）与签名，因此会与原版 Kite 并存安装，不会覆盖。确认无误后可卸载旧版。',
    whatsNewTitle: '2.1.0 新功能',
    whatsNewItems: [
      '发送链路重写：每个视频包一次串行写，心跳不再插入画面包中间。',
      '原生 ChaCha20-Poly1305 加密，ALAC 编码与 RTP 封包零分配，投屏 CPU 占用大幅下降。',
      '自适应码率只看 socket 背压，CPU 繁忙不再误判拥塞、误砍健康链路。',
      '独立高优先级发送线程与关键帧看门狗，丢帧后恢复更快。',
    ],
  },
  footer: {
    tagline: 'VaneGlider — 开源 Android AirPlay 发送端，GNU LGPL-3.0。',
    projectHeading: '项目与贡献',
    languagesHeading: '界面语言',
    creditsHeading: '致谢',
    links: {
      github: 'GitHub 仓库',
      releases: '全部版本',
      changelog: '更新日志',
      contributing: '技术设计与贡献指南',
      issues: '反馈问题',
    },
    languagesList:
      'English, 简体中文, 繁體中文, 日本語, 한국어, Deutsch, Français, Español, Português (Brasil), Русский, Italiano',
    creditsNote:
      '协议实现基于 Kite、doubletake、UxPlay、pyatv、shairport-sync 与 AirPlay 规范，感谢每一位上游维护者。',
    licensesLine:
      '内置开源库：AndroidX & Jetpack Compose（Apache-2.0）、Kotlin & kotlinx.coroutines（Apache-2.0）、Material Design 3（Apache-2.0）、Bouncy Castle（MIT）、JmDNS（Apache-2.0）。',
  },
};

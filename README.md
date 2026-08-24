# 拯救睡眠 · Sleep Saver

<p align="center">
  <img src="design-assets/app-icon-v3-sleepcap-toy-master.png" width="150" alt="拯救睡眠原创玩偶图标">
</p>

> 为了拯救睡眠，我给自己做了一个睡眠打卡 APP。🌙

## 为什么做它？

我不太喜欢戴着手环睡觉，但又很想知道：我昨晚到底几点放下了手机？半夜有没有突然醒来，又顺手刷了一会儿？

于是，我给自己做了「拯救睡眠」。它现在的核心逻辑其实很简单：

- 🌙 睡前，主动打卡一次；
- ☀️ 早起，再主动打卡一次；
- 📱 两次打卡之间，如果夜里真的解锁了手机，APP 就记录这次行为。

第二天打开它，我就能看看昨晚休息了多久、夜里有没有拿起手机，以及睡前是不是又没忍住多刷了一会儿。

它当然不能像手环一样测量心率、血氧或深度睡眠，更像是一本会帮我留意手机使用情况的睡眠手帐。对我这种不喜欢戴设备睡觉的人来说，已经足够帮助我发现一些以前没注意到的习惯了。

它当然还在继续长大：功能不算多，界面也已经改了好多版，甚至连 APP 图标都反复调整过，哈哈哈。但把生活里真实遇到的问题，慢慢做成一个自己愿意每天使用的小工具，这个过程本身就很值得记录。

接下来我会把它当作一个长期自我实验，看看连续使用 7 天、14 天、30 天后，它到底能不能让我更早放下手机、慢慢建立规律作息。

> **当前版本：`1.4.1-dev` · 支持 Android 10 及以上**<br>
> 最近一晚可在今日页或打卡完成页直接纠正，历史记录可从手帐顶部选择晚次修改；日期、小时和分钟统一在一个编辑器中完成。这是正在验证中的个人项目，不是医疗设备，也不能代替专业睡眠诊断。

[⬇️ 直接下载 Android APK](https://github.com/jiahe140325-design/sleep-saver-android/releases/download/v1.4.1-dev/SleepSaver-1.4.1-dev.apk) · [查看版本说明](https://github.com/jiahe140325-design/sleep-saver-android/releases/tag/v1.4.1-dev) · [查看完整更新记录](CHANGELOG.md)

### 这一版做了什么？

- ⏰ 打卡页跟随睡眠定时切换：活跃睡眠始终优先显示早起打卡，超过计划起床时间又没有记录时提示「昨晚漏记」。
- ✍️ 今日页和打卡完成页提供最近一晚的直接纠正入口；少于 1 小时或超过 16 小时的明显异常会突出提醒。
- 📔 手帐顶部提供「纠正历史记录」，先选择晚次再编辑；悬浮详情内原有修改按钮继续作为次级入口。
- 🕒 准备休息与实际起床统一在一个编辑器内修改日期、小时和分钟，并支持按 10 分钟或 1 小时快捷调整，不再嵌套系统日期和时钟弹窗。
- 🧾 原始点击时间、纠正时间与当晚计划快照会保留在 Room v3 和 JSON / CSV 导出中，便于回看修改历史。
- 🔒 好友功能仍显示「暂未开放」；睡眠数据继续只保存在手机本地，不声明 `INTERNET` 权限。

## 下载与安装

### 支持范围

- 支持 Android 10（API 29）及以上的标准安卓设备，不是小米专用 APP。
- 当前只在小米 14 Pro / HyperOS 上完成了实际安装验证。
- 其他品牌安卓手机原则上可以安装，但暂未逐一真机测试；不同厂商的安装授权、使用情况访问和后台提醒设置可能略有差异。

### 通用安卓安装步骤

1. 点击 [直接下载 `SleepSaver-1.4.1-dev.apk`](https://github.com/jiahe140325-design/sleep-saver-android/releases/download/v1.4.1-dev/SleepSaver-1.4.1-dev.apk)。如果直链没有开始下载，也可以前往 [版本发布页](https://github.com/jiahe140325-design/sleep-saver-android/releases/tag/v1.4.1-dev)，展开 `Assets` 后选择同名 APK。不要下载 `Source code` 压缩包。
   - 成功标志：下载后的文件名以 `.apk` 结尾。
2. 在系统通知栏或「文件管理」的下载目录中点击 APK。如果系统询问是否允许当前应用安装未知来源应用，按照提示临时允许。
   - 成功标志：进入显示「拯救睡眠」名称和图标的系统安装页面。
3. 点击「安装」，等待系统完成处理。
   - 成功标志：页面显示「应用已安装」，并且桌面出现「拯救睡眠」图标。
4. 如果是在更新旧版本，请直接覆盖安装，不要先卸载。
   - 成功标志：打开新版后，原有本地睡眠记录仍然存在。

### 小米 / HyperOS 特别提示

- 如果出现「增强防护」提示，点击安装页面右上角菜单，选择「单次安装授权」，再继续安装。
- 如果微信把文件保存成类似 `SleepSaver-1.4.1-dev.apk.2`，请先在系统「文件管理」中将末尾的 `.2` 删除，确保文件名以 `.apk` 结尾后再打开。

## 核心功能

- 睡前、早起双打卡，记录两次主动行为之间的休息窗口。
- 基于 Android 使用情况权限，统计睡前和夜间手机使用侧写。
- 睡眠定时与本地通知提醒。
- 睡前、早起、昨晚漏记、昨晚已记录 4 种状态与 6 种睡前事件组成的小手帐式打卡。
- 今日睡眠时间线与本周休息目标。
- 睡眠时长、偏离计划双趋势图与可自由选择的统计时间段。
- 同页展开的单晚详情、历史时间纠正/恢复、计划时间快照与非破坏性数据库升级。
- JSON / CSV 数据导出与本地持久化。
- 好友入口暂时保留，当前显示「暂未开放」。

## 隐私原则

- 数据仅保存在设备本机的 Room / DataStore。
- 不接入 Supabase、腾讯云或其他外部数据库。
- V1 不声明 `INTERNET` 权限，不上传睡眠记录。
- 不申请定位、联系人、短信、电话、麦克风、相机或相册权限。
- 禁止 destructive migration，关闭 Android Auto Backup。

## 界面与版本演变

### 已归档实机界面 · `1.3.5-dev`

以下截图来自 `1.3.5-dev` 在小米 14 Pro / HyperOS 上的实际安装效果，拍摄于 2026 年 8 月 20 日，用于保留版本演变证据，**不代表 `1.4.1-dev` 当前界面**。点击图片可以查看原始完整截图。

<p align="center">
  <a href="docs/screenshots/actual/v1.3.5/01-today-xiaomi14pro.jpg"><img src="docs/screenshots/actual/v1.3.5/01-today-xiaomi14pro.jpg" height="460" alt="拯救睡眠 1.3.5-dev 小米 14 Pro 实机今日页"></a>
  <a href="docs/screenshots/actual/v1.3.5/02-checkin-bedtime-xiaomi14pro.jpg"><img src="docs/screenshots/actual/v1.3.5/02-checkin-bedtime-xiaomi14pro.jpg" height="460" alt="拯救睡眠 1.3.5-dev 小米 14 Pro 实机睡前打卡页"></a>
  <a href="docs/screenshots/actual/v1.3.5/03-journal-xiaomi14pro.jpg"><img src="docs/screenshots/actual/v1.3.5/03-journal-xiaomi14pro.jpg" height="460" alt="拯救睡眠 1.3.5-dev 小米 14 Pro 实机睡眠手帐完整长截图"></a>
  <a href="docs/screenshots/actual/v1.3.5/04-friends-xiaomi14pro.jpg"><img src="docs/screenshots/actual/v1.3.5/04-friends-xiaomi14pro.jpg" height="460" alt="拯救睡眠 1.3.5-dev 小米 14 Pro 实机好友暂未开放页"></a>
</p>

<p align="center"><sub>今日 · 睡前打卡 · 睡眠手帐（完整长截图）· 好友暂未开放</sub></p>

<details>
  <summary>查看 <code>1.2.2-dev</code> 早期实机截图</summary>

以下图片用于记录项目最初的安装效果，不代表当前界面。

<p>
  <img src="docs/screenshots/actual/01-today-xiaomi14pro.jpg" width="190" alt="拯救睡眠 1.2.2-dev 小米 14 Pro 实机今日页">
  <img src="docs/screenshots/actual/02-checkin-bedtime-xiaomi14pro.jpg" width="190" alt="拯救睡眠 1.2.2-dev 小米 14 Pro 实机睡前打卡页">
  <img src="docs/screenshots/actual/03-journal-xiaomi14pro.jpg" width="190" alt="拯救睡眠 1.2.2-dev 小米 14 Pro 实机睡眠手帐页">
  <img src="docs/screenshots/actual/04-friends-xiaomi14pro.jpg" width="190" alt="拯救睡眠 1.2.2-dev 小米 14 Pro 实机好友暂未开放页">
</p>
</details>

## 原始设计参考

<details>
  <summary>展开查看早期设计稿</summary>

这些图片用于表达最初的信息架构和视觉方向，不是当前版本的安装截图，也不代表已经按像素级还原。

<p>
  <img src="01-今日.png" width="150" alt="早期今日页设计参考">
  <img src="02-打卡睡前.png" width="150" alt="早期睡前打卡页设计参考">
  <img src="03-打卡早起.png" width="150" alt="早期早起打卡页设计参考">
  <img src="04-手帐.png" width="150" alt="早期手帐页设计参考">
  <img src="05-好友.png" width="150" alt="早期好友页设计参考">
</p>
</details>

## 质量与测试

- 32 项本地自动化逻辑测试全部通过。
- 测试覆盖通知亮屏、指纹解锁、跨零点、重复打卡、进程恢复、权限失效、提醒更新、近 7 晚布局、日期范围、打卡状态切换、首次漏记、时间纠正、历史晚次选择、直接数字输入、跨午夜快捷调整、异常入口与导出审计等情况。
- 自动化结果与小米 14 Pro 真机结果分开记录，不用模拟测试冒充真实设备验收。

详细结果见 [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md)。

## 本地构建

需要：

- JDK 17
- Android SDK 36
- Android Build Tools 36

执行：

```bash
./gradlew testDebugUnitTest assembleDebug
```

成功标志：测试通过，并生成 `app/build/outputs/apk/debug/SleepSaver-1.4.1-dev.29.apk`。

## 项目文档

- [`拯救睡眠-设计方案.md`](拯救睡眠-设计方案.md)：产品定位、双打卡闭环、数据口径、技术方案与测试场景。
- [`CHANGELOG.md`](CHANGELOG.md)：每次修改的版本、日期、内容与原因。
- [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md)：自动化测试、APK校验值与真机验证边界。

## 使用与授权

当前仓库未添加开源许可证。代码和设计可公开查看，但不代表自动授权复制、再发布或商业使用。

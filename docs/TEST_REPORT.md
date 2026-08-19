# Sleep Saver V1 测试报告

> 测试日期：2026-08-19
> 测试版本：`1.3.5-dev`
> 设备目标：小米 14 Pro
> 原则：逻辑自动化与真机验证分开报告，不用模拟结果冒充 HyperOS 真机结果。

## 1. 当前结论

- Gradle 构建：通过。
- 本地自动化测试：17 项执行，17 项通过，0 跳过，0 失败，0 错误。
- Debug APK：已生成。
- 静态权限检查：未声明 `INTERNET`；声明 `PACKAGE_USAGE_STATS` 与 `POST_NOTIFICATIONS`；自动备份关闭。
- 小米 14 Pro 真机：用户已提供 `1.3.4-dev` 今日页截图，透明玩偶和底部锁节点均已完整显示，但「09:12」与「夜间解锁」发生文字覆盖。`1.3.5-dev` 已将两组内容改为结构化行布局，待覆盖安装复测。打卡一屏和手帐票券修正也仍等待 `1.3.5-dev` 截图确认。
- UI 结构核验：今日页早起/解锁两组信息不再使用相互独立的绝对纵坐标，改为两个 44dp 行按顺序测量与排列；主卡仍保持 304dp，不影响一屏节奏。业务逻辑与数据格式未变。
- 数据边界核验：时间轴对 Usage Access 不可用的历史记录显示「不可用」，不会伪装成「0 次」；进行中的记录显示「待打卡 / 待结算」。

## 2. 产物

- APK：`app/build/outputs/apk/debug/SleepSaver-1.3.5-dev.apk`
- 文件大小：67,617,957 bytes（约 64.5 MiB，debug 测试包未做发布压缩）
- SHA-256：`55a165260849cb28456d5eb3e76762ea8cb1a5103bd5ed46c001ae083bbf6ec4`
- 单元测试 XML：`app/build/test-results/testDebugUnitTest/TEST-com.sleepsaver.app.SleepSaverLogicTest.xml`
- 启动图标：APK 已识别原创睡眠帽玩偶版 `mdpi`、`hdpi`、`xhdpi`、`xxhdpi`、`xxxhdpi` 五档 `ic_launcher.png`。
- 打卡页插画：`app/src/main/res/drawable-nodpi/checkin_journal_hero.png`，1200×600px；生成母版保存在 `design-assets/check-in-journal-hero-source.png`。
- 今日页玩偶：`app/src/main/res/drawable-nodpi/today_timeline_mascot.png`，420×420px；生成母版保存在 `design-assets/today-timeline-mascot-source.png`。
- 手帐页月度票券：`app/src/main/res/drawable-nodpi/journal_month_ticket.png`，1200px 宽；生成母版保存在 `design-assets/journal-month-ticket-source.png`。
- 手帐页历史记录纸：`app/src/main/res/drawable-nodpi/journal_history_paper.png`，1200px 宽；生成母版保存在 `design-assets/journal-history-paper-source.png`。
- 手帐页视觉基准：`design-assets/journal-screen-v1.3.2-reference.png`，用于后续小米 14 Pro 截图对照。
- 今日页真机 QA：`design-assets/qa/today-v1.3.2-xiaomi14pro.jpg`；与设计稿并排对照保存在 `design-assets/qa/today-v1.3.2-reference-comparison.png`。
- 今日页第二轮真机 QA：`design-assets/qa/today-v1.3.4-xiaomi14pro.jpg`；并排对照为 `design-assets/qa/today-v1.3.4-reference-comparison.png`。
- 打卡页真机 QA：`design-assets/qa/checkin-v1.3.3-xiaomi14pro.jpg`；并排对照为 `design-assets/qa/checkin-v1.3.3-reference-comparison.png`。
- 手帐页真机 QA：`design-assets/qa/journal-v1.3.3-xiaomi14pro.jpg`；并排对照为 `design-assets/qa/journal-v1.3.3-reference-comparison.png`。
- 今日页透明玩偶：`design-assets/today-timeline-mascot-transparent.png`，App 内同名资源已替换旧的方形底图版本。

## 3. 17 项覆盖情况

| # | 方案场景 | 自动化逻辑 | 小米 14 Pro 真机 |
|---|---|---|---|
| 1 | 通知亮屏但未解锁 | 通过：0 次 | 待验证 |
| 2 | 电源键只看时间 | 通过：0 次 | 待验证 |
| 3 | 指纹解锁后锁屏 | 通过：1 个 Session | 待验证 |
| 4 | 一次解锁内切换多个 App | 通过：仍为 1 次 | 待验证 |
| 5 | 夜里分别解锁 3 次 | 通过：3 次 | 待验证 |
| 6 | 睡前打卡后直接锁屏 | 通过：打卡后使用 0 分 | 待验证 |
| 7 | 打卡后切其他 App 20 分钟 | 通过：记录 20 分 | 待验证 |
| 8 | 通知亮屏后再真解锁 | 通过：1 次 | 待验证 |
| 9 | 夜里一次 + 早晨打开 Sleep Saver | 通过：早晨当前 Session 被排除 | 待验证 |
| 10 | 整晚无解锁，仅早晨打开 App | 通过：夜间 0 次 | 待验证 |
| 11 | Usage Access 被取消 | 通过：数据为不可用，不写 0 | 待验证 |
| 12 | 连点两次睡前打卡 | 通过：状态策略拦截重复创建 | 待验证 |
| 13 | App 在打卡后被杀掉 | 通过：持久化实体可恢复 ACTIVE 状态 | 待验证进程杀死 |
| 14 | 手机重启 | 通过：持久化实体仍满足继续早起打卡的状态规则 | 待验证重启 |
| 15 | App 升级 | 通过：禁止 destructive migration 的策略被测试锁定 | 待以后有 v2 数据库时做迁移真测 |
| 16 | 跨 00:00 | 通过：一个 Session、时长正确 | 待验证 |
| 17 | 修改提醒时间 | 通过：始终使用同一个 unique work 名称 | 待验证通知与时间更新 |

## 4. 后续真机复测步骤

1. 在小米 14 Pro 打开「开发者选项 → USB 调试」，用数据线连接电脑，并在手机上点「允许」。
   - 成功标志：`adb devices -l` 显示一台状态为 `device` 的设备。
2. 安装 debug APK，首次打开应用。
   - 成功标志：版本显示为 `1.3.5-dev`，底部有今日、打卡、手帐、好友四项。
3. 点击「去授权」，在 HyperOS 使用情况访问页面允许 Sleep Saver。
   - 成功标志：回到应用后紫色权限提示消失。
4. 逐项做上表的真机动作，重点复测通知亮屏、指纹解锁、早晨 Session 排除与打卡后继续刷手机。
   - 成功标志：每项结果与表中预期一致。
5. 开启提醒并允许通知，修改一次就寝时间。
   - 成功标志：只有一条提醒，不会重复通知。
6. 先截取完整「今日」页，确认夜间解锁节点和数值完整可见，玩偶周围没有方形底色；再打开未选择状态的「打卡」与数据管理收起状态的「手帐」页，截取首屏。
   - 成功标志：今日时间轴的「早起打卡 / 09:12」与「夜间解锁 / 0 次」两组文字之间有清晰空隙，不重叠也不裁切；打卡页不下拉也能看到全部操作；手帐月度票券数值不贴底。

## 5. 尚未完成的发布事项

- 当前是可安装的 debug 测试包，不是商店发布包。
- 正式发布前需要由用户保管的 release keystore；未在项目中硬编码密码或私钥。
- 小米 14 Pro 真机验收通过后，再生成并签名 release APK。
- 当前电脑未连接安卓设备且未安装模拟器，因此 `design-qa.md` 仍标记为 `blocked`；收到 `1.3.5-dev` 的今日、打卡和手帐页复测截图后，再完成视觉对照。

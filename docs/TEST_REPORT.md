# Sleep Saver V1 测试报告

> 测试日期：2026-08-22
> 测试版本：`1.3.12-dev`
> 设备目标：小米 14 Pro
> 原则：逻辑自动化与真机验证分开报告，不用模拟结果冒充 HyperOS 真机结果。

## 1. 当前结论

- Gradle 构建：通过。
- 本地自动化测试：19 项执行，19 项通过，0 跳过，0 失败，0 错误。
- Debug APK：已生成。
- 静态权限检查：未声明 `INTERNET`；声明 `PACKAGE_USAGE_STATS` 与 `POST_NOTIFICATIONS`；自动备份关闭。
- 小米 14 Pro 真机：`1.3.11-dev` 截图确认过右侧裁切；`1.3.12-dev` 已改为近 7 晚一屏、长区间默认停靠最新。用户确认本轮可同步 GitHub；最新复图未纳入公开仓库。
- UI 结构核验：手帐主流程包括顶部数据管理、近 7 晚 / 近 30 晚 / 自定义、日期范围与完成晚数、按日 / 按周 / 按月、真实纸胶带睡眠时长图、闭眼玩偶偏离图及同页悬浮单晚详情。自定义日期改为单个范围日历；偏离图 7 点以内一屏展示，更多点才横向滚动。
- 数据边界核验：时间轴对 Usage Access 不可用的历史记录显示「不可用」，不会伪装成「0 次」；进行中的记录显示「待打卡 / 待结算」。
- 统计口径核验：「本机睡前使用」只描述本机数据；偏离计划以实际睡前打卡时间减去当晚计划就寝时间计算，正值显示「晚」，负值显示「早」。
- 数据库升级核验：Room 版本从 1 升到 2，采用显式 `MIGRATION_1_2`，没有 destructive migration。SQLite 迁移冒烟测试确认 1 条旧记录保留，四个计划时间快照字段以可空列正常加入；真机覆盖安装仍需复测。

## 2. 产物

- APK：`app/build/outputs/apk/debug/SleepSaver-1.3.12-dev.apk`
- 文件大小：70,506,079 bytes（约 67.2 MiB，debug 测试包未做发布压缩）
- SHA-256：`b71b42f4b0839644b5b99af3cf348dfbd895afe2cb9c99375b6b98ad44b705bf`
- 单元测试 XML：`app/build/test-results/testDebugUnitTest/TEST-com.sleepsaver.app.SleepSaverLogicTest.xml`
- Room v2 架构：`app/schemas/com.sleepsaver.app.data.SleepSaverDatabase/2.json`。
- 启动图标：APK 已识别原创睡眠帽玩偶版 `mdpi`、`hdpi`、`xhdpi`、`xxhdpi`、`xxxhdpi` 五档 `ic_launcher.png`。
- 打卡页插画：`app/src/main/res/drawable-nodpi/checkin_journal_hero.png`，1200×600px；生成母版保存在 `design-assets/check-in-journal-hero-source.png`。
- 今日页玩偶：`app/src/main/res/drawable-nodpi/today_timeline_mascot.png`，420×420px；生成母版保存在 `design-assets/today-timeline-mascot-source.png`。
- 手帐页历史记录纸：`app/src/main/res/drawable-nodpi/journal_history_paper.png`，1200px 宽；生成母版保存在 `design-assets/journal-history-paper-source.png`。
- 手帐页完整视觉基准：`design-assets/journal-v1.3.8-full-screen-reference.png`。
- 手帐纸胶带与玩偶资源：`app/src/main/res/drawable-nodpi/journal_duration_tape_1.png` 至 `journal_duration_tape_7.png`、`journal_tape_gingham.png`、`journal_tape_polka.png`、`journal_sleepy_mascot.png`；可复现处理脚本为 `design-assets/process-journal-assets.cjs`。
- `1.3.7-dev` 至 `1.3.11-dev` 的手帐真机缺陷截图作为本地私有 QA 证据保留，公开仓库不提交其中的个人日期与睡眠时间。
- 偏离计划区域纸胶带：`app/src/main/res/drawable-nodpi/journal_plan_ribbon.png`；透明母版为 `design-assets/journal-plan-ribbon-transparent.png`。
- 偏离计划时间纸便签：`app/src/main/res/drawable-nodpi/journal_deviation_label.png`，512×293px；生成母版为 `design-assets/journal-deviation-label-source.png`，透明成品为 `design-assets/journal-deviation-label-transparent.png`，可复现处理脚本为 `design-assets/process-journal-deviation-label.sh`。
- 今日页真机 QA：`design-assets/qa/today-v1.3.2-xiaomi14pro.jpg`；与设计稿并排对照保存在 `design-assets/qa/today-v1.3.2-reference-comparison.png`。
- 今日页第二轮真机 QA：`design-assets/qa/today-v1.3.4-xiaomi14pro.jpg`；并排对照为 `design-assets/qa/today-v1.3.4-reference-comparison.png`。
- 打卡页真机 QA：`design-assets/qa/checkin-v1.3.3-xiaomi14pro.jpg`；并排对照为 `design-assets/qa/checkin-v1.3.3-reference-comparison.png`。
- 手帐页真机 QA：`design-assets/qa/journal-v1.3.3-xiaomi14pro.jpg`；并排对照为 `design-assets/qa/journal-v1.3.3-reference-comparison.png`。
- 今日页透明玩偶：`design-assets/today-timeline-mascot-transparent.png`，App 内同名资源已替换旧的方形底图版本。

## 3. 19 项覆盖情况

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
| 15 | App 升级 | 通过：禁止 destructive migration；v1→v2 SQL 冒烟迁移保留旧记录 | 待验证 `1.3.11-dev` 覆盖安装到 `1.3.12-dev` |
| 16 | 跨 00:00 | 通过：一个 Session、时长正确 | 待验证 |
| 17 | 修改提醒时间 | 通过：始终使用同一个 unique work 名称 | 待验证通知与时间更新 |
| 18 | 偏离计划一屏与长区间布局 | 通过：264dp 及以上视口的 7 点模式不滚动且至少保留 4dp 标签间隔；8—31 点使用固定 42dp 点距与 8dp 标签间隔 | 待验证小米首次打开时最右项完整 |
| 19 | 自定义日期范围一次确认 | 通过：开始或结束缺失时不可确认；两者选齐后可确认；日期 UTC 毫秒转换无偏移 | 待验证范围着色与单次确认交互 |

## 4. 后续真机复测步骤

1. 在小米 14 Pro 打开「开发者选项 → USB 调试」，用数据线连接电脑，并在手机上点「允许」。
   - 成功标志：`adb devices -l` 显示一台状态为 `device` 的设备。
2. 用 `SleepSaver-1.3.12-dev.apk` 覆盖安装，不要先卸载旧版本。
   - 成功标志：应用可以直接打开，旧的睡眠记录仍存在，底部有今日、打卡、手帐、好友四项。
3. 点击「去授权」，在 HyperOS 使用情况访问页面允许 Sleep Saver。
   - 成功标志：回到应用后紫色权限提示消失。
4. 逐项做上表的真机动作，重点复测通知亮屏、指纹解锁、早晨 Session 排除与打卡后继续刷手机。
   - 成功标志：每项结果与表中预期一致。
5. 开启提醒并允许通知，修改一次就寝时间。
   - 成功标志：只有一条提醒，不会重复通知。
6. 打开「手帐」，选择「自定义」，选择覆盖至少 30 天的开始和结束日期，再选「按周」。
   - 成功标志：只出现一个范围日历；点击开始日期、结束日期后中间范围连续着色；只需点击一次「确认」，顶部即显示自定义日期范围和完成晚数。
7. 分别切换近 7 晚 / 近 30 晚和按日 / 按周 / 按月；点数超过一屏时左右滑动两张图。
   - 成功标志：近 7 晚首次打开即可完整看到最右一天且无需横向拖动；超过 7 点时默认显示最新记录，仍可向右回看较早日期；标签不重叠、不裁切。
8. 保持「自定义 + 按周」，展开「睡眠记录详情」，用左右箭头和日期标签切换至少 3 晚记录，再点击收起。
   - 成功标志：页面内出现一张带阴影的悬浮纸张卡，导航位于纸张上方，一次只显示一晚；收起后详情消失，不跳转到新页面。
9. 新完成一次睡前打卡，随后修改睡眠定时时间，再早起打卡并回到手帐查看偏离。
   - 成功标志：这一晚使用睡前打卡时保存的计划时间，不因之后修改定时而重新计算；数据库升级前的旧记录继续可读。

## 5. 尚未完成的发布事项

- 当前是可安装的开发预览包，不是应用商店正式发布包；用户已确认本轮可以同步 GitHub。
- 正式发布前需要由用户保管的 release keystore；未在项目中硬编码密码或私钥。
- 商店级正式发布仍需完整真机回归，并由用户保管 release keystore 后生成签名 APK。
- 当前电脑未连接安卓设备且没有可用模拟器，因此报告仍把 19 项自动化结果与最新真机视觉证据分开；本次按开发预发布同步，不宣称已完成多设备或像素级验收。

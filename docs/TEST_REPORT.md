# Sleep Saver V1 测试报告

> 测试日期：2026-08-24
> 测试版本：`1.4.1-dev`
> 设备目标：小米 14 Pro
> 原则：逻辑自动化与真机验证分开报告，不用模拟结果冒充 HyperOS 真机结果。

## 1. 当前结论

- Gradle 构建：通过。
- 本地自动化测试：32 项执行，32 项通过，0 跳过，0 失败，0 错误。
- Debug APK：已生成。
- 静态权限检查：未声明 `INTERNET`；声明 `PACKAGE_USAGE_STATS` 与 `POST_NOTIFICATIONS`；自动备份关闭。
- 小米 14 Pro 真机：`1.3.12-dev` 已完成上一轮手帐交互验证；`1.4.1-dev` 的前置纠正入口和单层时间编辑器尚未覆盖安装复测。本报告不把自动化构建结果视作真机验收。
- UI 结构核验：今日页与打卡完成态都能直接纠正最近一晚；异常时长会突出显示。手帐顶部有「纠正历史记录」，先选择晚次再打开共享编辑器；原有详情内修改按钮保留为次级入口。补记和纠正都在一个 Compose 弹窗内完成日期、小时和分钟编辑，不新增独立页面，也不再嵌套系统日期与模拟时钟弹窗。
- 数据边界核验：APP 不会自动猜测或生成漏记时间；用户确认后才保存。纠正会重查本机 Usage Access，可用历史重新统计，不可用历史继续标记不可用，不伪装成 0。
- 统计口径核验：纠正后的实际入睡/起床时间会重新计算休息时长、偏离计划、周目标、趋势图和导出；原始点击时间与纠正时间继续保留，可恢复。
- 数据库升级核验：Room 版本升到 3，继续保留显式 `MIGRATION_1_2`，新增非破坏性 `MIGRATION_2_3`；三项审计时间字段均为可空列。SQLite 冒烟迁移确认 1 条 v2 旧记录保留；真机覆盖安装仍需复测。

## 2. 产物

- APK：`app/build/outputs/apk/debug/SleepSaver-1.4.1-dev.29.apk`
- 文件大小：70,719,071 bytes（约 67.4 MiB，debug 测试包未做发布压缩）
- SHA-256：`6b46b7a0271de706f7de4868a970eaa693552cb3534feca74cf1a70982f1f23e`
- 单元测试 XML：`app/build/test-results/testDebugUnitTest/TEST-com.sleepsaver.app.SleepSaverLogicTest.xml`
- Room v3 架构：`app/schemas/com.sleepsaver.app.data.SleepSaverDatabase/3.json`。
- 启动图标：APK 已识别原创睡眠帽玩偶版 `mdpi`、`hdpi`、`xhdpi`、`xxhdpi`、`xxxhdpi` 五档 `ic_launcher.png`。
- 打卡页插画：`app/src/main/res/drawable-nodpi/checkin_journal_hero.png`，1200×600px；生成母版保存在 `design-assets/check-in-journal-hero-source.png`。
- 今日页玩偶：`app/src/main/res/drawable-nodpi/today_timeline_mascot.png`，420×420px；生成母版保存在 `design-assets/today-timeline-mascot-source.png`。
- 手帐页历史记录纸：`app/src/main/res/drawable-nodpi/journal_history_paper.png`，1200px 宽；生成母版保存在 `design-assets/journal-history-paper-source.png`。
- 手帐页完整视觉基准：`design-assets/journal-v1.3.8-full-screen-reference.png`。
- 纠正记录单层编辑器确认稿：`design-assets/session-time-editor-v1.4.1-reference.png`。
- 手帐纸胶带与玩偶资源：`app/src/main/res/drawable-nodpi/journal_duration_tape_1.png` 至 `journal_duration_tape_7.png`、`journal_tape_gingham.png`、`journal_tape_polka.png`、`journal_sleepy_mascot.png`；可复现处理脚本为 `design-assets/process-journal-assets.cjs`。
- `1.3.7-dev` 至 `1.3.11-dev` 的手帐真机缺陷截图作为本地私有 QA 证据保留，公开仓库不提交其中的个人日期与睡眠时间。
- 偏离计划区域纸胶带：`app/src/main/res/drawable-nodpi/journal_plan_ribbon.png`；透明母版为 `design-assets/journal-plan-ribbon-transparent.png`。
- 偏离计划时间纸便签：`app/src/main/res/drawable-nodpi/journal_deviation_label.png`，512×293px；生成母版为 `design-assets/journal-deviation-label-source.png`，透明成品为 `design-assets/journal-deviation-label-transparent.png`，可复现处理脚本为 `design-assets/process-journal-deviation-label.sh`。
- 今日页真机 QA：`design-assets/qa/today-v1.3.2-xiaomi14pro.jpg`；与设计稿并排对照保存在 `design-assets/qa/today-v1.3.2-reference-comparison.png`。
- 今日页第二轮真机 QA：`design-assets/qa/today-v1.3.4-xiaomi14pro.jpg`；并排对照为 `design-assets/qa/today-v1.3.4-reference-comparison.png`。
- 打卡页真机 QA：`design-assets/qa/checkin-v1.3.3-xiaomi14pro.jpg`；并排对照为 `design-assets/qa/checkin-v1.3.3-reference-comparison.png`。
- 手帐页真机 QA：`design-assets/qa/journal-v1.3.3-xiaomi14pro.jpg`；并排对照为 `design-assets/qa/journal-v1.3.3-reference-comparison.png`。
- 今日页透明玩偶：`design-assets/today-timeline-mascot-transparent.png`，App 内同名资源已替换旧的方形底图版本。

## 3. 32 项覆盖情况

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
| 15 | App 升级 | 通过：禁止 destructive migration；v1→v2 与 v2→v3 迁移链保留旧记录 | 待验证 `1.3.12-dev` 覆盖安装到 `1.4.1-dev` |
| 16 | 跨 00:00 | 通过：一个 Session、时长正确 | 待验证 |
| 17 | 修改提醒时间 | 通过：始终使用同一个 unique work 名称 | 待验证通知与时间更新 |
| 18 | 偏离计划一屏与长区间布局 | 通过：264dp 及以上视口的 7 点模式不滚动且至少保留 4dp 标签间隔；8—31 点使用固定 42dp 点距与 8dp 标签间隔 | 待验证小米首次打开时最右项完整 |
| 19 | 自定义日期范围一次确认 | 通过：开始或结束缺失时不可确认；两者选齐后可确认；日期 UTC 毫秒转换无偏移 | 待验证范围着色与单次确认交互 |
| 20 | 计划起床后仍有活跃记录 | 通过：继续显示早起打卡，不误判漏记 | 待验证 |
| 21 | 第一次漏记且没有历史记录 | 通过：计划起床后显示「昨晚漏记」 | 待验证 |
| 22 | 昨晚已经完成双打卡 | 通过：白天显示「昨晚已记录」 | 待验证 |
| 23 | 漏记跨到下一次计划就寝 | 通过：主卡恢复今晚睡前打卡，漏记降为次级提醒 | 待验证 |
| 24 | 时间顺序、未来时间与记录重叠 | 通过：三类非法时间分别拦截 | 待验证 |
| 25 | 界面时钟跨分钟但用户没有手动修改 | 通过：未点修改时始终保存真实点击时间，不误标为纠正 | 待验证 |
| 26 | 修正过的睡前打卡执行撤销 | 通过：5 分钟窗口以原始点击时间计算 | 待验证 |
| 27 | JSON / CSV 审计字段 | 通过：导出 v2 包含原始时间、纠正时间与计划快照 | 待验证 |
| 28 | 选择历史日期后修改对应晚次 | 通过：按 Session ID 解析选中记录，选择 8/23 不会回落到最新的 8/24 | 待验证点击 8/23 后按钮与纸张同步切换 |
| 29 | 历史日期与修改按钮点击区域 | 通过：日期标签至少 58×44dp，修改按钮高度至少 48dp | 待验证单手点击命中率 |
| 30 | 单层编辑器直接输入 24 小时制时间 | 通过：22:00 可解析，24 时与 60 分被判为无效 | 待验证数字键盘、光标和错误提示 |
| 31 | 快捷按钮跨午夜调整 | 通过：00:05 减 10 分钟得到前一日 23:55 | 待验证日期同步变化与连续点击 |
| 32 | 异常时长的纠正入口 | 通过：正常 7 小时不突出，16 小时 23 分触发显著纠正提示 | 待验证今日页粉色提示与点击路径 |
## 4. 后续真机复测步骤

1. 在小米 14 Pro 打开「开发者选项 → USB 调试」，用数据线连接电脑，并在手机上点「允许」。
   - 成功标志：`adb devices -l` 显示一台状态为 `device` 的设备。
2. 用 `SleepSaver-1.4.1-dev.29.apk` 覆盖安装，不要先卸载旧版本。
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
   - 成功标志：页面内出现一张带阴影的悬浮纸张卡；点击 8/23 后纸张切到 8/23，导航下方显示「修改 8/23 这晚」；收起后详情消失，不跳转到新页面。
9. 新完成一次睡前打卡，随后修改睡眠定时时间，再早起打卡并回到手帐查看偏离。
   - 成功标志：这一晚使用睡前打卡时保存的计划时间，不因之后修改定时而重新计算；数据库升级前的旧记录继续可读。

10. 将睡眠定时设置为 00:00—08:00，在没有活跃记录和昨晚记录的情况下于 08:30 后打开打卡页。
    - 成功标志：主标题显示「昨晚漏记」，只有点击补记并确认实际起止时间后才新增记录。
11. 先完成睡前打卡，超过计划起床时间后再打开；把实际起床时间改为更早的真实时间后完成早起打卡。
    - 成功标志：仍显示早起打卡；保存后今日时长、手帐趋势和偏离计划同步变化，未点击修改的正常打卡不显示「已调整」。
12. 在手帐展开单晚详情，先选择一条非最新记录（例如 8/23），点击「修改 8/23 这晚」，调整时间并保存，再次进入修改选择「恢复原始时间」。
    - 成功标志：只有 8/23 被更新且界面仍停留在 8/23；纸张底部出现「已调整」，恢复后回到首次记录时间；重叠、未来或起床早于入睡时会提示并拒绝保存。
13. 分别从今日页「纠正记录」、打卡完成态「纠正昨晚记录」和手帐顶部「纠正历史记录」进入编辑器；在手帐入口选择 8/23。
    - 成功标志：三个入口都打开同一种单层编辑器；手帐会先显示晚次列表，选择 8/23 后编辑对象仍是 8/23；不会再连续弹出系统日期框和模拟时钟。
14. 在同一编辑器内直接把准备休息改成 22:00、实际起床改成次日 07:00，再用 `−10分` 和 `+1时` 快捷按钮试一次跨午夜调整。
    - 成功标志：小时与分钟可直接输入，日期箭头和快捷按钮会同步更新日期；页面即时显示休息时长，确认保存后今日、手帐和趋势图一起更新，数字键盘不会遮住保存按钮。

## 5. 尚未完成的发布事项

- 当前是可安装的本地开发预览包，不是应用商店正式发布包；本轮尚未推送 GitHub 或创建公开 Release。
- 正式发布前需要由用户保管的 release keystore；未在项目中硬编码密码或私钥。
- 商店级正式发布仍需完整真机回归，并由用户保管 release keystore 后生成签名 APK。
- 当前电脑未连接安卓设备且没有可用模拟器，因此报告仍把 32 项自动化结果与小米 14 Pro 真机证据分开；`1.4.1-dev` 不宣称已完成真机新交互或多设备验收。

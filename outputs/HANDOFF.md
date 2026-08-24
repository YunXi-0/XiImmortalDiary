# 修仙游戏 - 项目交接文档（2026-08-24 最新）

## 项目概述
竖屏修仙游戏，纯文字+按钮，面向 Android WebView。无素材，浅色主题白色背景。

## 仓库
- GitHub: https://github.com/YunXi-0/XiImmortalDiary.git
- 存储在 localStorage，键名 xiImmortalSave，当前 SAVE_VER=4
- 推送时机：用户说“上传”时才 git push

## 文件结构
- `outputs/index.html` — HTML骨架 + CSS样式 (11834字节, 111行)
- `outputs/game.js` — 全部游戏逻辑 (87410字节, 248行压缩)
- `outputs/待补充.md` — 用户明确表示后续补充的机制清单
- `outputs/app/` — Android WebView 项目结构
- `.github/workflows/build.yml` — GitHub Actions APK构建

## 重要技术约束
- **game.js 中所有中文文本用 Unicode 转义**（`\uXXXX`），不能直接写中文
- **文件使用 Windows 行尾**（`\r\n`），脚本中搜索替换需注意
- 用 node 脚本修改文件，每次修改后用 `require('vm').createScript(s)` 验证语法
- `skills.insight` 等运行时变量不能在 Node.js 构建脚本中求值，必须作为字面量写入
- 推荐写 Node.js 脚本文件到 `work/` 目录执行，避免 PowerShell heredoc 转义问题

---

## 界面（6个Tab）
1. **主界面** — 境界、修为进度条、修炼/突破按钮、开发者调试按钮
2. **游历** — 目的地选择、前进/探索/返回、战斗、洞察控制按钮可见性
3. **存储** — 装备栏 + 仓库 + 背包
4. **商店** — 铜钱显示、出售物品、刷新（5铜钱/10分钟自动）
5. **制作** — 炼丹/锻造/秘籍配方、制作等级系统
6. **信息** — 属性/统计/图鉴/技能天赋 四个子tab

---

## 境界系统

### 主线境界（13个，各含4阶段：前期/中期/后期/圆满）
练气(14层) → 筑基 → 金丹 → 元婴 → 化神 → 炼虚 → 合体 → 渡劫 → 真仙 → 太乙玉仙 → 金仙 → 大罗金仙 → 银灵仙
- 最大 stageIdx = 61
- 练气有14层（0-13），之后每个境界4层
- 渡劫圆满→真仙前期按钮改为“飞升”（物品需求待定）
- 境界突破已取消物品需求，仅需修为

### 特殊境界（15个）
特殊境界在特定主境界突破时概率触发，使用独立变量 `specialRealm`(1-15) + `specialStage`(0-3)，不占用 stageIdx。
完成特殊境界后自动进入下一主境界。同一突破点完成特殊境界后不可再次触发（`_srDone` 标记）。
进入特殊境界时保留已有修为。
修为需求 = 下一主境界对应阶段的 60%（`getSpecialReq()`）。

| ID | 境界 | 触发点 | 概率 | 前置条件 | 阶段奖励(前/中/后/圆满) | 完成加成 |
|---|---|---|---|---|---|---|
| 1 | 紫府 | 练气→筑基 | 5% | — | atk +3/6/9/12 | +5% atk |
| 2 | 极境基础 | 练气→筑基 | 10% | — | def +1/2/3/4 | +5% def |
| 3 | 通脉 | 练气→筑基 | 10% | — | hp +10/20/30/50 | +5% hp |
| 4 | 真元境 | 练气→筑基 | 1% | — | hp+5/10/20/30 atk+1/2/3/5 def+1/2/2/3 | +3%/3%/3% |
| 5 | 极境入门 | 筑基→金丹 | 30% | 极境基础已完成 | def +3/6/9/12 | +7% def |
| 6 | 婴变 | 金丹→元婴 | 5% | — | ls +1%/1.5%/2%/2.5% | +7% hp +5% atk |
| 7 | 假婴 | 金丹→元婴 | 10% | — | atkPct +1%/1.5%/2%/2.5% | +10% atk |
| 8 | 极境中级 | 金丹→元婴 | 50% | 极境入门已完成 | def +10/13/16/20 | +10% def |
| 9 | 返虚 | 化神→炼虚 | 5% | — | hp +100/150/200/250 | +20% hp |
| 10 | 洞虚 | 化神→炼虚 | 5% | — | hp +100/150/200/250 | +20% hp |
| 11 | 玉虚 | 化神→炼虚 | 5% | — | def +25/40/60/80 | +20% def |
| 12 | 极境高级 | 化神→炼虚 | 65% | 极境中级已完成 | def +20/30/40/50 | +20% def |
| 13 | 碎涅 | 炼虚→合体 | 2% | — | hp+200~1000 atk+50~250 def+30~100 | +20%/20%/20% |
| 14 | 体空 | 炼虚→合体 | 10% | — | hp-100 atk+60~150 def+10~80 | -5%/+15%/+10% |
| 15 | 极境终极 | 炼虚→合体 | 88% | 极境高级已完成 | def +80/100/150/200 | +40% def |

### 特殊境界关键变量
- `specialPctBonus` — `{hp:0, atk:0, def:0}` 对象，完成特殊境界后累加百分比
- `completedSpecials` — 数组，记录已完成的特殊境界ID
- `applySpecialEntryReward()` — 进入特殊境界时给予第一阶段奖励
- 阶段突破奖励在 `$btnBreak` click handler 中通过 `sf` 数组实现

---

## 属性系统

### 有效属性计算
- 有效HP = `(baseHP + bonusHP) * (1 + (getPctBonus() + specialPctBonus.hp) / 100) + containerHPBonus`
- 有效ATK = `(baseATK + bonusATK) * (1 + (getPctBonus() + specialPctBonus.atk) / 100)`
- 有效DEF = `(baseDEF + bonusDEF) * (1 + (getPctBonus() + specialPctBonus.def) / 100)`

### 属性详情弹窗（showAttrDetail）
点击属性行弹出详情，按来源分类展开/收动：
- **境界** — 基础属性值
- **特殊境界** — bonusHP/bonusATK/bonusDEF/bonusLifesteal 基础数值加成（仅非0时显示）
- **丹药** — 灵草丹/蚊子丸使用次数对应加成
- **藏品** — 容器类藏品加成（仅在弹窗内显示，不在属性界面主显示区显示）
- **百分比加成** — 分别显示“主境界加成 +X%”和“特殊境界加成 +Y%”（仅非0时显示特殊境界项）

### 属性界面主显示区（renderAttrTab）
血量/攻击/防御括号内显示总百分比（主境界+特殊境界），不显示藏品加成。

---

## 洞察系统

### 洞察等级效果
| 等级 | 效果 |
|---|---|
| Lv.0 | 游历界面事件/掉落/稀有/属性按钮全部隐藏，仅显示“地图等级” |
| Lv.1 | 显示“事件”按钮；点击“地图等级”悬浮提示，3秒 |
| Lv.2 | 显示“掉落”按钮（不显示Boss级怪物） |
| Lv.5 | 显示“稀有”按钮 |
| Lv.7 | 显示“属性”按钮 |

### 洞察技能介绍（点击“洞察”字样弹窗）
Lv.0 = “用于看到更多...”，每升一级追加一次“更多...”字符串。

### 获取途径
攻击属性弹窗首次(+1)、小雄蚊图鉴首次(+1)、一品灵草丹配方弹窗首次(+1)、探索1%随机

---

## 游历系统
地图经验等级：每击杀+1经验，每级+10完成后可继续探索/前进次数（`getPostDoneMoves`）。
100%进度后显示“剩余可探索次数：X”，次数为0时不显示“探索”按钮。
银灵/金灵出场时1秒隐藏攻击/逃跑按钮。

## 战斗系统
吸血属性：造成伤害×吸血比例恢复血量。变体怪物独立击杀统计。

## 商店系统
3个格子（可升级到5：3→4=200铜钱，4→5=500铜钱）。商品按最近游历地点刷新。
银灵碎/金灵碎/闪灵碎 sellPrice=null，不可出售，不可丢弃。

## 制作系统
炼丹：一品灵草丹(maxCraft:30)、一品蚊子丸(maxCraft:10)。蚊子丸使用按钮为“服用”（丹药类）。
锻造：灵草结（不限次数）。秘籍：洞察（5×2^洞察等级个洞察碎片）。

## 开发者调试
主界面底部按钮，弹窗内可修改：境界(stageIdx)、洞察等级(skills.insight)、修为(exp)、铜钱(copper)、藏品(containerOwned等)。

---

## 待补充清单（用户明确表示）
1. 飞升物品需求待定
2. 特殊境界物品需求待定（修为已实现，物品需求已取消）
3. Boss级怪物定义待补充
4. 极境系列后续境界待补充
5. 稀有物品概率等级升级机制待补充
6. 锻造/秘籍等级加成效果待补充
7. 灵碎物品用途待补充
8. 洞察碎片后续获取途径待补充

## 构建与部署
- GitHub Actions workflow在 `.github/workflows/build.yml`
- APK构建使用 Gradle 7.5.1 + AGP 7.4.2 + JDK 17
- 用户要求：APK不需要每次修改都生成，等待明确指令再执行

## 存档键名映射
v→SAVE_VER  sI→stageIdx  exp→exp  cop→copper
pHP→playerHP  pMHP→playerMaxHP  bp→backpack  wh→warehouse
mk→monsterKills  cO→containerOwned  cFO→containerFemaleOwned
cKB→containerKillBase  cFKB→containerFemaleKillBase
cs→craftStats  bHP→bonusHP  bATK→bonusATK  bDEF→bonusDEF
bLS→bonusLifesteal  eq→equipment  sh→shopSlots  sr→shopRefreshAt
lt→lastTravelDest  cl→craftLevels  sL→{gc,rl}→shopLevel
sk→skills  me→mapExp  en→enchanted  vk→variantKills
ifd→insightFirstDone  sr→specialRealm  ss→specialStage
spb→specialPctBonus(对象{hp,atk,def})  cs2→completedSpecials

## 核心数据
STAGES = [练气,筑基,金丹,元婴,化神,炼虚,合体,渡劫,真仙,太乙玉仙,金仙,大罗金仙,银灵仙]
BREAKTHROUGH_BONUS = [1,2,4,8,16,32,64,128,256,512,1024,2048,4096]
REALM_REQ_BASE = [2000,4860,9720,19440,38880,77760,155520,311040,622080,1244160,2488320,4976640]
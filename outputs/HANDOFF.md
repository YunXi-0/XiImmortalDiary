# 修仙游戏 - 项目交接文档

## 项目概述
竖屏修仙游戏，纯文字+按钮实现，面向手机 Android WebView。无素材，浅色主题白色背景。

## 仓库
- GitHub: https://github.com/YunXi-0/XiImmortalDiary.git
- 存储在 localStorage，键名 xiImmortalSave，当前 SAVE_VER=4
- 推送时机：用户说"上传"时才 git push

## 文件结构
- outputs/index.html — HTML骨架 + CSS样式
- outputs/game.js — 全部游戏逻辑
HTML 通过 script src=game.js 引入 JS。

## 重要：编码问题
game.js 中所有中文文本必须用 Unicode 转义写入，不能直接写中文字符。
写入方式：用 node 脚本写文件（通过 exec_command），或用 apply_patch 工具。
绝对不要用 PowerShell 的字符串替换操作中文内容，会因转义问题失败。

## 重要：大文件修改策略
不要用 PowerShell 字符串替换修改 game.js。推荐方式：
1. 用 apply_patch 重写整个文件
2. 用 node 脚本写入临时 .js 文件，exec_command 运行
3. 小改动可用 node -e 一行命令

## 界面结构（6个Tab）
1. 主界面 — 境界、修为进度条、修炼/突破按钮
2. 游历 — 目的地选择、游历（前进/探索/返回）、战斗
3. 存储 — 装备栏 + 仓库 + 背包
4. 商店 — 铜钱显示、出售物品
5. 制作 — 炼丹/饰品配方
6. 信息 — 属性/统计/图鉴 三个子tab

## 状态管理
playerState = 'idle' | 'cultivating' | 'traveling'（互斥）

## 修炼
- 每10秒+10修为（通过 setExp 应用饰品加成后写入）
- 修炼中每秒恢复1%最大血量
- 突破不中断修炼

## 境界
- 练气一阶~九阶，需求 100/200/.../800
- 每升一阶：+1%血攻防百分比加成

## 属性计算
- 基础：血量50、攻击5、防御0、自然恢复0
- 百分比加成 = stageIdx (0~8)
- 血量显示1位小数，攻击防御显示2位小数

## 物品系统
faintSpirit 微弱灵气残影 品质1 迷你 出售10 可用(吸收)
mosquitoContainer 小雄蚊灵体容器 品质3 中 特殊
mosquitoFemaleContainer 小雌蚊灵体容器 品质3 中 特殊
herb 灵草 品质1 迷你 出售2
herbPill 一品灵草丹 品质1 迷你 出售null 可用(服用) 描述="由灵草制作的丹药，入口微甜"
grassKnot 灵草结 品质1 迷你 出售5 可装备(饰品)

## 特殊战利品
- 不占仓库格子，获取后直接点亮图鉴，效果即刻生效
- 进度满后图鉴黑色镶边，未满金色镶边
- 弹窗仅显示信息，无操作按钮

## 装备系统
- 6个装备栏，解锁数 = min(1+stageIdx, 6)
- 同时只允许装备1件饰品
- equippable:true 的物品可装备

## 制作系统
一品灵草丹 | 炼丹 | 灵草x5 | 100% | 10次 | +1血量+0.1攻+0.1防
灵草结 | 饰品 | 灵草x20 | 100% | 不限 | 修为获取+10%
- 制作界面弹窗仅显示描述和效果（用 showCraftPopup，不用 showModal）

## 游历事件概率表
前进：20%怪物 | 80%修为 | 额外20%铜钱
探索(普通)：20%怪物 | 20%素材(灵草) | 60%修为(1-10) | 额外20%铜钱
探索(10km后)：20%怪物 | 20%素材 | 55%修为(5-10) | 5%灵气残影 | 额外20%铜钱
怪物判定优先级最高，触发时铜钱等不触发。

## 怪物
小雄蚊 20血 5攻 0防 5修为 | 嗡~嗡~~
小雌蚊 15血 3攻 0防 4修为 | 吸血(自愈)
- 击杀1只显示血量，5只显示攻击，10只显示防御
- 玩家先手，秒杀不反击
- 玩家血量归零：HP设1，仅显示返回，返回后自动修炼

## 战斗文本重复
相同伤害时显示 x2/x3，放在两行伤害右侧中间

## 存档键名映射
sI->stageIdx cop->copper pHP->playerHP pMHP->playerMaxHP
bp->backpack wh->warehouse mk->monsterKills
cO->containerOwned cFO->containerFemaleOwned
cs->craftStats bHP->bonusHP bATK->bonusATK bDEF->bonusDEF
eq->equipment(新字段,旧存档默认6个null)

## 已知未解决的问题

### 装备栏锁定格子锁标志不显示
- 描述：未解锁的装备格子应该显示锁标志，但实际不显示
- 代码位置：game.js 的 renderStorage() 函数
- 已尝试：Unicode转义、实际emoji字符，文件中确认存在但浏览器不显示
- 可能原因：emoji在WebView中渲染问题或font-size太小
- 建议：改用文字"锁"或放大字体，或用CSS伪元素

## Github Token
- 用户会在新对话中提供 token
- 仅用于此仓库
- 用户说"上传"时才 git push，不要自动推送
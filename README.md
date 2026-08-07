# 🎣 Astrail Fisher

An AFK-friendly **auto-fishing** macro extracted from the *Astrail client*,
tuned for **SkyBlock-style** servers (Minecraft **26.2**, **Fabric**, client-side only).

> 从 *Astrail 客户端* 中提取的 **自动钓鱼** 宏，专为空岛类（SkyBlock）服务器优化。
> 纯客户端功能，不影响服务器数据；Minecraft **26.2** · **Fabric**。

![Minecraft](https://img.shields.io/badge/Minecraft-26.2-44677a) ![Fabric](https://img.shields.io/badge/Fabric%20API-0.154.2%2B26.2-dbd0b4) ![Java](https://img.shields.io/badge/Java-25%2B-f89820) ![License](https://img.shields.io/badge/License-MIT-green)

---

## Table of Contents 目录

1. [Overview · 简介](#overview--简介)
2. [Features · 功能](#features--功能)
3. [Requirements · 运行要求](#requirements--运行要求)
4. [Installation · 安装](#installation--安装)
5. [Usage · 使用方法](#usage--使用方法)
6. [Configuration · 配置详解](#configuration--配置详解)
7. [How It Works · 工作原理](#how-it-works--工作原理)
8. [SkyBlock Tips · 空岛钓鱼建议](#skyblock-tips--空岛钓鱼建议)
9. [FAQ · 常见问题](#faq--常见问题)
10. [Building From Source · 源码构建](#building-from-source--源码构建)
11. [Files · 文件说明](#files--文件说明)
12. [License · 许可](#license--许可)

---

## Overview · 简介

Astrail Fisher keeps your fishing rod working **while you are AFK**: it casts when your
bobber is missing, reels the moment the server shows a bite marker (**「!!!」**, the marker
most SkyBlock servers already render next to the bobber), then recasts. On top of that it
adds **humanization** touches — short random footwork after a catch, subtle camera drift
and sneaking — so an idle session does not look like a robot clicking continuously.

本 Mod 让你**挂机钓鱼**：钩子不见了就自动抛竿，浮标旁边出现服务器显示的咬钩标记
（「!!!」）就自动收竿，随后自动瞄准水面再抛。
除了自动循环，它还带几项**拟人化**细节——收竿后随机小碎步、轻微镜头摆动、潜行，
让挂机看起来不像一台机器在疯狂连点。

## Features · 功能

- **Auto cast / reel / recast loop** — fully hands-free once a rod is in your main hand.
- **Bite detection via the 「!!!」 marker** — no pixel-peeping, no server-side hooks.
- **Humanization** — Random Movement, Sneak While Moving, Always Sneak, Subtle Rotation.
- **Auto Reset** — recovers hooks stuck on land or ones the server has dropped.
- **Auto Attack (optional)** — fights back when a SkyBlock sea creature is caught.

- **自动抛竿 / 收竿 / 再抛** —— 主手拿上钓鱼竿之后完全不用管。
- **按「!!!」咬钩标记判定收竿时机** —— 无需像素识别，不吃服务器粒子。
- **拟人化** —— 随机移动、移动潜行、全程潜行、轻微镜头转动。
- **自动重置** —— 钩子卡岸 / 卡石头或被服务器吞掉时自动收回重抛。
- **自动攻击（可选）** —— 钓出空岛海怪时自动切武器按技能清怪。

## Requirements · 运行要求

- Minecraft **26.2**（客户端）。
- Fabric Loader **0.19.3+**；Fabric API **0.154.2+26.2**（已在模组清单中声明）。
- Java **25+**，与 Fabric 客户端环境一致。

**Important:** the macro relies on the server-side bite marker **「!!!」** to know when to
reel. If your server does not render that marker, the macro cannot detect bites (see
[FAQ](#faq--常见问题)).

> **注意**：收竿时机依赖服务器在浮标旁显示的「!!!」咬钩标记。如果服务器不显示或
> 魔改了标记，本模组无法识别咬钩（详见 [FAQ](#faq--常见问题)）。

## Installation · 安装

- 从 **Releases** 下载最新的 `astrail-fisher-*.jar`，或按下方步骤自行编译。
- 把 `astrail-fisher-*.jar` 放进 `.minecraft/mods/`（Fabric 环境）。
- 启动游戏，进入任意存档 / 服务器即可。

## Usage · 使用方法

1. 把钓鱼竿放到**主手**（快捷栏当前选中的格子）。
2. 按默认按键【右 Shift】打开**自动钓鱼界面**（本 Mod 只有这一个配置页）。
3. 按需调整开关（默认配置即可直接使用）。
4. 面向水面站好钓鱼点，按 `ESC` 关闭界面 —— 自动钓鱼立即开始。

1. Put the fishing rod in your **main hand**.
2. Press **Right Shift** (default) to open the **Auto Fishing GUI**.
3. Toggle what you need (defaults are fine for most servers).
4. Stand at your fishing spot facing water, press `ESC` — fishing starts right away.

### Controls · 按键

| Action | Default | How to change |
| --- | --- | --- |
| Open Auto Fishing GUI | Right Shift | Options → Controls → *Astrail Fisher* category |
| 打开自动钓鱼界面 | 右 Shift | 选项 → 控制 → *Astrail Fisher* 分类 |

All options live on that one page — nothing hidden in other menus.

> 所有配置都在这一页，没有隐藏的子菜单。

---

## Configuration · 配置详解

Every option is a **client-side** preference, saved automatically to
`config/astrail/config.json`(inside the game directory). Defaults are tuned for AFK
fishing on a normal SkyBlock island.

> 所有配置均为**客户端本地配置**，修改后自动保存到 `config/astrail/config.json`。
> 默认值针对普通空岛挂机钓鱼优化，开箱即可用。

| # | Setting | Default | Range | 一句话说明 |
| --- | --- | --- | --- | --- |
| 1 | Random Movement 随机移动 | ON | – | 收竿后向安全方向随机走一小步再退回，模拟真人 |
| 2 | Sneak While Moving 移动时潜行 | ON | – | 移动碎步期间自动潜行，动作更轻 |
| 3 | Always Sneak 全程潜行 | ON | – | 全程潜行，减少被打偏/被动位移 |
| 4 | Auto Reset 自动重置 | ON | – | 钩子卡住/消失约 20 秒后自动收竿重抛 |
| 5 | Throw Delay 抛竿延迟 | 10 tick | 1~30 tick | 收竿后多久抛下一竿（1 tick = 1/20 秒） |
| 6 | Subtle Rotation 轻微转动 | ON | – | 收竿后镜头轻微摆动再回正 |
| 7 | Auto Attack 自动攻击 | OFF | – | 钓出怪时自动切武器按技能（需配合 8） |
| 8 | Weapon Slot 武器槽位 | 1 | 1–9 | 武器所在的快捷栏格子序号 |

### 1) Random Movement 随机移动

**What it does**: after every successful catch, the player takes one short random step
sideways or backward, sometimes followed by a step back — like a real player adjusting
their footing between casts.

**Safety**: before pressing a key the macro probes the ground ahead (footprint and body
clearance). If no direction is safe — e.g. a one-block ledge over water — the player
plainly **stays still**. It never steps into air or water, and it never auto-jumps.

**中文**：每次成功收竿后，向左右/后方随机挪一小步，偶尔退回原位，像真人调整站姿。
移动前会探测脚下实地；找不到安全方向就**原地不动**——绝不踩空、绝不自动跳跃。

> On platforms wider than ~3 blocks this reads as a natural shuffle; on a 1×1 ledge the
> function keeps still, which is exactly the safe choice.

> 小提示：3 格以上的平台会产生自然的碎步；1×1 岩沿上保持不动，这正是最安全的方案。

### 2) Sneak While Moving（移动时潜行）

During a movement step the macro briefly holds sneak: quieter footfalls, no sprint
flicker. Recommended to stay ON.

移动碎步期间自动潜行：脚步更轻，动作更真实。建议保持 ON。

### 3) Always Sneak（全程潜行）

The client holds sneak for the entire fishing session — a little extra protection against
knockback, being pushed by other players, or a platform that shifts under you. Switch it
off only if your server penalizes long-term sneaking.

全程潜行直到关闭自动钓鱼——减少小击退、被人推动、平台位移的影响。除非服务器对
潜行有惩罚，否则保持 ON。

### 4) Auto Reset（自动重置）

If a hook stays in the world for about **20 seconds** without producing a bite marker,
the macro reels it and casts again. It handles hooks that landed on the bank or vanished
bobbers, so you do not need to police the pond. Keep it ON.

钩子存在约 **20 秒** 仍无咬钩标记，就自动收回并重抛，解决「钩子挂岸」和「服务器吞
浮标」的情况。建议保持 ON。

### 5) Throw Delay（抛竿延迟）

Ticks between the reel and the next cast (1 tick = 1/20 s). Larger values give a laggy
connection more time to register your throw (no throw-and-instant-reel loops); smaller
values give a faster rhythm.

单位 tick（1/20 秒）。数值越大，高延迟下越稳；数值越小，抛竿节奏越快。
默认 10；出现「抛出立刻收回」时调到 15~20。

### 6) Subtle Rotation（轻微转动）

After each catch the camera drifts gently (1–2°) and returns, mimicking the mouse wobble
of a human player. Cosmetic only — toggle it whenever you like.

收竿后镜头轻微晃动再回正（约 1~2°）。纯视觉优化，不影响钓鱼结果。

### 7) Auto Attack（自动攻击）+ 8) Weapon Slot（武器槽位）

For SkyBlock islands where catches spawn **sea creatures**: when a newly-caught monster
appears, the macro swaps the hotbar to the configured slot, uses the weapon ability, then
goes back to fishing. `Weapon Slot` is the hotbar index (1–9, leftmost = 1).

针对钓出海洋生物的空岛服：钓起怪物后自动切到指定武器格并使用技能，清完怪再继续
钓鱼。`武器槽位` 填武器所在的快捷栏序号（1–9，最左为 1）。

> Keep Auto Attack **OFF** on servers without sea-monster mechanics — otherwise it will
> keep swapping your hotbar for nothing.

> 没有海怪机制的服务器请保持 OFF，避免频繁切换装备。

---

## How It Works · 工作原理

（背景：游戏 tick，1 tick = 1/20 秒）

1. **空闲自动抛竿**：主手为钓鱼竿、水中无钩子、未排队重抛时，约 **14 tick（0.7s）**
   自动抛竿；两次空闲抛之间至少间隔 40 tick，避免高 ping 下把刚丢出的钩又收回。
2. **咬钩判定**：抛竿后先等 **6 tick** 的「浮标预热」窗口（忽略上一次咬钩的残留
   标记），之后浮标周围 **3 格** 内出现名为「!!!」的实体标记即自动收竿（同一标记
   只收一次）。
3. **重抛流程**：等拟人动作结束 → 恢复收竿时的镜头 → 确认前方水体无遮挡 → 抛下一竿；
   若水面异常无法确认，最多等 **60 tick** 后强行抛出，避免水池干涸卡死循环。
4. **自动重置**：钩子 400 tick（20 秒）无咬钩 → 自动收竿并重抛（第 4 项设置）。

**In English:**

1. **Auto cast** — 14 ticks (0.7 s) after noticing there is no hook; 40-tick minimum gap
   between idle casts; a 6-tick warm-up window ignores stale markers from previous catches.
2. **Bite detection** — the first 「!!!」-named entity within 3 blocks of the bobber
   triggers a reel; each marker is reeled only once.
3. **Recast wait** — finishes the humanized motion, restores the aim that caught the fish,
   confirms a clear water line, then casts again. The hold is capped at 60 ticks so a
   drained pond cannot stall the loop forever.
4. **Auto Reset** — a hook stuck for 20 seconds is pulled and recast (setting #4).

---

## SkyBlock Tips · 空岛钓鱼建议

- **站位**：站在正对水面的实心平台上；平台别太窄，给「随机移动」留一点可选方向。
- **附魔**：*鱼饵 Lure* 让鱼更快咬竿（每级近似 -5 秒等待）；*海之眷顾 Luck of the Sea*
  提升宝藏/高品质鱼获；具体数值以服务器 Wiki 为准。
- **海怪**：服务器钓出来是实体怪物时，搭配 `Auto Attack` + `Weapon Slot` 使用。
- **机制差异**：各空岛服的「!!!」标记、掉落、怪物技能都不同，以**对应服务器的
  SkyBlock Wiki** 为准。通用参考：[Minecraft Wiki · Fishing](https://minecraft.wiki/w/Fishing)。

---

## FAQ · 常见问题

**Q1: 打开了但完全没反应？**
A：① 主手必须是钓鱼竿；② 面前要有看得见的水；③ 界面开关为 ON。

**Q1 (EN): enabled but nothing happens?**
A: rod in main hand, facing open water, module toggled ON.

**Q2: 从来不自动收竿？**
A：判竿依赖服务器「!!!」标记；服务器不显示就识别不了，这是机制限制。

**Q3: 会不会自动跳/走进水？**
A：不会。随机移动有地面探测，找不到安全方向就原地不动；自动跳跃已移除。

**Q4: 配置保存在哪？**
A：`config/astrail/config.json`（游戏目录，界面内改动自动保存）。不推荐手改。

**Q5: 抛出又立刻被收回？**
A：把 Throw Delay 调大（如 15~20），或等网络稳定后再挂机。

---

## Building From Source · 源码构建

```bash
# Windows
gradlew.bat build --console=plain -q
# macOS / Linux
./gradlew build --console=plain -q
```

产物：`build/libs/astrail-fisher-*.jar`，放进 `mods/` 即可。

## Files · 文件说明

- `src/main/java/.../feature/macro/fishing/AutoFishModule.java` — 自动钓鱼核心逻辑。
- `src/main/java/.../platform/minecraft/AstrailKeybinds.java` — GUI 打开按键（默认右 Shift，可在游戏内按键设置修改）。
- `src/main/java/.../mixin/KeyboardInputMixin.java` — 把模拟按键合入原版输入，避免卡键。
- `使用说明.txt` — 面向普通玩家的中文快速手册（和 README 互补）。

---

## License · 许可

The code is licensed under the **MIT License**, see [LICENSE](LICENSE). Bundled fonts
keep their own licenses (see `assets/astrail/font/inter/ofl.txt`).

> 说明：Astrail Fisher 是 Astrail 客户端中「自动钓鱼」模块的独立实现子集；「!!!」咬钩
> 标记属于服务器侧机制，本 Mod 只负责观察标记、收竿、抛竿，不修改任何服务器数据。
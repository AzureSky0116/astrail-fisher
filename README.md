<h1 align="center">🎣 Astrail Fisher</h1>

<p align="center">
  <a href="#简体中文"><b>简体中文</b></a> | <a href="#english">English</a>
</p>

---

## 简体中文

> 一款专为 **Hypixel 空岛（SkyBlock）** 打造的 **自动钓鱼** 模组。
> 拿上钓竿，剩下的交给它——挂机也能把鱼箱装满。

Astrail Fisher 要做的事很简单：**你拿着钓鱼竿，它接管其余所有步骤**。
钩子消失就自动抛竿，浮标一亮起「!!!」就自动收竿，随后自动把镜头校正回水面、干净利落地再抛出去。
全程循环，不需要你按下任何键。

它也不是一台只会「右键-右键-右键」的连点机器：
内置了几组**拟人化细节**——收竿后随机小碎步、镜头轻微摆动、移动时蹲下放缓脚步——让挂机行为看起来更像真人。

- **纯客户端**：不读游戏内存、不伪装发包、不修改服务器数据
- **开箱即用**：默认配置就是为 Hypixel 空岛钓鱼优化的
- **一个页面管所有**：全部设置集中在单一配置页，没有层层叠叠的菜单

### ✨ 功能一览

- **全自动循环** —— 无钩自抛 → 「!!!」咬钩自收 → 瞄水再抛，直到你关闭
- **按「!!!」判定收竿时机** —— 直接利用 Hypixel 浮标自带的咬钩提示，时机准确
- **拟人化** —— 随机移动、步间潜行、全程潜行、镜头微晃
- **自动重置** —— 钩子卡岸、卡石或被服务器吞掉，约 20 秒后自动收竿重抛
- **自动攻击（可选）** —— 钓出海兽（Sea Creatures）时自动切至武器格并使用技能；
  可关闭**攻击瞄准**（配合 Hyperion 这类右键会传送的武器不会被传离钓点），
  或开启**单次释放**（每只海怪只按一次右键，防止技能把蓝耗空）
- **精准的海怪识别** —— 支持 Banshee、Frog Man 等人形海怪；只攻击自己浮标落点
  新出现的海怪（不会误打别人的），海怪被抢杀/击退/超时后自动停手

### 🛠 运行要求

| 依赖 | 版本 |
| --- | --- |
| Minecraft | 26.2（客户端） |
| Fabric Loader | ≥ 0.19.3 |
| Fabric API | ≥ 0.154.2+26.2 |
| Java | ≥ 25 |

### 📦 安装

1. 从本仓库 **Releases** 下载 `astrail-fisher-0.1.0.jar`；
2. 把 jar 放进 `.minecraft/mods/` 文件夹（需要 Fabric 环境）；
3. 启动游戏，进入 Hypixel 或任意存档即可。

### 🚀 快速上手

1. **主手拿钓鱼竿**（快捷栏当前高亮的格子）；
2. 按 **右 Shift**（默认）打开「自动钓鱼」配置页；
3. 站到钓鱼点，**面向水面**，按 `ESC` 关闭界面——自动钓鱼立刻开始；
4. 想停随时再按一次快捷键即可。

> 开关键可在「**选项 → 控制 → *Astrail Fisher***」中改成任意按键。

### ⚙️ 配置详解

所有配置均为**客户端本地设置**，改动后自动写入 `config/astrail/config.json`，
无需手动编辑。下表是速查，下面是每项的详细解读。

| # | 设置 | 默认 | 范围 | 一句话说明 |
| --- | --- | --- | --- | --- |
| 1 | 随机移动 Random Movement | 开 | – | 收竿后朝安全方向随机小走一步，模拟真人 |
| 2 | 全程潜行 Always Sneak | 开 | – | 整个钓鱼过程持续潜行，防轻微位移 |
| 3 | 自动重置 Auto Reset | 开 | – | 钩子卡住/消失后自动收竿重抛（配合 4） |
| 4 | 重置超时 Reset Timeout | 20 秒 | 5~120 秒 | 多久没咬钩就收竿重抛 |
| 5 | 重试上限 Retry Limit | 5 | 1~50 | 连续卡钩重试多少次后自动关闭自动钓鱼 |
| 6 | 抛竿延迟 Throw Delay | 10 tick | 1~30 tick | 收竿后隔多久抛下一竿（1 tick = 1/20 秒） |
| 7 | 视角锁定 View Lock | 开 | – | 抛竿前把镜头锁定在鱼竿落点；关闭后视角完全自由 |
| 8 | 轻微转动 Subtle Rotation | 开 | – | 收竿后镜头轻微摆动再回正 |
| 9 | 自动攻击 Auto Attack | 关 | – | 钓出海兽时自动切武器用技能（需配合 10~13） |
| 10 | 攻击频率 Attack CPS | 5 | 1~10 | 打海兽时每秒右键次数 |
| 11 | 攻击瞄准 Aim Before Attack | 开 | – | 右键前自动对准海怪；关闭后不移动视角（适合 Hyperion 等传送武器） |
| 12 | 单次释放 Single Use | 关 | – | 每只海怪只按一次右键就停手，防止技能空按把蓝耗空 |
| 13 | 武器槽位 Weapon Slot | 1 | 1~9 | 武器所在快捷栏格子序号 |

**1）随机移动 Random Movement（默认：开）**

收竿成功后，角色会朝「安全方向」随机走一小步——左右微挪或后退半步，偶尔再退回原位，
看起来就像真人在换重心。它绝不是乱走：每次移动前都会先探测脚下及四周是不是实心地面，
如果一个方向都出不去（比如站在 1×1 水沿），就干脆原地不动。**不踩空、不自动跳**，
放心挂着。小步移动期间会自动短暂潜行（该行为固定开启，无需配置）。

**2）全程潜行 Always Sneak（默认：开）**
整个钓鱼过程持续按下潜行，防止被海之击退、被其他玩家无意推动或在平台边缘产生漂移。若所在
服务器对长时间潜行有专门的检测或惩罚，可以关掉。

**3）自动重置 Auto Reset（默认：开）**
钩子已经存在超过「重置超时」设定的秒数却没有任何「咬钩标记」——多半是钩子挂在了岸边的石头上，
或是服务器吞了浮标——此时自动收竿并重新抛投，不用你手动干预。建议保持开启。

**4）重置超时 Reset Timeout（默认：20 秒，可调 5~120 秒）**
多久没有咬钩就判定「卡钩」并收竿重抛。默认 20 秒适合大多数场景；钓点咬钩慢（如深夜时段）可以
调大，想更快回收空钩可以调小。仅在第 3 项「自动重置」开启时生效。

**5）重试上限 Retry Limit（默认：5 次，可调 1~50）**
连续卡钩（由第 3 项「自动重置」触发）达到这个次数、且中间没有成功钓到鱼时，模组会**自动关闭
自动钓鱼**并在聊天里提示你，防止一直空抛耗时间。中间钓到任何一条鱼都会重新计数。

**6）抛竿延迟 Throw Delay（默认：10 tick，可调 1~30）**
收竿与下一竿之间的等待时间，单位 tick（1 tick = 1/20 秒）。网络波动较大的环境下，稍大的值
（如 15~20）可以避免系统把刚抛出的浮标又收回去；网速好、想要更快节奏可以调小。默认 10 已能
满足大多数场景。

**7）视角锁定 View Lock（默认：开）**
每次重抛前，模组会把镜头平滑拉回鱼竿落点，确认能抛到水再出手。
**关闭后模组完全不移动你的视角**：收竿动作一结束就直接按你当前镜头方向抛竿，
可以自由转动镜头看别处。注意：镜头没对着水时抛竿可能落空，落空/卡钩由
「自动重置」兜底。

**8）轻微转动 Subtle Rotation（默认：开）**
每次收竿后，镜头会在水平/垂直方向做一个 1~2° 的小幅晃动再回正，模仿真人握鼠标时手部抖动的
自然感。纯外观向抖动，不影响收竿判定，随时可关。

**9）自动攻击 Auto Attack（默认：关）+ 10）攻击频率 Attack CPS（默认：5）+ 11）攻击瞄准 Aim Before Attack（默认：开）+ 12）单次释放 Single Use（默认：关）+ 13）武器槽位 Weapon Slot（默认：1）**
Hypixel 空岛的钓鱼会「钓出」海兽（Sea Creature）。开启后，每次上钩新生物时模组会自动把快捷栏
切到**武器槽位**指定的格子、使用武器技能，再在清完怪后切回钓竿继续钓鱼。
仅在你需要处理海兽保护时开启；普通水面钓鱼请保持关闭，避免频繁切装。

**10）攻击频率 Attack CPS（默认：5，可调 1~10）**：打海兽时每秒点几次右键。默认 5 下/秒
适合大多数武器；使用 Soul Whip 这类带隐藏冷却（0.5 秒）的武器时可适当调低，避免空按。

**11）攻击瞄准 Aim Before Attack（默认：开）**：开启时，模组会在右键前自动把镜头对准海兽，确保技能命中。
**关闭后，模组完全不移动你的视角**——只负责切武器、按时右键，瞄准交给你自己。
如果你用的是 Hyperion 这类「右键会传送」的武器，建议关闭：自动瞄准海兽后右键会把角色直接传送到海兽身边、远离钓鱼点，
关闭后你可以在安全方向手动右键（爆炸伤害同样能清掉海兽），钓点纹丝不动。

**12）单次释放 Single Use（默认：关）**：开启后，每钓到一只海兽**只按一次右键**便立即停手、
切回钓竿继续钓鱼，不再连续右键。适合技能伤害足够一击清怪、或不想让模组反复释放技能
把蓝量耗空的场景。关闭时保持默认行为：持续右键直到海兽被清掉或离开攻击范围
（若海兽被击退/传送出范围，模组会自动停手，不会对着空气空按）。
---

### 🔄 工作原理

（时间单位：游戏 tick，1 tick = 1/20 秒）

1. **空闲抛竿**：主手是钓竿、水里没有自己的钩子、也未排队重抛时，约 **14 tick（0.7 秒）**
   自动抛竿；两次空闲抛竿之间至少间隔 40 tick，防止刚抛出的新钩被自己收掉。
2. **咬钩判定**：抛竿后先等待 **6 tick** 的浮标「预热期」（过滤上次收竿残留的标记），
   之后浮标附近 **3 格** 内出现「!!!」标记即立刻收竿（同一标记只结算一次）。
3. **重抛流程**：先等拟人动作结束；「视角锁定」开启时会拉回收竿镜头并确认水面无遮挡
   （最多等 **60 tick** 后无条件抛出，防止水池干涸卡死）；关闭时镜头完全不受影响，
   动作一结束就按当前视角直接抛竿。
4. **自动重置**：钩子超过「重置超时」（默认 20 秒 = 400 tick）没有咬钩信号，自动收竿并重新
   抛投（受第 3、4 项设置开关控制）。
5. **海兽检测**：收竿后的 60 tick（3 秒）内，在浮标落点 **8 格**半径内寻找新出现的生物，
   取最近者作为本次收获的海兽。人形海兽（Banshee、Frog Man、Alligator、鲨鱼系列、
   Spooky 系列等）在 Hypixel 上以合成玩家实体呈现，模组内置官方 wiki 的**水/熔岩海怪
   全量名单**，结合「是否在 Tab 列表」双重判定区分海兽 NPC 与真实玩家（不依赖名字里的等级
   前缀——真实 SkyBlock 玩家的名字前同样带等级），不会误伤真人。攻击中若海兽被别人击杀、被击退出范围或战斗超过 20 秒，自动
   停手并继续钓鱼。

---

### 🐠 Hypixel 空岛钓鱼小贴士

- **咬钩信号**：Hypixel 空岛的鱼竿浮标在可收竿时会冒「!!!」提示，本模组正是靠这个信号判定
  收竿时机的，所以请确保游戏画面里能够看到浮标（不要故意把视野拉到看不到的地方）。
- **站位**：站在能正对水面的实心平台上，且周围留有空间；平台越小，「随机移动」越倾向于
  原地不动——这是安全设计。
- **附魔与掉落**：鱼竿附魔（鱼饵 Lure、海之眷顾 Luck of the Sea 等）的具体收益，以及海兽战斗规则，请以 **Hypixel
  SkyBlock Wiki** 为准：[hypixel-skyblock.fandom.com/wiki/Fishing](https://hypixel-skyblock.fandom.com/wiki/Fishing)。
- **自动攻击**：只有你的钓点会出「海兽」才需要开启 `Auto Attack`；普通钓鱼点请保持关闭。

---

### ❓ 常见问题（FAQ）

**Q1：开了却没反应？**
① 主手必须是钓鱼竿；② 面前要有可见的水面；③ 配置页开关均为开启。

**Q2：从不自动收竿？**
说明服务器没有弹出「!!!」标记（极少数自定义服务器会移除该提示），本模组依赖它判定，无法兼容
此类服务器。

**Q3：会跳进水里吗？**
不会。随机移动带有逐格地面探测，找不到安全落点就原地不动；内置的自动跳跃功能已移除。

**Q4：配置保存在哪？**
游戏目录下 `config/astrail/config.json`，界面里的改动会自动同步落盘。

**Q5：抛出后立刻又被收回？**
把「抛竿延迟 Throw Delay」调大（建议 15~20 tick），或等网络稳定后再挂机。

**Q6：人形海怪（Banshee / Frog Man / Alligator / 鲨鱼 / Spooky 系列等）能自动攻击吗？**
可以。这些海怪在服务器上以「玩家」外形实体呈现，本模组通过「海怪名名单 + Tab 列表」
双重判定区分真实玩家与海怪 NPC（名单覆盖官方 wiki 水/熔岩海怪全表），会正常识别并
攻击；反过来，真人玩家永远不会被当成海怪。

**Q7：武器是 Soul Whip 之类的「钓鱼武器」时无法自动攻击？**
Soul Whip 等钓鱼武器在游戏里属于钓鱼竿类型的物品，旧版本会误把它当成钓竿而拒绝
切换。现在武器槽位里的任何物品（含 Soul Whip）都会被当作武器使用——只要它不是
当前主手正拿着的那根钓竿。

---

### 🏗 构建（开发者）

```bash
# Windows
gradlew.bat build --console=plain -q
# macOS / Linux
./gradlew build --console=plain -q
```

产物 `build/libs/astrail-fisher-0.1.0.jar`，丢进 `mods/` 即可。

### 📁 项目结构

- `src/main/java/.../feature/macro/fishing/AutoFishModule.java` —— 自动钓鱼核心逻辑（状态机）
- `src/main/java/.../platform/minecraft/AstrailKeybinds.java` —— 打开配置页的按键绑定
- `src/main/java/.../mixin/KeyboardInputMixin.java` —— 将模组的模拟按键注入原版移动输入
- `使用说明.txt` —— 面向普通玩家的一页纸中文手册

### 🔏 许可

代码以 **MIT** 协议开源（见 [LICENSE](LICENSE)）；内置字体资源遵循其各自许可
（见 `assets/astrail/font/inter/ofl.txt`）。

---

## English

<p align="center">
  <a href="#简体中文">简体中文</a> · <b>English</b>
</p>

An **auto-fishing mod built specifically for Hypixel SkyBlock** (Minecraft 26.2, Fabric).
Hold a rod, and it does the rest.

Astrail Fisher handles the entire fishing loop for you: it casts when your hook is gone,
reels the instant the bobber raises the signature **「!!!」** bite prompt, re-aims at the
water and casts again — no key presses, no babysitting.

It is not a blind right-click bot either. A few **humanization** details — a short random
footstep after each catch, a subtle camera wobble, crouching while stepping — keep the
session looking like an actual player instead of a machine.

- **100% client-side** — no memory reads, no fabricated packets, no server data touched
- **Defaults tuned for Hypixel SkyBlock fishing** — works out of the box
- **Single-page settings** — every option lives on one screen, nothing buried

### ✨ Features

- **Full auto loop**: cast → reel on bite → re-aim → recast, forever
- **「!!!」 bite detection**: uses Hypixel's native bobber prompt for instant, precise timing
- **Humanization**: random movement, sneaking while stepping, always-sneak, subtle rotation
- **Auto Reset**: hooks that get stuck or vanish are pulled in and recast after ~20 s
- **Auto Attack (optional)**: when a Sea Creature bites, swap to your weapon slot and use
  its ability, then return to fishing. Aim assist can be turned off so teleport
  weapons like Hyperion never yank you off your fishing spot, and a **Single Use**
  option right-clicks each sea creature exactly once to avoid draining mana.
- **Precise creature detection** — humanoid sea creatures (Banshee, Frog Man, ...) are
  recognised; only the creature that spawned at your own bobber is attacked (never a
  neighbour's), and the fight stops when it is killed by others, leaves range, or times out.

### 🛠 Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 26.2 (client) |
| Fabric Loader | ≥ 0.19.3 |
| Fabric API | ≥ 0.154.2+26.2 |
| Java | ≥ 25 |

### 📦 Installation

1. Grab `astrail-fisher-0.1.0.jar` from **Releases**.
2. Drop it into `.minecraft/mods/` (Fabric setup required).
3. Launch the game and join Hypixel (or any world).

### 🚀 Quick Start

1. Equip the fishing rod in your **main hand**.
2. Press **Right Shift** (default) to open the Auto Fishing page.
3. Stand at your spot facing water, press `ESC` — fishing begins immediately.
4. Press the same key again whenever you want to stop.

> Rebind it anytime under **Options → Controls → Astrail Fisher**.

### ⚙️ Configuration

All options are client-side and persist automatically to `config/astrail/config.json` —
no manual editing needed. Use the table as a cheat-sheet; details follow.

| # | Setting | Default | Range | What it does |
| --- | --- | --- | --- | --- |
| 1 | Random Movement | ON | – | Small random step in a safe direction after each catch |
| 2 | Always Sneak | ON | – | keep the catch steady, resistant to small nudges |
| 3 | Auto Reset | ON | – | reel & recast a hook stuck or missing (works with #4) |
| 4 | Reset Timeout | 20 s | 5–120 s | seconds without a bite before the hook is reeled and recast |
| 5 | Retry Limit | 5 | 1–50 | consecutive stuck retries before auto fishing turns itself off |
| 6 | Throw Delay | 10 ticks | 1–30 | ticks between reel and next cast (1 tick = 1/20 s) |
| 7 | View Lock | ON | – | lock the camera on the rod spot before each cast; off = free view |
| 8 | Subtle Rotation | ON | – | small camera drift after each catch |
| 9 | Auto Attack | OFF | – | fight Sea Creatures with a weapon (needs #10–#13) |
| 10 | Attack CPS | 5 | 1–10 | right-clicks per second while fighting a sea creature |
| 11 | Aim Before Attack | ON | – | aim at the monster before right-click; off = camera never moved (Hyperion-friendly) |
| 12 | Single Use | OFF | – | right-click each sea creature exactly once, then stop (saves mana) |
| 13 | Weapon Slot | 1 | 1–9 | hotbar slot holding the weapon |

**1) Random Movement (ON)**
After every reel, the player takes one short random step sideways or backward — sometimes
a step back to the original spot — like a real player shifting their weight. It always
checks the ground ahead first: on a 1×1 water edge where no direction is safe, it simply
stands still. **Never steps into water, never auto-jumps.** (Each step comes with a
brief crouch; that behaviour is fixed and needs no toggle.)

**2) Always Sneak (ON)**
Keeps the player crouched for the whole session to minimise tiny knockback, pushes and
platform drift near the pond edge. Turn off if your server dislikes perma-sneak.

**3) Auto Reset (ON)**
If a hook exists for longer than the **Reset Timeout** with no bite signal — stuck on a bank,
wedged in a block, or swallowed by the server — it is pulled in and recast. Set-and-forget.

**4) Reset Timeout (20 s, 5–120 s)**
Seconds without a bite before the hook counts as stuck and gets recast. Raise it for slow
fishing spots, lower it to recycle empty hooks faster. Only active while #3 is on.

**5) Retry Limit (5, 1–50)**
After this many consecutive stuck-hook resets (from #3) with no catch in between, the mod
**turns auto fishing off** and tells you in chat, so it never spends forever casting into
nothing. Any successful catch restarts the count.

**6) Throw Delay (10 ticks, 1–30)**
The pause between reel and next cast. Bump it to 15–20 on laggy connections to avoid the
cast-then-instant-reel loop; lower it for a quicker rhythm on good ping.

**7) View Lock (ON)**
Before each recast the mod smoothly swings your camera back to the rod landing spot and
only throws once the water is in view. Turn it **off** and the camera is never moved:
the rod goes out on schedule in whatever direction you are looking. If you are not
facing water the cast may land badly — Auto Reset cleans that up.

**8) Subtle Rotation (ON)**
A ~1–2° camera wobble and return after every catch. Cosmetic only.

**9) Auto Attack (OFF) + 10) Attack CPS (5) + 11) Aim Before Attack (ON) + 12) Single Use (OFF) + 13) Weapon Slot (1–9)**
If your pond spawns **Sea Creatures**, the mod will switch to the configured hotbar slot,
use the weapon ability, and continue fishing once the fight is over. Enable only when your
fishing spot actually spawns monsters.

**10) Attack CPS (5, 1–10)**: how many right-clicks per second the mod fires while fighting.
Five per second suits most weapons; lower it for weapons with a hidden cooldown, like the
Soul Whip (0.5 s), so clicks are not wasted.

**11) Aim Before Attack (ON)**: with it on, the mod swings your camera to the monster
before every right-click so abilities connect. Turn it **off** and the camera is never
moved — it only swaps weapons and clicks on schedule, leaving the aiming to you.
This is the setting for **Hyperion-style teleport weapons**: with aiming on, the
right-click teleports you onto the monster and away from your fishing spot; with it off
you right-click a safe direction yourself and the explosion still clears the mob.

**12) Single Use (OFF)**: with it on, the mod right-clicks **exactly once** per caught
sea creature and immediately switches back to fishing — no repeated clicks, no wasted
mana. With it off (default) it keeps clicking until the mob dies or leaves the attack
range (a range guard now stops it instead of clicking into the air).
---

### 🔄 How It Works

(All timings in game ticks; 1 tick = 1/20 s.)

1. **Auto cast** — with no hook in the water and nothing queued, casts after ~14 ticks
   (0.7 s), with a 40-tick floor between idle casts.
2. **Bite detection** — a 6-tick warm-up window ignores stale markers from the previous
   catch; the first 「!!!」-named entity within 3 blocks of your bobber triggers the reel
   (each marker counts once).
3. **Recast** — finishes the humanized motion; with View Lock on it restores the aim
   that caught the fish and confirms an open water line (the wait caps at 60 ticks).
   With View Lock off the camera is never moved and the cast goes out on schedule.
4. **Auto Reset** — a hook idle past the Reset Timeout (20 s = 400 ticks by default) is
   reeled and recast (settings #3, #4).
5. **Sea creature detection** — within 60 ticks (3 s) of the reel, a newly arrived living
   entity within 8 blocks of the bobber spot is taken as this catch's creature (nearest
   wins). Humanoid creatures (Banshee, Frog Man, Alligator, the sharks, the Spooky set, ...)
   render as synthetic player entities on Hypixel; the mod recognises them through its
   **full water/lava sea creature name list from the official wiki**, plus the tab list (level prefixes are deliberately ignored — real SkyBlock players
   carry a level before their name too), so actual players are never targeted. The fight
   stops when
   the creature is killed by someone else, knocked out of range, or survives 20 seconds.

---

### 🐠 Hypixel SkyBlock Tips

- The Hypixel bobber shows **「!!!」** when ready to reel — that is the exact signal this
  mod reads, so keep the bobber in view.
- Fish from a solid platform with visible water; on a very narrow edge the random
  movement safely stays put — that is intentional.
- For rod enchants, Sea Creature odds and combat rules, always consult your current
  edition of the [Hypixel SkyBlock Wiki](https://hypixel-skyblock.fandom.com/wiki/Fishing).
- Enable `Auto Attack` only where your spot spawns Sea Creatures.

---

### ❓ FAQ

**Q1: Enabled but nothing happens?**
Rod in main hand? Facing water? All toggles ON? That covers 99% of the cases.

**Q2: Never auto-reels?**
The marker `「!!!」` is required. Custom servers that strip it from the bobber cannot be
supported — the mod has nothing to read.

**Q3: Will it walk me into the water?**
No. Every step is ground-checked; if nothing is safe the player won't move, and jump
behaviour was removed entirely.

**Q4: Where is the config file?**
`config/astrail/config.json` in the game folder, saved automatically.

**Q5: Cast-and-instant-reel loop?**
Raise Throw Delay to 15–20 ticks or fish on a steadier connection.

**Q6: Do humanoid sea creatures (Banshee / Frog Man / Alligator / sharks / the Spooky set) get attacked?**
Yes. They appear as player-shaped entities on the server; the mod tells real players apart
via the full wiki sea creature name list and the tab list, so they are detected and fought
normally — and real players never are.

**Q7: My weapon is a fishing weapon like the Soul Whip and it never gets used.**
The Soul Whip and similar fishing weapons are fishing-rod items in the game, which older
builds rejected as a plain rod. Any item in the weapon slot is now used as the weapon —
as long as it is not the very rod currently held in the main hand.

---

### 🏗 Building

```bash
# Windows
gradlew.bat build --console=plain -q
# macOS / Linux
./gradlew build --console=plain -q
```

Artifact: `build/libs/astrail-fisher-0.1.0.jar`.

### 📁 Repository Layout

- `src/main/java/.../feature/macro/fishing/AutoFishModule.java` — the fishing state machine
- `src/main/java/.../platform/minecraft/AstrailKeybinds.java` — GUI key binding (default Right Shift)
- `src/main/java/.../mixin/KeyboardInputMixin.java` — fuses synthetic input into vanilla controls
- `使用说明.txt` — a pocket-sized Chinese manual for players

### 🔄 License

MIT — see [LICENSE](LICENSE). Bundled fonts keep their own licenses
(`assets/astrail/font/inter/ofl.txt`).

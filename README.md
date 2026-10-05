# CompassUnlock

**Explorer's Compass 的全员传送附属模组 / A teleport-unlock addon for Explorer's Compass**

> English summary: a small server-side companion mod for Minecraft **1.20.1 Forge**. It mixes into
> `PlayerUtils#canTeleport` of [Explorer's Compass](https://github.com/MattCzyr/ExplorersCompass) so that
> **every player** can teleport to a located structure, instead of only creative / opped / cheat-mode players.
> Install it on the server only — clients need nothing beyond Explorer's Compass itself.

---

## 它做什么

原版探险者指南针只有「创造模式 / OP / 开了作弊」的玩家才能看到并使用传送按钮。
本模组把这道门禁拆掉：**任何玩家**都能搜索结构，然后点传送按钮直接过去。

## 为什么只装服务端就生效

探险者指南针里传送权限只由**一个方法**决定：

```java
// com.chaosthedude.explorerscompass.util.PlayerUtils
public static boolean canTeleport(MinecraftServer server, Player player) {
    return cheatModeEnabled(server, player) || isOp(player);
}
```

它只有两处调用，而且**两处都在服务端**：

| 调用处 | 作用 |
| --- | --- |
| `ExplorersCompassItem#use` | 结果随 `SyncPacket` 发给客户端 → **决定客户端显不显示传送按钮** |
| `TeleportPacket#handle` | 决定服务端**是否真的执行传送** |

关键点：客户端「按钮显不显示」不是它自己判断的，而是**服务端告诉它的**。
所以只要在服务端让这个方法返回 `true`，客户端就会收到「可以传送」并显示按钮，
点击后的传送也在服务端执行 —— **客户端一个字节都不用改**。

而搜索结构本来就对所有人开放（1.20.1 的 1.3.3 / 1.4.0 都没有经验消耗，默认 `defaultXpLevels = 0`）。

## 实现

一个 Mixin，在方法入口直接返回 `true`：

```java
@Mixin(targets = "com.chaosthedude.explorerscompass.util.PlayerUtils", remap = false)
public class PlayerUtilsMixin {
    @Inject(
            method = "canTeleport(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void compassunlock$allowEveryPlayer(MinecraftServer server, Player player,
                                                       CallbackInfoReturnable<Boolean> cir) {
        if (Config.allowAllPlayersTeleport()) {
            cir.setReturnValue(true);
        }
    }
}
```

几个刻意的设计：

- **`targets = "类名字符串"` 而不是 `@Mixin(PlayerUtils.class)`**
  → 编译期完全不依赖探险者指南针，本工程单独就能构建，不需要把对方的 jar 放进 `libs`。
- **`remap = false`**
  → Forge 1.20.1 只重映射成员名、类名保持官方名，方法描述符在开发和线上完全一致，因此不需要 refmap。
- **`at = @At("HEAD")` + `cancellable`**
  → 在入口就返回，原方法体一行都不执行。

## 安装

把 `compassunlock-1.0.0.jar` 放进 `mods` 目录：

- 服务器：`<服务端>\mods\`
- 单机整合包：`<整合包>\.minecraft\mods\`

**只放服务端即可，客户端不用放** —— 客户端只要有探险者指南针。

删除这个 jar 就完全恢复原样，探险者指南针本身不会被改动。

## 配置

`config/compassunlock-common.toml`

```toml
[General]
	# true  = 所有玩家都能传送（默认）
	# false = 恢复原版行为（只有创造 / OP / 开作弊能传送）
	allowAllPlayersTeleport = true
```

探险者指南针自己的 `allowTeleport` 仍是**总闸**：设为 `false` 时谁都传不了。

## 兼容性

| 目标 | 结果 |
| --- | --- |
| 原版 Explorer's Compass 1.20.1-1.3.3-forge | ✅ 已实测 |
| 原版 Explorer's Compass 1.20.1-1.4.0-forge | ✅ 已实测 |
| [MCMostWolf/ExplorersCompassEdited](https://github.com/MCMostWolf/ExplorersCompassEdited) 1.3.4 / 1.3.6 | ✅ 已实测 |

分支版没有改动传送权限，其 `PlayerUtils.class` 与原版**逐字节相同**（sha256 `5176cb30…`），
且不自带 Mixin 配置，因此不冲突。注意分支版与原版**是同一个 modId**，不能同时安装。

## 构建

需要 **JDK 17**。

```bash
./gradlew build
```

产物：`build/libs/compassunlock-1.0.0.jar`

> **开发环境注意**：Mixin 配置是通过 jar 清单里的 `MixinConfigs` 属性注册的，
> 而 ForgeGradle 的开发环境把模组挂成**目录**（目录没有 MANIFEST），所以开发环境下 Mixin 不会加载。
> 要本地验证必须把构建出的 jar 丢进 `run/mods/`。`build.gradle` 里的 `mods { }` 已注释掉并附说明。

## 验证方式

模组带一个常驻启动自检：服务器启动时反射加载探险者指南针**真实被加载的** `PlayerUtils`，
调用 `canTeleport(null, null)` 并打日志。生效时输出：

```
[mixin/]: Mixing PlayerUtilsMixin from compassunlock.mixins.json into com.chaosthedude.explorerscompass.util.PlayerUtils
[CompassUnlock/]: Teleport unlock ACTIVE - every player can teleport to a structure located with Explorer's Compass.
```

不生效时会打 WARN，而不是闷声失灵。

## 许可与致谢

- 本模组代码采用 **MIT**，不含 Explorer's Compass 的任何代码，只是按名字引用它的一个方法。
- [Explorer's Compass](https://github.com/MattCzyr/ExplorersCompass) 作者 **ChaosTheDude**，
  采用 **CC BY-NC-SA 4.0**。本模组是其功能层面的附属扩展，未复制其代码，亦未重新分发其产物。

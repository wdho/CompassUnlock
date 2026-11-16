# CompassUnlock

**A teleport-unlock addon for Explorer's Compass** (Minecraft **1.20.1 Forge**)

**Language:** [中文](README.md) · English

---

> See [DISCLAIMER.md](DISCLAIMER.md) for project and AI contribution disclosures.

---

## What it does

By default, Explorer's Compass only lets players in creative mode, opped players, or players with cheats enabled
see and use the teleport button. This addon removes that gate: **any player** can search for a structure and click
the teleport button to travel straight there.

## Why installing it on the server alone is enough

Teleport permission in Explorer's Compass is decided by **a single method**:

```java
// com.chaosthedude.explorerscompass.util.PlayerUtils
public static boolean canTeleport(MinecraftServer server, Player player) {
    return cheatModeEnabled(server, player) || isOp(player);
}
```

It has exactly two call sites, and **both of them run on the server**:

| Call site | Effect |
| --- | --- |
| `ExplorersCompassItem#use` | The result goes out in a `SyncPacket` → **decides whether the client shows the teleport button** |
| `TeleportPacket#handle` | Decides whether the server **actually performs the teleport** |

The key point: the client does not decide whether to show the button — **the server tells it**. So if the server
makes this method return `true`, the client is told "teleport allowed" and shows the button, and the teleport itself
also runs on the server — **not one byte on the client has to change**.

Searching for structures is already open to everyone (neither 1.3.3 nor 1.4.0 for 1.20.1 charges XP; the default
`defaultXpLevels` is `0`).

## How it works

One Mixin that returns `true` right at the method entry:

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

Three deliberate choices:

- **`targets = "class name string"` instead of `@Mixin(PlayerUtils.class)`**
  → no compile-time dependency on Explorer's Compass at all, so this project builds on its own; you never have to
  drop the other mod's jar into `libs/`.
- **`remap = false`**
  → Forge 1.20.1 remaps member names only and keeps official class names, so the method descriptor is identical in
  development and in production. That is why no refmap is needed.
- **`at = @At("HEAD")` + `cancellable`**
  → it returns at the entry point, so not a single line of the original method body runs.

## Install

Drop `compassunlock-1.0.0.jar` into your `mods` directory:

- Server: `<server>\mods\`
- Single-player modpack: `<instance>\.minecraft\mods\`

**The server is enough — you do not need it on the client.** Clients only need Explorer's Compass itself.

Delete the jar and everything goes back to normal; Explorer's Compass itself is never modified.

## Config

`config/compassunlock-common.toml`

```toml
[General]
	# true  = every player may teleport (default)
	# false = restore the original behaviour (creative / opped / cheats only)
	allowAllPlayersTeleport = true
```

Explorer's Compass's own `allowTeleport` option is still the **master switch**: set it to `false` and nobody can
teleport at all.

## Compatibility

| Target | Result |
| --- | --- |
| Explorer's Compass 1.20.1-1.3.3-forge | ✅ tested |
| Explorer's Compass 1.20.1-1.4.0-forge | ✅ tested |
| [MCMostWolf/ExplorersCompassEdited](https://github.com/MCMostWolf/ExplorersCompassEdited) 1.3.4 / 1.3.6 | ✅ tested |

The fork does not touch the teleport permission: its `PlayerUtils.class` is **byte-for-byte identical** to the
original (sha256 `5176cb30…`), and it ships no Mixin configuration of its own, so there is no conflict. Note that the
fork uses **the same modId** as the original, so the two cannot be installed side by side.

## Building

Requires **JDK 17**.

```bash
./gradlew build
```

Output: `build/libs/compassunlock-1.0.0.jar`

> **Development caveat**: a Mixin configuration is registered through the `MixinConfigs` attribute in the jar
> manifest, but the ForgeGradle development environment mounts a mod as a **directory** — and a directory has no
> MANIFEST — so the Mixin will not load in a dev run. To test locally you must drop the built jar into `run/mods/`.
> The `mods { }` block in `build.gradle` is commented out with a note explaining this.

## Verifying it works

The mod ships a permanent self check: when the server starts, it reflectively loads the **actually loaded**
`PlayerUtils` from Explorer's Compass, calls `canTeleport(null, null)` and logs the result. When the unlock is in
effect you get:

```
[mixin/]: Mixing PlayerUtilsMixin from compassunlock.mixins.json into com.chaosthedude.explorerscompass.util.PlayerUtils
[CompassUnlock/]: Teleport unlock ACTIVE - every player can teleport to a structure located with Explorer's Compass.
```

If it is not in effect you get a WARN instead of a silent failure.

## Declaration

- Author and maintainer: **wdho**. The maintainer determines requirements, design choices and releases. The original disclosure records AI-generated code, build scripts and documentation using **DeepSeek Harness / `deepseek-flash`**. This declaration rewrite used **OpenAI Codex assistance**.
- This is an unofficial community mod, not affiliated with, partnered with or endorsed by ChaosTheDude, Mojang Studios, Microsoft or the Forge team. Game names, trademarks and third-party content belong to their respective owners.
- Original code in this repository is released under [MIT](LICENSE). Retain copyright and license notices when redistributing. MIT does not cover Minecraft, Forge or upstream mods.
- [Explorer's Compass](https://github.com/MattCzyr/ExplorersCompass) is by **ChaosTheDude** and uses **CC BY-NC-SA 4.0**. This addon's MIT license does not replace or extend the upstream license or grant rights to upstream names, code or artifacts.
- This addon changes Explorer's Compass teleport-permission checks so ordinary players can teleport. Server administrators should understand this behaviour and may disable it in the configuration.
- Verification and supported versions are limited to the records in the README; no claim is made for every game version, mod combination or server environment. Provided as is; warranty and liability terms are in [LICENSE](LICENSE).
- See [DISCLAIMER.md](DISCLAIMER.md) for the full bilingual declaration.

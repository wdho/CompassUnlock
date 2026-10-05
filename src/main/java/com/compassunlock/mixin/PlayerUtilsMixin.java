package com.compassunlock.mixin;

import com.compassunlock.Config;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Explorer's Compass only shows and honours the teleport button for players who are in
 * creative mode, opped, or in cheat mode. That single decision lives in
 * {@code com.chaosthedude.explorerscompass.util.PlayerUtils#canTeleport}, which is called
 * in exactly two places, both on the server:
 *
 * <ul>
 *   <li>{@code ExplorersCompassItem#use} - the result is put into the sync packet that tells
 *       the client whether to show the teleport button.</li>
 *   <li>{@code TeleportPacket#handle} - the result decides whether the server actually teleports.</li>
 * </ul>
 *
 * Because both call sites run on the server, overriding this one method on the server is enough:
 * the client needs no addon installed at all, it just receives "teleport allowed" from the server.
 *
 * The target is referenced by name only, so this addon has no compile-time dependency on
 * Explorer's Compass. The descriptor uses class names, which are identical in development and
 * production (Forge 1.20.1 keeps official class names and only remaps member names), hence
 * {@code remap = false} everywhere.
 */
@Mixin(targets = "com.chaosthedude.explorerscompass.util.PlayerUtils", remap = false)
public class PlayerUtilsMixin {

	@Inject(
			method = "canTeleport(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/world/entity/player/Player;)Z",
			at = @At("HEAD"),
			cancellable = true,
			remap = false
	)
	private static void compassunlock$allowEveryPlayer(MinecraftServer server, Player player, CallbackInfoReturnable<Boolean> cir) {
		if (Config.allowAllPlayersTeleport()) {
			cir.setReturnValue(true);
		}
	}
}

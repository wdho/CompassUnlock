package com.compassunlock;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Server-side companion addon for Explorer's Compass.
 *
 * <p>Install it on the server (or in the mods folder of a single player world) and every player can
 * use the compass to search for a structure and then click the teleport button. Nothing has to be
 * installed on the client beyond Explorer's Compass itself.</p>
 */
@Mod(CompassUnlock.MODID)
public class CompassUnlock {

	public static final String MODID = "compassunlock";

	public static final Logger LOGGER = LogManager.getLogger("CompassUnlock");

	private static final String PLAYER_UTILS = "com.chaosthedude.explorerscompass.util.PlayerUtils";

	private static volatile String lastProbeError = "unknown";

	public CompassUnlock() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "compassunlock-common.toml");
		MinecraftForge.EVENT_BUS.register(this);
	}

	/**
	 * Self check. Loading PlayerUtils forces the Mixin to be applied, so this reports whether the
	 * addon actually took effect instead of leaving the server admin guessing.
	 */
	@SubscribeEvent
	public void onServerStarted(ServerStartedEvent event) {
		final Boolean active = probeCanTeleport();

		if (Boolean.TRUE.equals(active)) {
			LOGGER.info("Teleport unlock ACTIVE - every player can teleport to a structure located with Explorer's Compass.");
		} else if (Boolean.FALSE.equals(active)) {
			LOGGER.warn("Teleport unlock NOT active - PlayerUtils.canTeleport still denies ordinary players. "
					+ "Make sure the Mixin applied (no earlier error about compassunlock.mixins.json) and that "
					+ "allowAllPlayersTeleport is true in config/compassunlock-common.toml.");
		} else {
			LOGGER.warn("Teleport unlock could not be verified: {}", lastProbeError);
		}
	}

	/**
	 * Calls {@code PlayerUtils.canTeleport(null, null)} reflectively.
	 *
	 * <p>The mixin short-circuits the method before either argument is touched, so nulls are safe.
	 * If the mixin did not apply, the original code also tolerates nulls (it returns false), so the
	 * probe can never crash the server - it just reports the wrong answer.</p>
	 */
	private static Boolean probeCanTeleport() {
		try {
			final Class<?> playerUtils = Class.forName(PLAYER_UTILS);
			final Method canTeleport = playerUtils.getMethod("canTeleport", MinecraftServer.class, Player.class);
			final Object result = canTeleport.invoke(null, new Object[] { null, null });
			return (Boolean) result;
		} catch (Throwable t) {
			lastProbeError = t.toString();
			return null;
		}
	}
}

package com.compassunlock;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

	private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

	public static final General GENERAL = new General(BUILDER);
	public static final ForgeConfigSpec SPEC = BUILDER.build();

	public static class General {
		public final ForgeConfigSpec.BooleanValue allowAllPlayersTeleport;

		General(ForgeConfigSpec.Builder builder) {
			builder.push("General");

			allowAllPlayersTeleport = builder
					.comment("When true, every player may teleport to a structure located with Explorer's Compass,",
							"regardless of game mode, cheat mode or op status.",
							"Set to false to restore the original behaviour (creative / opped / cheats only).")
					.define("allowAllPlayersTeleport", true);

			builder.pop();
		}
	}

	/**
	 * Read defensively: this is consulted from a Mixin that may run before the config is loaded,
	 * and the whole point of the addon is to allow teleporting.
	 */
	public static boolean allowAllPlayersTeleport() {
		try {
			return GENERAL.allowAllPlayersTeleport.get();
		} catch (Exception e) {
			return true;
		}
	}
}

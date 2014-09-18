package com.github.worldsender.mcanm;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = "mcanm", name = "Minecraft Animated", version = "0.0.101a_1710")
public class MCAnm {

	@Mod.Instance("mcanm")
	public static MCAnm instance;

	public static Logger logger;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent pre) {
		logger = pre.getModLog();
	}
}

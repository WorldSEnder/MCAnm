package com.github.worldsender.mcanm;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@Mod(
		modid = "@MODID@",
		name = "Minecraft Animated",
		version = "@VERSION@",
		guiFactory = "com.github.worldsender.mcanm.client.config.MCAnmGuiFactory")
public class MCAnm {

	@Mod.Instance("@MODID@")
	public static MCAnm instance;

	public static Logger logger;

	@SidedProxy(modId = "@MODID@",
			clientSide = "com.github.worldsender.mcanm.client.ClientProxy",
			serverSide = "com.github.worldsender.mcanm.server.ServerProxy")
	public static Proxy proxy;

	private Configuration config;
	public static Property enableReload;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent pre) {
		logger = pre.getModLog();
		config = new Configuration(pre.getSuggestedConfigurationFile());
		config.load();
		enableReload = config.get(Configuration.CATEGORY_GENERAL,
				Reference.config_reload_enabled, true).setLanguageKey(
				Reference.gui_config_reload_enabled);
		config.save();
		proxy.register();
		FMLCommonHandler.instance().bus().register(this);
		logger.info("Successfully loaded MC Animations");
	}

	@SubscribeEvent
	public void onConfigChange(OnConfigChangedEvent occe) {
		if (!occe.modID.equals("@MODID@"))
			return;
		if (config.hasChanged())
			config.save();
	}
}

package com.github.worldsender.mcanm;

import org.apache.logging.log4j.Logger;

import com.github.worldsender.mcanm.client.config.MCAnmConfiguration;
import com.github.worldsender.mcanm.test.CubeEntity;

import cpw.mods.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.EntityRegistry;

@Mod(
		modid = Reference.core_modid,
		name = Reference.core_modname,
		version = Reference.core_modversion,
		guiFactory = "com.github.worldsender.mcanm.client.config.MCAnmGuiFactory")
public class MCAnm {
	/**
	 * Enables various visual outputs, e.g. the bones of models are rendered.
	 */
	public static final boolean isDebug = true;

	@Mod.Instance(Reference.core_modid)
	public static MCAnm instance;

	@SidedProxy(
			modId = Reference.core_modid,
			clientSide = "com.github.worldsender.mcanm.client.ClientProxy",
			serverSide = "com.github.worldsender.mcanm.server.ServerProxy")
	public static Proxy proxy;

	private MCAnmConfiguration config;
	private Logger logger;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent pre) {
		logger = pre.getModLog();
		config = new MCAnmConfiguration(pre.getSuggestedConfigurationFile());
		proxy.preInit();
		FMLCommonHandler.instance().bus().register(this);
		logger.info("Successfully loaded MC Animations");
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		if (!isDebug)
			return;
		int id = 0;
		EntityRegistry.registerModEntity(CubeEntity.class, "Cube", id, this, 80, 1, true);
		proxy.init();
	}

	@SubscribeEvent
	public void onConfigChange(OnConfigChangedEvent occe) {
		if (!occe.modID.equals(Reference.core_modid))
			return;
		config.onConfigChange(occe);
	}

	public static MCAnmConfiguration configuration() {
		return instance.getConfiguration();
	}

	public MCAnmConfiguration getConfiguration() {
		return this.config;
	}

	public static Logger logger() {
		return instance.getLogger();
	}

	public Logger getLogger() {
		return this.logger;
	}
}

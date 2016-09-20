package com.github.worldsender.mcanm;

import org.apache.logging.log4j.Logger;

import com.github.worldsender.mcanm.client.config.MCAnmConfiguration;
import com.github.worldsender.mcanm.test.CubeEntity;
import com.github.worldsender.mcanm.test.CubeEntityV2;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

@Mod(
		modid = Reference.core_modid,
		name = Reference.core_modname,
		version = Reference.core_modversion,
		guiFactory = "com.github.worldsender.mcanm.client.config.MCAnmGuiFactory")
public class MCAnm {
	/**
	 * Enables various visual outputs, e.g. the bones of models are rendered.
	 */
	public static final boolean isDebug = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");

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
		MinecraftForge.EVENT_BUS.register(this);
		logger.info("Successfully loaded MC Animations");
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		if (isDebug) {
			EntityRegistry.registerModEntity(CubeEntity.class, "Cube", 0, this, 80, 1, true);
			EntityRegistry.registerModEntity(CubeEntityV2.class, "CubeV2", 1, this, 80, 1, true);
		}
		proxy.init();
	}

	@SubscribeEvent
	public void onConfigChange(OnConfigChangedEvent occe) {
		if (!occe.getModID().equals(Reference.core_modid))
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

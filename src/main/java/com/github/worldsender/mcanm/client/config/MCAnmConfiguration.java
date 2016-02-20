package com.github.worldsender.mcanm.client.config;

import java.io.File;
import java.util.List;

import com.github.worldsender.mcanm.Reference;

import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class MCAnmConfiguration {

	private Configuration config;
	private Property enableReload;

	public MCAnmConfiguration(File loadFile) {
		config = new Configuration(loadFile);
		config.load();
		enableReload = config.get(Configuration.CATEGORY_GENERAL, Reference.config_reload_enabled, true)
				.setLanguageKey(Reference.gui_config_reload_enabled);
		save();
	}

	public void onConfigChange(OnConfigChangedEvent occe) {
		save();
	}

	public void save() {
		if (config.hasChanged())
			config.save();
	}

	public boolean isReloadEnabled() {
		return this.enableReload.getBoolean();
	}

	public void addPropertiesToDisplayList(List<IConfigElement> list) {
		list.add(new ConfigElement<Boolean>(enableReload));
	}
}

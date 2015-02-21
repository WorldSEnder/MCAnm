package com.github.worldsender.mcanm.client.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.Reference;

public class MCAnmGUI extends GuiConfig {

	public MCAnmGUI(GuiScreen parent) {
		super(parent, getConfigElements(), Reference.core_modid, false, false,
				I18n.format(Reference.gui_config_title));
	}

	private static List<IConfigElement> getConfigElements() {
		List<IConfigElement> list = new ArrayList<IConfigElement>();
		list.add(new ConfigElement(MCAnm.enableReload));
		return list;
	}
}

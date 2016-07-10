package com.github.worldsender.mcanm.client.config;

import java.util.ArrayList;
import java.util.List;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.Reference;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

public class MCAnmGUI extends GuiConfig {

	public MCAnmGUI(GuiScreen parent) {
		super(parent, getConfigElements(), Reference.core_modid, false, false, I18n.format(Reference.gui_config_title));
	}

	private static List<IConfigElement> getConfigElements() {
		List<IConfigElement> list = new ArrayList<>();
		MCAnm.configuration().addPropertiesToDisplayList(list);
		return list;
	}
}

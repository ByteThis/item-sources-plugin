package com.itemsources;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("itemsources")
public interface ItemSourcesConfig extends Config
{
	@ConfigSection(
			name = "Show Sections",
			description = "Toggle which data types appear in the panel",
			position = 0
	)
	String showSections = "showSections";

	@ConfigItem(keyName = "showAll", name = "All Sources", description = "Show All Sources", position = 1, section = "showSections")
	default boolean showMonsterDrops() { return true; }

	@ConfigItem(keyName = "showShops", name = "Shops", description = "Show Shop locations", position = 2, section = "showSections")
	default boolean showShops() { return true; }

	@ConfigItem(keyName = "showSpawns", name = "Spawns", description = "Show Spawns", position = 3, section = "showSections")
	default boolean showSpawns() { return true; }

	@ConfigSection(
			name = "Settings",
			description = "Plugin behavior settings",
			position = 4
	)
	String settingsSection = "settingsSection";

	@ConfigItem(keyName = "shiftClickOnly", name = "Shift + Right Click", description = "Only show the menu option when holding Shift", position = 5, section = "settingsSection")
	default boolean shiftClickOnly() { return false; }
}
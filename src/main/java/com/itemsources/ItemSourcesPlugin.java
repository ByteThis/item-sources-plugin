package com.itemsources;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import java.io.IOException;

@PluginDescriptor(name = "Item Sources")
public class ItemSourcesPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientToolbar clientToolbar;
	@Inject private ItemSourcesConfig config;
	@Inject private OkHttpClient okHttpClient;
	@Inject private ItemManager itemManager;

	private ItemSourcesPanel panel;
	private NavigationButton navButton;
	private static final String MENU_OPTION = "Item Sources";
	private static final String USER_AGENT = "runelite-item-sources-plugin";
	private static final String WIKI_DOMAIN = "https://oldschool.runescape.wiki";

	@Override
	protected void startUp()
	{
		panel = new ItemSourcesPanel();
		panel.onSearch(e -> {
			String input = panel.getSearchText();
			if (input == null || input.trim().isEmpty()) return;

			var results = itemManager.search(input);
			if (results.isEmpty()) return;

			// Priority 1: Exact match (case-insensitive)
			// Priority 2: First partial match
			String targetName = results.stream()
					.filter(i -> i.getName().equalsIgnoreCase(input.trim()))
					.findFirst()
					.map(i -> i.getName())
					.orElse(results.get(0).getName());

			fetchItemData(targetName);
		});

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		navButton = NavigationButton.builder()
				.tooltip("Item Sources")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() { clientToolbar.removeNavigation(navButton); }

	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		if (config.shiftClickOnly() && !client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)) return;

		MenuEntry[] entries = client.getMenuEntries();
		for (int i = 0; i < entries.length; i++)
		{
			MenuEntry entry = entries[i];
			if (!Text.removeTags(entry.getOption()).equals("Examine")) continue;

			MenuAction type = entry.getType();
			int itemId = -1;

			// 1. Handle Inventory and Bank (Widget-based)
			if (type == MenuAction.CC_OP_LOW_PRIORITY)
			{
				itemId = entry.getItemId();
			}
			// 2. Handle Ground Items
			else if (type == MenuAction.EXAMINE_ITEM_GROUND)
			{
				itemId = entry.getIdentifier();
			}
			
			if (itemId <= 0) continue;

			client.createMenuEntry(i)
					.setOption(MENU_OPTION)
					.setTarget(entry.getTarget())
					.setType(MenuAction.RUNELITE)
					.setIdentifier(itemId)
					.setParam0(entry.getParam0())
					.setParam1(entry.getParam1());
			break;
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuOption().equals(MENU_OPTION))
		{
			int itemId = event.getId();
			if (itemId <= 0) return;

			String itemName = client.getItemDefinition(itemId).getName();

			SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
			fetchItemData(itemName);
		}
	}

	private void fetchItemData(String itemName)
	{
		SwingUtilities.invokeLater(() -> panel.updateContent(itemName.toUpperCase(), "<i>Fetching data...</i>", null));

		String wikiUrl = WIKI_DOMAIN + "/w/" + itemName.replace(" ", "_") + "?action=render";		Request request = new Request.Builder().url(wikiUrl).header("User-Agent", USER_AGENT).build();

		var searchResults = itemManager.search(itemName);
		java.awt.image.BufferedImage icon = searchResults.isEmpty() ? null : itemManager.getImage(searchResults.get(0).getId());

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				SwingUtilities.invokeLater(() -> panel.updateContent("Error", "Could not connect to Wiki.", null));
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (response)
				{
					if (!response.isSuccessful()) return;
					String html = response.body().string();
					SwingUtilities.invokeLater(() -> panel.updateContent(itemName.toUpperCase(), parseWikiData(html, itemName), icon));
				}
			}
		});
	}

	private String parseWikiData(String html, String itemName)
	{
		Document doc = Jsoup.parse(html);
		StringBuilder sb = new StringBuilder();
		boolean found = false;

		if (config.showMonsterDrops()) {
			String data = extractTable(doc, "Item_sources", new int[]{0, 1, 3}, -1, 2, true, itemName);
			if (!data.isEmpty()) {
				sb.append("<b style='color:#a5ff7f;'>ALL SOURCES</b>").append(data).append("<br>");
				found = true;
			}
		}
		if (config.showShops()) {
			String data = extractTable(doc, "Shop_locations", new int[]{0, 1, 4}, 2, 0, false, itemName);
			if (!data.isEmpty()) {
				sb.append("<b style='color:#7fb5ff;'>SHOPS</b>").append(data).append("<br>");
				found = true;
			}
		}
		if (config.showSpawns()) {
			String data = extractTable(doc, "Item_spawns", new int[]{0, 2}, -1, 0, false, itemName);
			if (!data.isEmpty()) {
				sb.append("<b style='color:#ff7f7f;'>SPAWNS</b>").append(data).append("<br>");
				found = true;
			}
		}

		return found ? sb.toString() : "No data found for selected categories.";
	}

	private String extractTable(Document doc, String sectionId, int[] cols, int filterIdx, int sortColIdx, boolean isRaritySort, String currentItem)
	{
		Element anchor = doc.getElementById(sectionId);
		if (anchor == null) return "";

		Element table = anchor.parent().nextElementSibling();
		while (table != null && !table.tagName().equals("table") && !table.tagName().matches("h[1-6]")) table = table.nextElementSibling();
		if (table == null || !table.tagName().equals("table")) return "";

		StringBuilder html = new StringBuilder("<table width='100%' style='border-collapse:collapse; font-size:9px; table-layout:fixed;'>");
		Elements rows = table.select("tr");

		List<ExtractedRow> tableData = new ArrayList<>();
		List<String> header = new ArrayList<>();

		for (int i = 0; i < rows.size(); i++)
		{
			Elements cells = rows.get(i).select("th, td");
			if (filterIdx != -1 && cells.size() > filterIdx && cells.get(filterIdx).text().trim().equals("0")) continue;

			ExtractedRow row = new ExtractedRow();
			for (int idx : cols)
			{
				if (idx < cells.size())
				{
					Element cell = cells.get(idx);
					String val = cell.text().replaceAll("\\[edit\\]", "").replace("Price sold at", "Price").trim();

					// Use data attribute for correct math/display
					Element raritySpan = cell.selectFirst("span[data-drop-oneover]");
					if (raritySpan != null) val = raritySpan.attr("data-drop-oneover");

					row.rowData.add(val);

					if (idx == 0) {
						Element link = cell.selectFirst("a");
						// Preserve anchors like #High
						row.linkUrl = (link != null) ? WIKI_DOMAIN + link.attr("href") : WIKI_DOMAIN + "/w/" + val.replace(" ", "_");
					}
				}
			}

			if (i == 0) header = row.rowData;
			else if (!row.rowData.isEmpty()) tableData.add(row);
		}

		// Sorting Logic
		tableData.sort((r1, r2) -> {
			String val1 = r1.rowData.get(sortColIdx);
			String val2 = r2.rowData.get(sortColIdx);

			if (isRaritySort) {
				// Convert to commonality denominator (Always = 0, 1/10 = 10, 1/100 = 100)
				// Lower denominator = more common = top of ascending list
				double d1 = parseRarityToDenominator(val1);
				double d2 = parseRarityToDenominator(val2);
				if (d1 != d2) return Double.compare(d1, d2);
			}
			// Tie breaker or standard sort by Name
			return r1.rowData.get(0).compareToIgnoreCase(r2.rowData.get(0));
		});

		html.append("<tr>");
		for (int i = 0; i < header.size(); i++) {
			String align = (i == 0) ? "left" : "center";
			html.append("<th align='").append(align).append("' style='color:#999; padding:4px 2px;'>").append(header.get(i)).append("</th>");
		}
		html.append("</tr>");

		for (ExtractedRow row : tableData)
		{
			html.append("<tr style='border-top:1px solid #444;'>");
			for (int i = 0; i < row.rowData.size(); i++)
			{
				String align = (i == 0) ? "left" : "center";
				String content = row.rowData.get(i);

				if (i == 0) {
					String url = row.linkUrl;
					if (sectionId.equals("Item_spawns") || sectionId.equals("Spawns")) {
						url = WIKI_DOMAIN + "/w/" + currentItem.replace(" ", "_") + "#Item_spawns";
					}
					content = "<a href='" + url + "' style='color:#ff9800; text-decoration:none;'>" + content + "</a>";
				}

				html.append("<td align='").append(align).append("' style='padding:5px 2px;'>").append(content).append("</td>");
			}
			html.append("</tr>");
		}

		return html.append("</table>").toString();
	}

	private double parseRarityToDenominator(String rarity)
	{
		if (rarity.equalsIgnoreCase("Always")) return 0.0;
		try {
			String clean = rarity.toLowerCase().replace(" ", "").replace("×", "*");
			if (clean.contains("*")) {
				String[] parts = clean.split("\\*");
				// Math: multiplier * (1 / denominator). To get rarity value, we do 1 / result.
				double probability = Double.parseDouble(parts[0]) * evaluateFraction(parts[1]);
				return probability == 0 ? Double.MAX_VALUE : 1.0 / probability;
			}
			double prob = evaluateFraction(clean);
			return prob == 0 ? Double.MAX_VALUE : 1.0 / prob;
		} catch (Exception e) { return Double.MAX_VALUE; }
	}

	private double evaluateFraction(String ratio)
	{
		if (ratio.contains("/")) {
			String[] parts = ratio.split("/");
			return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
		}
		try { return Double.parseDouble(ratio); } catch (Exception e) { return 0.0; }
	}

	private static class ExtractedRow {
		List<String> rowData = new ArrayList<>();
		String linkUrl = "";
	}

	@Provides
	ItemSourcesConfig provideConfig(ConfigManager configManager) { return configManager.getConfig(ItemSourcesConfig.class); }
}
package org.kaimac;

import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import okhttp3.*;
import com.google.gson.*;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

@PluginDescriptor(name = "omg im rich")
public class OmgImRich extends Plugin {
	private static final String BACKEND_URL = "http://localhost:8000/api/recommendations";
	private final OkHttpClient httpClient = new OkHttpClient();
	private final Gson gson = new Gson();

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MemeOverlay overlay;

	private OmgImRichPanel panel;
	private NavigationButton navButton;

	private int gold = 0;

	@Getter
	private final Map<Integer, Recommendation> recommendedItems = new HashMap<>();
	private final Set<Integer> alreadySuggestedToBuy = new HashSet<>();
	private final Set<Integer> alreadySuggestedToSell = new HashSet<>();

	@Override
	protected void startUp() {
		overlayManager.add(overlay);
		panel = new OmgImRichPanel();
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panel_icon.png");
		navButton = NavigationButton.builder()
				.tooltip("Omg Im Rich")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();
		clientToolbar.addNavigation(navButton);
		fetchRecommendations();
	}

	@Override
	protected void shutDown() {
		overlayManager.remove(overlay);
		if (navButton != null) {
			clientToolbar.removeNavigation(navButton);
		}
		recommendedItems.clear();
		alreadySuggestedToBuy.clear();
		alreadySuggestedToSell.clear();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY)) {
			gold = Arrays.stream(event.getItemContainer().getItems())
					.filter(i -> i.getId() == ItemID.COINS_995)
					.mapToInt(Item::getQuantity)
					.sum();
			fetchRecommendations();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick tick) {
		if (client.getGameState() != GameState.LOGGED_IN) return;

		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null) return;

		boolean geOpen = client.getWidget(WidgetInfo.GRAND_EXCHANGE_WINDOW_CONTAINER) != null;
		if (!geOpen) {
			return;
		}

		for (Item item : inventory.getItems()) {
			int itemId = item.getId();
			if (recommendedItems.containsKey(itemId) && !alreadySuggestedToSell.contains(itemId)) {
				alreadySuggestedToSell.add(itemId);
				Recommendation r = recommendedItems.get(itemId);
				overlay.setMessage("SELL " + r.name.toUpperCase() + " x" + r.quantity + " @ " + r.sell + " ea");
				return;
			}
		}

		if (gold > 0) {
			for (Recommendation r : recommendedItems.values()) {
				if (!alreadySuggestedToBuy.contains(r.id)) {
					alreadySuggestedToBuy.add(r.id);
					overlay.setMessage("BUY " + r.name.toUpperCase() + " x" + r.quantity + " @ " + r.buy + " ea");
					return;
				}
			}
		}


	}

	private void fetchRecommendations() {
		System.out.println("omg im asking for items!!!!!!!!");
		if (gold <= 0 || panel == null) return;

		httpClient.newCall(new Request.Builder()
				.url(HttpUrl.parse(BACKEND_URL).newBuilder()
						.addQueryParameter("gold", String.valueOf(gold))
						.build())
				.build()).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				System.err.println("Failed to fetch recommendations: " + e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				System.out.println("omg we got some stuff!!!!!!!");
				if (!response.isSuccessful()) {
					System.err.println("Unexpected response: " + response);
					return;
				}

				String body = response.body().string();
				JsonElement jsonElement = new JsonParser().parse(body);
				JsonArray jsonArray = jsonElement.getAsJsonArray();
				List<String> results = new ArrayList<>();

				recommendedItems.clear();
				alreadySuggestedToBuy.clear();
				alreadySuggestedToSell.clear();

				GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
				Set<Integer> geItemIds = new HashSet<>();
				for (GrandExchangeOffer offer : offers) {
					if (offer != null && offer.getState() != GrandExchangeOfferState.EMPTY) {
						geItemIds.add(offer.getItemId());
					}
				}

				for (JsonElement element : jsonArray) {
					JsonObject obj = element.getAsJsonObject();
					int itemId = obj.get("id").getAsInt();
					String itemName = obj.get("name").getAsString();
					int predictedLow = obj.get("predicted_low").getAsInt();
					int predictedHigh = obj.get("predicted_high").getAsInt();
					int profit = obj.get("actual_profit").getAsInt();

					if (geItemIds.contains(itemId)) {
						System.out.println("Skipping " + itemName + " - already in GE slot");
						continue;
					}

					recommendedItems.put(itemId, new Recommendation(
							itemId, itemName, predictedLow, predictedHigh, 1  // you can adjust quantity logic later
					));

					results.add(String.format("%s: Buy %d → Sell %d | Profit: %d", itemName, predictedLow, predictedHigh, profit));
				}

				clientThread.invokeLater(() -> panel.updateResults(results));
			}
		});
	}

	private static class Recommendation {
		int id;
		String name;
		int buy;
		int sell;
		int quantity;

		Recommendation(int id, String name, int buy, int sell, int quantity) {
			this.id = id;
			this.name = name;
			this.buy = buy;
			this.sell = sell;
			this.quantity = quantity;
		}
	}
}



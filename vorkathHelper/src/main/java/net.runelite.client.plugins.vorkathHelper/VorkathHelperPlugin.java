package net.runelite.client.plugins.vorkathHelper;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.basicapi.BasicApiPlugin;
import net.runelite.client.plugins.basicapi.utils.Inventory;
import net.runelite.client.plugins.basicapi.utils.PrayerUtils;
import net.runelite.client.plugins.iutils.*;
import net.runelite.client.plugins.iutils.game.Game;
import net.runelite.client.plugins.iutils.scripts.iScript;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.*;

import static net.runelite.api.GraphicID.VORKATH_BOMB_AOE;
import static net.runelite.api.GraphicID.VORKATH_ICE;
import static net.runelite.api.ObjectID.ACID_POOL_32000;


@Extension
@PluginDependency(iUtils.class)
@PluginDependency(BasicApiPlugin.class)
@PluginDescriptor(
	name = "Vorkath Assistant",
	description = "Automatic Vorkath",
	tags = {"Vorkath"}
)
@Slf4j
public class VorkathHelperPlugin extends iScript {

	@Inject
	private VorkathHelperConfig config;

	@Inject
	private Client client;

	@Inject
	private PrayerUtils prayerUtils;

	@Inject
	private iUtils utils;

	@Inject
	private Inventory inventory;

	@Inject
	private NPCUtils npcUtils;

	@Inject
	private PlayerUtils playerUtils;

	@Inject
	private WalkUtils walkUtils;

	@Inject
	private ObjectUtils objectUtils;

	@Inject
	private Game game;

	@Inject
	private CalculationUtils calc;

	@Inject
	private BasicApiPlugin basicApi;

	private final List<WorldPoint> acidSpots;
	private List<WorldPoint> acidFreePath;
	private long sleepLength;
	private int timeout;
	private boolean dodgeFirebomb;
	private WorldPoint fireBallPoint;
	private WorldPoint safeTile;
	private List<WorldPoint> safeMeleeTiles;
	private boolean isAcid;
	private boolean isMinion;
	private boolean startPlugin;
	private final Set<Integer> DIAMOND_SET = Set.of(ItemID.DIAMOND_DRAGON_BOLTS_E, ItemID.DIAMOND_BOLTS_E);
	private final Set<Integer> RUBY_SET = Set.of(ItemID.RUBY_DRAGON_BOLTS_E, ItemID.RUBY_BOLTS_E);

	public VorkathHelperPlugin() {
		acidSpots = new ArrayList<>();
		acidFreePath = new ArrayList<>();
		safeMeleeTiles = new ArrayList<>();
		fireBallPoint = null;
		safeTile = null;
	}

	@Provides
	VorkathHelperConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(VorkathHelperConfig.class);
	}

	@Override
	protected void startUp() {
		startPlugin = false;
		log.info("Vorkath Helper startUp");
	}

	@Override
	protected void shutDown() {
		startPlugin = false;
		log.info("Vorkath Helper shutDown");
		stop();
	}

	@Override
	protected void onStart() {
		game.sendGameMessage("Vorkath Helper started");
		startPlugin = true;
		safeMeleeTiles.clear();
		dodgeFirebomb = false;
		timeout = 0;
	}

	@Override
	protected void onStop() {
		game.sendGameMessage("Vorkath Helper stopped");
		startPlugin = false;
		isAcid = false;
		dodgeFirebomb = false;
		isMinion = false;
		safeTile = null;
		fireBallPoint = null;
		safeMeleeTiles.clear();
		acidFreePath.clear();
		acidSpots.clear();
		timeout = 0;
	}

	@Subscribe
	public void onGameTick(GameTick event){

		if(client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null || !startPlugin) return;

		final Player player = client.getLocalPlayer();
		final LocalPoint localLoc = player.getLocalLocation();

		if(isVorkathAsleep() || !isAtVorkath()) {
			safeMeleeTiles.clear();
			isMinion = false;
			isAcid = false;
			dodgeFirebomb = false;
			return;
		}

		if(!isAcid()){
			isAcid = false;
			safeTile = null;
			acidFreePath.clear();
			acidSpots.clear();
		}

		Widget runOrb = client.getWidget(WidgetInfo.MINIMAP_RUN_ORB);

		createSafetiles();

		switch(getState()){

			case TIMEOUT:
				timeout--;
				break;
			case TOGGLE_RUN:
				playerUtils.enableRun(runOrb.getBounds());
				break;
			case DODGE_BOMB:
				LocalPoint bomb = LocalPoint.fromWorld(client, fireBallPoint);
				LocalPoint dodgeRight = new LocalPoint(bomb.getX() + 256, bomb.getY()); //Local point is 1/128th of a tile. 256 = 2 tiles
				LocalPoint dodgeLeft = new LocalPoint(bomb.getX() - 256, bomb.getY());
				LocalPoint dodgeReset = new LocalPoint(6208, 7872);

				if(dodgeFirebomb && !player.getWorldLocation().equals(fireBallPoint)){
					fireBallPoint = null;
					dodgeFirebomb = false;
					return;
				}
				if(localLoc.getY() > 7872){
					walkUtils.sceneWalk(dodgeReset, 0, sleepDelay());
					dodgeFirebomb = false;
					timeout+=2;
					return;
				}
				if (localLoc.getX() < 6208) {
					walkUtils.sceneWalk(dodgeRight, 0, sleepDelay());
				} else {
					walkUtils.sceneWalk(dodgeLeft, 0, sleepDelay());
				}

				break;
			case KILL_MINION:
				NPC iceMinion = npcUtils.findNearestNpc(NpcID.ZOMBIFIED_SPAWN_8063);

				if(player.getInteracting() != null && player.getInteracting().getName() != null && player.getInteracting().getName().equalsIgnoreCase("Vorkath")){ //Stops attacking vorkath
					walkUtils.sceneWalk(localLoc, 0, sleepDelay());
					return;
				}
				if(prayerUtils.isQuickPrayerActive() && config.enablePrayer()){ //Turns pray off during this phase *Could probably rearrange getState to have it toggle prayer off there, will change up later.
					prayerUtils.toggleQuickPrayer( false, sleepDelay());
					return;
				}
				if(iceMinion != null && player.getInteracting() == null) {
					attackMinion();
					timeout+=4;
				}
				break;
			case ACID_WALK:
				NPC vorkath = npcUtils.findNearestNpc(NpcID.VORKATH_8061);
				Widget runWidget = client.getWidget(WidgetInfo.MINIMAP_RUN_ORB);
				Widget prayerWidget = client.getWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
				if (runWidget != null && playerUtils.isRunEnabled() && player.isMoving()) {
					utils.doInvokeMsTime(new LegacyMenuEntry("Toggle Run", "", 1, MenuAction.CC_OP, -1,
							10485783, false), 0);
				}

				if(prayerWidget != null && prayerUtils.isQuickPrayerActive() && (config.walkMethod().getId() != 2 || (config.walkMethod().getId() == 2 && player.isMoving()))){
					prayerUtils.toggleQuickPrayer(false, 0);
				}

				if(config.walkMethod().getId() == 1) return;
				if(config.walkMethod().getId() == 2){
					if(!acidSpots.isEmpty()){
						if(acidFreePath.isEmpty()){
							calculateAcidFreePath();
						}

						WorldPoint firstTile;
						WorldPoint lastTile;
						if(!acidFreePath.isEmpty()){
							firstTile = acidFreePath.get(0);
						}else{
							return;
						}

						if(acidFreePath.size() > config.acidFreePathLength()){
							lastTile = acidFreePath.get(config.acidFreePathLength() -1);
						}else{
							lastTile = acidFreePath.get(acidFreePath.size() - 1);
						}

						log.info("First tile: " + firstTile);
						log.info("Last Tile: " + lastTile);
						log.info("Actual length: " + (firstTile.getX() != lastTile.getX() ? Math.abs(firstTile.getX() - lastTile.getX()) : Math.abs(firstTile.getY() - lastTile.getY())));

						if(acidFreePath.contains(player.getWorldLocation())){
							if(player.getWorldLocation().equals(firstTile)){
								walkUtils.sceneWalk(lastTile, 0, sleepDelay());
							}
							if(player.getWorldLocation().equals(lastTile)){
								walkUtils.sceneWalk(firstTile, 0, sleepDelay());
							}
						}else if(!player.isMoving()){
							walkUtils.sceneWalk(lastTile, 0, 0);
						}

					}
				}
				else {
					Collections.sort(safeMeleeTiles, Comparator.comparingInt(o -> o.distanceTo(player.getWorldLocation())));

					if (safeTile == null) {
						for (int i = 0; i < safeMeleeTiles.size(); i++) {
							WorldPoint temp = safeMeleeTiles.get(i);
							WorldPoint temp2 = new WorldPoint(temp.getX(), temp.getY() - 1 , temp.getPlane());
							if (!acidSpots.contains(temp) && !acidSpots.contains(temp2)) {
								safeTile = temp2;
								break;
							}
						}
					}

					if(safeTile != null){
						if(player.getWorldLocation().equals(safeTile)){
							utils.doNpcActionMsTime(vorkath, MenuAction.NPC_SECOND_OPTION.getId(), 0);
						}else{
							LocalPoint lp = LocalPoint.fromWorld(client, safeTile);
							if(lp != null){
								walkUtils.walkTile(lp.getSceneX(), lp.getSceneY());
							}else{
								log.info("Local point is a null");
							}
						}
					}
				}

				break;
			case SWITCH_RUBY:
				inventory.interactWithItem(RUBY_SET.stream().mapToInt(Integer::intValue).toArray(), sleepDelay(), "Wield");
				break;
			case SWITCH_DIAMOND:
				inventory.interactWithItem(DIAMOND_SET.stream().mapToInt(Integer::intValue).toArray(), sleepDelay(), "Wield");
				break;
			case RETALIATE:
				attackVorkath();
			case QUICKPRAYER_ON:
				if(!prayerUtils.isQuickPrayerActive() && prayerUtils.getRemainingPoints() > 0)
					prayerUtils.toggleQuickPrayer( true, sleepDelay());
				break;
			case QUICKPRAYER_OFF:
				if(prayerUtils.isQuickPrayerActive())
					prayerUtils.toggleQuickPrayer( false, sleepDelay());
				break;
			case DEFAULT:
				break;
		}
	}

	@Override
	protected void loop() {

	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged event) {
		if(client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null || !isAtVorkath()) return;

		Player player = client.getLocalPlayer();

		WorldPoint playerWorldLocation = player.getWorldLocation();

		LocalPoint playerLocalLocation = player.getLocalLocation();

		Actor actor = event.getActor();

		if(actor == null) return;

		if(actor.getAnimation() == 7889 || actor.getAnimation() == 7891){ //Minion death + hit animation (which is when he technically dies)
			isMinion = false;
			return;
		}

		if(actor.getName().equalsIgnoreCase("Vorkath") && actor.getAnimation() == 7957) //Waits one tick during his acid animation for projectile paths to calculate.
			timeout+=1;

	}
	@Subscribe
	private void onProjectileSpawned(ProjectileSpawned event) {

		if(client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null || !isAtVorkath()) return;

		Player player = client.getLocalPlayer();

		Projectile projectile = event.getProjectile();

		WorldPoint playerWorldLocation = player.getWorldLocation();

		if (projectile.getId() == VORKATH_BOMB_AOE && config.dodgeBomb()){
			fireBallPoint = playerWorldLocation;
			dodgeFirebomb = true;
		}

		if (projectile.getId() == VORKATH_ICE)
			isMinion = true;

		if(projectile.getId() == 1483)
			isAcid = true;

	}

	@Subscribe
	private void onProjectileMoved(ProjectileMoved event) {
		Projectile projectile = event.getProjectile();
		WorldPoint position = WorldPoint.fromLocal(client, event.getPosition());

		if(client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null || !isAtVorkath()) return;

		if (projectile.getId() == 1483) {
			addAcidSpot(position);
		}

	}

	private VorkathStates getState(){
		NPC vorkathAlive = npcUtils.findNearestNpc(NpcID.VORKATH_8061);
		Player player = client.getLocalPlayer();

		if(player == null) return null;

		if(timeout > 0 && (config.dodgeBomb() && !dodgeFirebomb))
			return VorkathStates.TIMEOUT;

		if(isAtVorkath() && !playerUtils.isRunEnabled() && !isAcid)
			return VorkathStates.TOGGLE_RUN;

		if(config.dodgeBomb() && dodgeFirebomb)
			return VorkathStates.DODGE_BOMB;

		if(config.killSpawn() && isMinion)
			return VorkathStates.KILL_MINION;

		if(config.walkMethod().getId() != 1 && isAcid)
			return VorkathStates.ACID_WALK;

		if(config.switchBolts() && vorkathAlive == null
				&& playerUtils.isItemEquipped(DIAMOND_SET) && inventory.contains(RUBY_SET.stream().mapToInt(Integer::intValue).toArray()))
			return VorkathStates.SWITCH_RUBY;

		if(config.switchBolts() && vorkathAlive != null && !vorkathAlive.isDead()
				&& playerUtils.isItemEquipped(RUBY_SET)
				&& inventory.contains(DIAMOND_SET.stream().mapToInt(Integer::intValue).toArray())
				&& calculateHealth(vorkathAlive) > 0
				&& calculateHealth(vorkathAlive) < 260
				&& vorkathAlive.getAnimation() != 7960
				&& vorkathAlive.getAnimation() != 7957)
			return VorkathStates.SWITCH_DIAMOND;

		if(config.enablePrayer() && isAtVorkath() && prayerUtils.isQuickPrayerActive()
				&& prayerUtils.getRemainingPoints() > 0
				&& (isVorkathAsleep()
				|| (vorkathAlive != null && vorkathAlive.isDead())
				|| isMinion
				|| isAcid))
			return VorkathStates.QUICKPRAYER_OFF;

		if(config.enablePrayer() && prayerUtils.getRemainingPoints() > 0 && isAtVorkath() && !prayerUtils.isQuickPrayerActive()
				&& ((vorkathAlive != null
				&& !vorkathAlive.isDead()
				&& !isAcid
				&& !isMinion) || isWakingUp()))
			return VorkathStates.QUICKPRAYER_ON;

		if(config.fastRetaliate() && isAtVorkath() && player.getInteracting() == null
				&& vorkathAlive != null && !vorkathAlive.isDead()
				&& !isMinion
				&& !dodgeFirebomb
				&& !isAcid
				&& !isWakingUp()) // No idea why this check is here given alive / asleep are two different ids lol
			return VorkathStates.RETALIATE;

		return VorkathStates.DEFAULT;
	}

	private long sleepDelay() {
		sleepLength = calc.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		return sleepLength;
	}

	public void attackVorkath(){
		NPC vorkath = npcUtils.findNearestNpc(NpcID.VORKATH_8061);
		if(vorkath != null && vorkath.getAnimation() != 7957 && (vorkath.getAnimation() != 7949 || !vorkath.isDead())) //Acid animation check
			utils.doNpcActionMsTime(vorkath, MenuAction.NPC_SECOND_OPTION.getId(), sleepDelay());
	}

	public void attackMinion(){
		NPC iceMinion = npcUtils.findNearestNpc(NpcID.ZOMBIFIED_SPAWN_8063);
		if(iceMinion != null && !iceMinion.isDead()) {
			LegacyMenuEntry entry = new LegacyMenuEntry("Cast", "", iceMinion.getIndex(), MenuAction.WIDGET_TARGET_ON_NPC.getId(), 0, 0, false);
			utils.oneClickCastSpell(WidgetInfo.SPELL_CRUMBLE_UNDEAD, entry, iceMinion.getConvexHull().getBounds(), sleepDelay());
		}
	}

	/*
	This method creates an initial row of tiles depending on the walk method.
	If the user uses the melee woox walk, it will create a row directly in front of vorkath which is then used to create a secondary walk tile behind
	If the user uses the ranged woox walk, it will create a row at the entrance of the instance that will then be used to create a secondary walk tile in front
	Blowpipe walk does the above, but 2 tiles in front due to the range of the weapon.
	 */
	public void createSafetiles(){
		if(isAtVorkath()){
			if(safeMeleeTiles.size() > 8) safeMeleeTiles.clear();

			LocalPoint southWest = config.walkMethod().getId() == 3 ? new LocalPoint(5824, 7872) : config.walkMethod().getId() == 4 ? new LocalPoint(5824, 7104) : new LocalPoint(5824, 7360);
			WorldPoint base = WorldPoint.fromLocal(client, southWest);

			for(int i = 0; i < 7; i++){
				safeMeleeTiles.add(new WorldPoint(base.getX() + i, base.getY(), base.getPlane()));
			}
		}else if(!isAtVorkath() && !safeMeleeTiles.isEmpty()){
			safeMeleeTiles.clear();
		}
	}

	public boolean isAtVorkath(){
		NPC vorkath = npcUtils.findNearestNpc("Vorkath");
		return client.isInInstancedRegion() && vorkath != null;
	}

	public boolean isVorkathAsleep(){
		NPC vorkathAsleep = npcUtils.findNearestNpc(NpcID.VORKATH_8059);
		return isAtVorkath() && vorkathAsleep != null;
	}

	public boolean isWakingUp(){
		NPC vorkathWaking = npcUtils.findNearestNpc(NpcID.VORKATH_8058);
		return vorkathWaking != null;
	}

	private void addAcidSpot(WorldPoint worldPoint) {
		if (!acidSpots.contains(worldPoint))
			acidSpots.add(worldPoint);
	}

	/*
	Taken from xKylees vorkath plugin
	https://github.com/xKylee/plugins-source/blob/master/vorkath/src/main/java/net/runelite/client/plugins/vorkath/VorkathPlugin.java
	*/

	private void calculateAcidFreePath()
	{
		acidFreePath.clear();

		Player player = client.getLocalPlayer();
		NPC vorkath = npcUtils.findNearestNpc(NpcID.VORKATH_8061);

		if (vorkath == null)
		{
			return;
		}

		final int[][][] directions = {
				{
						{0, 1}, {0, -1} // Positive and negative Y
				},
				{
						{1, 0}, {-1, 0} // Positive and negative X
				}
		};

		List<WorldPoint> bestPath = new ArrayList<>();
		double bestClicksRequired = 99;

		final WorldPoint playerLoc = player.getWorldLocation();
		final WorldPoint vorkLoc = vorkath.getWorldLocation();
		final int maxX = vorkLoc.getX() + 14;
		final int minX = vorkLoc.getX() - 8;
		final int maxY = vorkLoc.getY() - 1;
		final int minY = vorkLoc.getY() - 8;

		// Attempt to search an acid free path, beginning at a location
		// adjacent to the player's location (including diagonals)
		for (int x = -1; x < 2; x++)
		{
			for (int y = -1; y < 2; y++)
			{
				final WorldPoint baseLocation = new WorldPoint(playerLoc.getX() + x,
						playerLoc.getY() + y, playerLoc.getPlane());

				if (acidSpots.contains(baseLocation) || baseLocation.getY() < minY || baseLocation.getY() > maxY)
				{
					continue;
				}

				// Search in X and Y direction
				for (int d = 0; d < directions.length; d++)
				{
					// Calculate the clicks required to start walking on the path
					double currentClicksRequired = Math.abs(x) + Math.abs(y);
					if (currentClicksRequired < 2)
					{
						currentClicksRequired += Math.abs(y * directions[d][0][0]) + Math.abs(x * directions[d][0][1]);
					}
					if (d == 0)
					{
						// Prioritize a path in the X direction (sideways)
						currentClicksRequired += 0.5;
					}

					List<WorldPoint> currentPath = new ArrayList<>();
					currentPath.add(baseLocation);

					// Positive X (first iteration) or positive Y (second iteration)
					for (int i = 1; i < 25; i++)
					{
						final WorldPoint testingLocation = new WorldPoint(baseLocation.getX() + i * directions[d][0][0],
								baseLocation.getY() + i * directions[d][0][1], baseLocation.getPlane());

						if (acidSpots.contains(testingLocation) || testingLocation.getY() < minY || testingLocation.getY() > maxY
								|| testingLocation.getX() < minX || testingLocation.getX() > maxX)
						{
							break;
						}

						currentPath.add(testingLocation);
					}

					// Negative X (first iteration) or positive Y (second iteration)
					for (int i = 1; i < 25; i++)
					{
						final WorldPoint testingLocation = new WorldPoint(baseLocation.getX() + i * directions[d][1][0],
								baseLocation.getY() + i * directions[d][1][1], baseLocation.getPlane());

						if (acidSpots.contains(testingLocation) || testingLocation.getY() < minY || testingLocation.getY() > maxY
								|| testingLocation.getX() < minX || testingLocation.getX() > maxX)
						{
							break;
						}

						currentPath.add(testingLocation);
					}

					if (currentPath.size() >= config.acidFreePathLength() && currentClicksRequired < bestClicksRequired
							|| (currentClicksRequired == bestClicksRequired && currentPath.size() > bestPath.size()))
					{
						bestPath = currentPath;
						bestClicksRequired = currentClicksRequired;
					}
				}
			}
		}

		if (bestClicksRequired != 99)
		{
			acidFreePath = bestPath;
		}
	}


	private int calculateHealth(NPC target) {
		// Based on OpponentInfoOverlay HP calculation & taken from the default slayer plugin
		if (target == null || target.getName() == null)
		{
			return -1;
		}

		final int healthScale = target.getHealthScale();
		final int healthRatio = target.getHealthRatio();
		final int maxHealth = 750;

		if (healthRatio < 0 || healthScale <= 0)
		{
			return -1;
		}

		return (int)((maxHealth * healthRatio / healthScale) + 0.5f);
	}

	public boolean isAcid(){
		GameObject pool = objectUtils.findNearestGameObject(ACID_POOL_32000);
		NPC vorkath = npcUtils.findNearestNpc(NpcID.VORKATH_8061);
		return pool != null || (vorkath != null && vorkath.getAnimation() == 7957);
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
		if (!configButtonClicked.getGroup().equalsIgnoreCase("VorkathAssistantConfig"))
			return;

		if (configButtonClicked.getKey().equalsIgnoreCase("startHelper")) {
			if (!startPlugin) {
				start();
			} else {
				stop();
			}
		}
	}

}

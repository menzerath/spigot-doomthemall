package pro.marvin.minecraft.doomthemall;

import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;

import java.util.*;

public class DoomThemAll extends JavaPlugin implements Listener {
	// Game-stats ...
	public HashMap<Integer, Boolean> arenaConfig = new HashMap<>();
	private HashMap<Integer, Block> updateSigns = new HashMap<>();

	private HashMap<Integer, Boolean> gameStarted = new HashMap<>();
	private HashMap<Integer, Integer> countdown = new HashMap<>();

	public List<Player> playersList = new ArrayList<>();
	public HashMap<String, Integer> playersMap = new HashMap<>();
	public HashMap<String, String> playersTeam = new HashMap<>();

	private HashMap<String, Long> reloadTime = new HashMap<>();
	private HashMap<String, Boolean> canDie = new HashMap<>();

	private HashMap<Integer, Integer> blueScore = new HashMap<>();
	private HashMap<Integer, Integer> redScore = new HashMap<>();

	private HashMap<Integer, String> playerOnKillstreak = new HashMap<>();
	private HashMap<Integer, Integer> numberOfKillstreak = new HashMap<>();

	// Config-file
	public static int maxScore;
	public static int maxPlayers;
	public static String gameWorldPrefix;
	public static boolean funForOPs;

	// Scoreboard
	private ScoreboardManager manager;
	private Scoreboard board;
	private Objective[] objective = new Objective[101];
	private Score[] scoreRed = new Score[101];
	private Score[] scoreBlue = new Score[101];

	public static final String PERMISSION_PREMIUM = "doomthemall.premium";

	/**
	 * Set-up important timers and other stuff
	 */
	@Override
	public void onEnable() {
		getCommand("dta").setExecutor(new CommandManager(this));
		getServer().getPluginManager().registerEvents(this, this);
		manager = Bukkit.getScoreboardManager();
		board = manager.getNewScoreboard();

		init();

		loadConfig();

		// Start repeating tasks
		countdown();
		reloadTime();
		updateSigns();
	}

	/**
	 * Remove every player from the arena
	 */
	@Override
	public void onDisable() {
		for (Player p : playersList) {
			p.setGameMode(GameMode.ADVENTURE);
			p.getInventory().clear();
			p.getInventory().setHelmet(new ItemStack(Material.AIR));
			p.teleport(dtaSpawn());
			p.removePotionEffect(PotionEffectType.SPEED);
			p.removePotionEffect(PotionEffectType.JUMP);
			p.setExp(0);
			p.setLevel(0);
			p.setScoreboard(manager.getNewScoreboard());
		}
	}

	/**
	 * Prepare the HashMaps
	 */
	private void init() {
		for (int i = 1; i < 100; i++) {
			gameStarted.put(i, false);
			countdown.put(i, -1);
			playerOnKillstreak.put(i, "");
			numberOfKillstreak.put(i, 0);
			arenaConfig.put(i, false);
		}
	}

	/**
	 * A game will start. Prepare the players and send them in the arena.
	 *
	 * @param mapId          Which map / arena will start
	 * @param countdownStart Method called by countdown or not?
	 */
	public void startGame(int mapId, boolean countdownStart) {
		if (!countdownStart) {
			// Additional 10sec countdown
			countdown.put(mapId, 10);
			return;
		}
		countdown.put(mapId, -1);

		// Prepare world
		World w = getServer().getWorld(gameWorldPrefix + mapId);
		w.setTime(6000);
		w.setDifficulty(Difficulty.PEACEFUL);
		w.setStorm(false);
		setupScoreboard(mapId);

		// Prepare players
		for (Player p : getPlayerInArena(mapId)) {
			p.setGameMode(GameMode.ADVENTURE);
			p.setHealth(20);
			p.setFoodLevel(20);
			p.setLevel(0);
			p.removePotionEffect(PotionEffectType.SPEED);
			p.removePotionEffect(PotionEffectType.JUMP);
			preparePlayers(p, false);
		}

		// Sound
		for (Player p : getPlayerInArena(mapId)) {
			p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 3, 1);
		}

		// Balance teams
		int inRed = 0;
		int inBlue = 0;
		for (Player p : getPlayerInArena(mapId)) {
			if (playerInTeamRed(p)) {
				inRed++;
			} else if (playerInTeamBlue(p)) {
				inBlue++;
			}
		}

		if (getPlayerInArena(mapId).size() % 2 == 0) {
			if (inRed != inBlue) {
				int s = 0;
				for (Player p : getPlayerInArena(mapId)) {
					if (s % 2 == 0) {
						playersTeam.put(p.getName(), "red");
					} else if (s % 2 == 1) {
						playersTeam.put(p.getName(), "blue");
					}
					s++;
				}
			}
		} else if (getPlayerInArena(mapId).size() % 2 == 1) {
			if (inRed + 1 != inBlue || inBlue + 1 != inRed) {
				int s = 0;
				for (Player p : getPlayerInArena(mapId)) {
					if (s % 2 == 0) {
						playersTeam.put(p.getName(), "red");
					} else if (s % 2 == 1) {
						playersTeam.put(p.getName(), "blue");
					}
					s++;
				}
			}
		}

		// Count team-members
		inRed = 0;
		inBlue = 0;
		for (Player p : getPlayerInArena(mapId)) {
			if (playerInTeamRed(p)) {
				inRed++;
			} else if (playerInTeamBlue(p)) {
				inBlue++;
			}
		}

		// Show player's team
		for (Player p : getPlayerInArena(mapId)) {
			if (playerInTeamRed(p)) {
				p.sendMessage(Texts.PRE_TEXT + Texts.GAME_JOINED_TEAM_RED);
			} else if (playerInTeamBlue(p)) {
				p.sendMessage(Texts.PRE_TEXT + Texts.GAME_JOINED_TEAM_BLUE);
			}
			p.sendMessage(Texts.PRE_TEXT + Texts.GAME_TEAM_DISTRIBUTION.replace("%inRed", "" + inRed).replace("%inBlue", "" + inBlue));
		}

		// Set score to 0 and start
		blueScore.put(mapId, 0);
		redScore.put(mapId, 0);
		playerOnKillstreak.put(mapId, "");
		numberOfKillstreak.put(mapId, 0);

		gameStarted.put(mapId, true);
	}

	/**
	 * A game will stop. Remove weapons and start a firework.
	 *
	 * @param mapId Which map / arena will stop
	 */
	public void stopGame(final int mapId) {
		if (!getGameStarted(mapId)) return;

		// Send a message with the score
		for (Player p : getPlayerInArena(mapId)) {
			p.sendMessage("");
			p.sendMessage(Texts.GAME_SCORE_HEAD);
			p.sendMessage(Texts.GAME_SCORE_RED.replace("%score", "" + redScore.get(mapId)));
			p.sendMessage(Texts.GAME_SCORE_BLUE.replace("%score", "" + blueScore.get(mapId)));

			// Show a firework
			Firework fw = (Firework) p.getWorld().spawnEntity(p.getLocation(), EntityType.FIREWORK);
			FireworkMeta fwm = fw.getFireworkMeta();

			Color c = Color.GREEN;
			if (blueScore.get(mapId) > redScore.get(mapId)) {
				c = Color.BLUE;
			} else if (blueScore.get(mapId) < redScore.get(mapId)) {
				c = Color.RED;
			}

			FireworkEffect effect = FireworkEffect.builder().flicker(true).withColor(c).with(Type.STAR).trail(true).build();
			fwm.addEffect(effect);
			fwm.setPower(0);
			fw.setFireworkMeta(fwm);

			// Clear the inventory (delayed, because a player might get a grenade right when the game ends)
			getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
				for (Player p1 : getPlayerInArena(mapId)) {
					p1.getInventory().clear();
				}
			}, 2);
		}

		// Send the players back to the lobby
		getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
			for (Player p : getPlayerInArena(mapId)) {
				p.setGameMode(GameMode.ADVENTURE);
				p.getInventory().clear();
				p.getInventory().setHelmet(new ItemStack(Material.AIR));
				p.setExp(0);
				p.setLevel(0);
				p.teleport(dtaSpawn());
				p.removePotionEffect(PotionEffectType.SPEED);
				p.removePotionEffect(PotionEffectType.JUMP);
				p.removePotionEffect(PotionEffectType.CONFUSION);

				p.setScoreboard(manager.getNewScoreboard());
				playersList.remove(p);
				playersTeam.remove(p.getName());
				playersMap.remove(p.getName());
				reloadTime.remove(p.getName());
				canDie.remove(p.getName());
			}
			objective[mapId].unregister();
			gameStarted.put(mapId, false);
		}, 10 * 20L);
	}

	/**
	 * A player joined the arena. Start the countdown (if possible) and put him/her in a team
	 *
	 * @param p     Player which joined
	 * @param mapId Which map / arena he/she joined
	 */
	public void playerJoinedArena(final Player p, final int mapId) {
		if (playersList.contains(p)) {
			// Player already in game
			p.sendMessage(Texts.PRE_TEXT + Texts.GAME_ALREADY_INGAME);
			return;
		}
		if (getPlayerInArena(mapId).size() > maxPlayers - 1 && !p.hasPermission(PERMISSION_PREMIUM)) {
			// Arena full
			p.sendMessage(Texts.PRE_TEXT + Texts.GAME_ARENA_FULL);
			return;
		} else if (getPlayerInArena(mapId).size() > maxPlayers - 1 && p.hasPermission(PERMISSION_PREMIUM)) {
			// Arena full, but player is premium
			if (!kickRandomPlayer(mapId, 0)) {
				p.sendMessage(Texts.PRE_TEXT + Texts.GAME_ARENA_NOKICK);
				return;
			}
		}

		// Prepare player
		playersList.add(p);
		playersMap.put(p.getName(), mapId);
		reloadTime.put(p.getName(), System.currentTimeMillis() - 3000);
		canDie.put(p.getName(), true);
		p.removePotionEffect(PotionEffectType.SPEED);
		p.removePotionEffect(PotionEffectType.JUMP);
		p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 999999, 0));
		p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 2));
		p.getInventory().clear();
		p.setGameMode(GameMode.ADVENTURE);
		p.teleport(randomSpawn(mapId));

		// Put player in a team
		int c = getPlayerInArena(mapId).size();
		if (c % 2 == 0) {
			playersTeam.put(p.getName(), "red");
		} else if (c % 2 == 1) {
			playersTeam.put(p.getName(), "blue");
		}

		// Announce the new player
		for (Player player : getPlayerInArena(mapId)) {
			player.sendMessage(Texts.PRE_TEXT + p.getDisplayName() + Texts.GAME_PLAYER_JOINED);
		}

		// Not enough players to start the countdown
		if (getPlayerInArena(mapId).size() < 2) {
			p.sendMessage(Texts.PRE_TEXT + Texts.GAME_NOT_ENOUGH_PLAYERS);
			return;
		}

		if (countdown.get(mapId) != -1) {
			return;
		}

		countdown.put(mapId, 60);
	}

	/**
	 * Setup the scoreboard for an specified map / arena
	 *
	 * @param map Which map / arena shall use this scoreboard
	 */
	private void setupScoreboard(int map) {
		objective[map] = board.registerNewObjective("kills", "dummy");
		objective[map].setDisplaySlot(DisplaySlot.SIDEBAR);
		objective[map].setDisplayName(Texts.GAME_SCORE);

		scoreRed[map] = objective[map].getScore(Bukkit.getOfflinePlayer(ChatColor.RED + Texts.GAME_TEAM_RED));
		scoreBlue[map] = objective[map].getScore(Bukkit.getOfflinePlayer(ChatColor.BLUE + Texts.GAME_TEAM_BLUE));
		scoreRed[map].setScore(0);
		scoreBlue[map].setScore(0);

		for (Player p : getPlayerInArena(map)) {
			p.setScoreboard(board);
		}
	}

	/**
	 * Load data from the config-file
	 */
	public void loadConfig() {
		saveDefaultConfig();
		reloadConfig();
		maxScore = getConfig().getInt("maxScore");
		maxPlayers = getConfig().getInt("maxPlayers");
		gameWorldPrefix = getConfig().getString("gameWorldPrefix");
		funForOPs = getConfig().getBoolean("funForOPs");

		updateSigns.clear();
		arenaConfig.clear();
		for (int i = 1; i < 100; i++) {
			List<Double> sign = getConfig().getDoubleList("maps." + i + ".sign");
			if (!sign.isEmpty()) {
				World w = Bukkit.getWorld(getConfig().getString("maps." + i + ".signWorld"));
				updateSigns.put(i, w.getBlockAt(new Location(w, sign.get(0), sign.get(1), sign.get(2))));
			}

			arenaConfig.put(i, getConfig().getBoolean("maps." + i + ".enabled"));
		}
	}

	/**
	 * A player died or the game started: He/She needs stuff and a teleport
	 *
	 * @param p       Player which will be prepared
	 * @param respawn If the player was already in-game before or not
	 */
	private void preparePlayers(final Player p, final boolean respawn) {
		getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
			p.teleport(randomSpawn(getPlayersArena(p)));
			p.getInventory().clear();
			p.getInventory().setHeldItemSlot(0);

			if (!getGameStarted(getPlayersArena(p))) {
				return;
			}

			if (blueScore.get(getPlayersArena(p)) == maxScore || redScore.get(getPlayersArena(p)) == maxScore) {
				return;
			}

			// Give player the shotgun and a grenade (if first spawn)
			List<String> lsShotgun = new ArrayList<String>();
			lsShotgun.add(Texts.GAME_HOWTO_SHOTGUN);
			if (p.hasPermission(PERMISSION_PREMIUM)) {
				lsShotgun.add(Texts.GAME_RELOAD_SHOTGUN + "0.8s");
			} else {
				lsShotgun.add(Texts.GAME_RELOAD_SHOTGUN + "1s");
			}
			p.getInventory().addItem(setName(new ItemStack(Material.BLAZE_ROD, 1), "§2Shotgun", lsShotgun, 1));
			if (!respawn) {
				givePlayerAmmo(p);
			}
			p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 999999, 0));
			p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 2));
			p.getInventory().setHeldItemSlot(0);

			// Give the player a funky wool-helmet
			if (playerInTeamRed(p)) {
				p.getInventory().setHelmet(new ItemStack(Material.WOOL, 1, (byte) 14));
			} else if (playerInTeamBlue(p)) {
				p.getInventory().setHelmet(new ItemStack(Material.WOOL, 1, (byte) 11));
			}

			// Spawn-protection
			if (respawn) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 80, 10));
			}
		});
	}

	/**
	 * Grenades! Grenades for everyone!
	 *
	 * @param p Player which gets a grenade
	 */
	private void givePlayerAmmo(Player p) {
		List<String> lsGrenade = new ArrayList<String>();
		lsGrenade.add(Texts.GAME_HOWTO_GRENADE);
		lsGrenade.add(Texts.GAME_RELOAD_GRENADE);
		p.getInventory().addItem(setName(new ItemStack(Material.EGG, 1), "§2Grenade", lsGrenade, 1));
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		// Player uses the blaze-rod (shotgun)
		if (getPlayerInGame(e.getPlayer()) && e.getPlayer().getItemInHand().getType() == Material.BLAZE_ROD && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && getGameStarted(getPlayersArena(e.getPlayer()))) {
			final Player p = e.getPlayer();


			if (p.isOp() && funForOPs) { // OP? Have fun :)
				Vector vec = p.getLocation().getDirection().multiply(5);
				Snowball ball = p.getWorld().spawn(p.getEyeLocation(), Snowball.class);
				ball.setShooter(p);
				ball.setVelocity(vec);

				p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_HURT, 1, 1);
				reloadTime.put(p.getName(), System.currentTimeMillis());
			} else if (p.hasPermission(PERMISSION_PREMIUM)) { // Premium? Shoot more often!
				if (System.currentTimeMillis() - (reloadTime.get(e.getPlayer().getName())) >= 800) {
					Vector vec = p.getLocation().getDirection().multiply(4);
					Snowball ball = p.getWorld().spawn(p.getEyeLocation(), Snowball.class);
					ball.setShooter(p);
					ball.setVelocity(vec);

					p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_HURT, 1, 1);
					reloadTime.put(p.getName(), System.currentTimeMillis());
				} else {
					e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, (float) 0.2, (float) 1.9);
				}
			} else { // Normal player? Shoot every second!
				if (System.currentTimeMillis() - (reloadTime.get(e.getPlayer().getName())) >= 1000) {
					Vector vec = p.getLocation().getDirection().multiply(4);
					Snowball ball = p.getWorld().spawn(p.getEyeLocation(), Snowball.class);
					ball.setShooter(p);
					ball.setVelocity(vec);

					p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_HURT, 1, 1);
					reloadTime.put(p.getName(), System.currentTimeMillis());
				} else {
					e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, (float) 0.2, (float) 1.9);
				}
			}
		}

		// Its an egg! Throw the grenade
		if (getPlayerInGame(e.getPlayer()) && e.getPlayer().getItemInHand().getType() == Material.EGG && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && getGameStarted(getPlayersArena(e.getPlayer()))) {
			if (e.getPlayer().getItemInHand().hasItemMeta() && e.getPlayer().getItemInHand().getItemMeta().getDisplayName().equals("§2Grenade")) {
				Player p = e.getPlayer();
				Vector vec = p.getLocation().getDirection().multiply(2.8);

				TNTPrimed tnt = p.getWorld().spawn(p.getEyeLocation(), TNTPrimed.class);
				tnt.setVelocity(vec);
				tnt.setFuseTicks(33);
				tnt.setMetadata("shooter", new FixedMetadataValue(this, p.getName()));

				List<String> lsGrenade = new ArrayList<String>();
				lsGrenade.add(Texts.GAME_HOWTO_GRENADE);
				lsGrenade.add(Texts.GAME_RELOAD_GRENADE);
				p.getInventory().removeItem(setName(new ItemStack(Material.EGG, 1), "§2Grenade", lsGrenade, 1));

				e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1, 1);
				e.setCancelled(true);
			}
		}

		// Do not allow anything else
		if (getPlayerInGame(e.getPlayer())) {
			e.setCancelled(true);
			return;
		}

		// Player clicked a block. If this is a join-sign, join!
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null && isSign(e.getClickedBlock())) {
			Sign theSign = (Sign) e.getClickedBlock().getState();
			if (theSign.getLine(0).equals(ChatColor.GREEN + "[DoomThemAll]")) {
				for (int i = 1; i < 100; i++) {
					if (theSign.getLine(3).startsWith(ChatColor.BLUE + "A" + i + ": ")) {
						e.getPlayer().performCommand("dta join " + i);
					}
				}

				// Teleport player to the lobby
				if (theSign.getLine(2).equals(ChatColor.DARK_GREEN + "--> Lobby <--")) {
					if (getPlayerInGame(e.getPlayer())) {
						playersList.remove(e.getPlayer());
						playersTeam.remove(e.getPlayer().getName());
						playersMap.remove(e.getPlayer().getName());
					}
					e.getPlayer().performCommand("dta lobby");

					for (Player player : getPlayerInArena(getPlayersArena(e.getPlayer()))) {
						player.sendMessage(Texts.PRE_TEXT + "§6" + e.getPlayer().getDisplayName() + Texts.GAME_PLAYER_LEFT);
					}
				}
			}
		}
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent e) {
		// No BOOOOM in DTA-worlds!
		if (e.getLocation().getWorld().getName().startsWith(gameWorldPrefix)) e.blockList().clear();
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			// No fall-damage, suicide or drowning while in-game!
			if ((e.getCause().equals(DamageCause.FALL) || e.getCause().equals(DamageCause.SUICIDE) || e.getCause().equals(DamageCause.DROWNING)) && getPlayerInGame((Player) e.getEntity())) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onHit(EntityDamageByEntityEvent e) {
		// Player has spawn-protection
		if (e.getEntity() instanceof Player && ((Player) e.getEntity()).hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE) && getPlayerInGame((Player) e.getEntity())) {
			e.setCancelled(true);
			return;
		}

		if (e.getDamager() instanceof Snowball && e.getEntity() instanceof Player && getPlayerInGame((Player) e.getEntity())) {
			// A player shot you.
			final Player shooter = (Player) ((Snowball) e.getDamager()).getShooter();
			final Player target = (Player) e.getEntity();
			e.setCancelled(true);

			// Announce this kill and give shooter a grenade
			if (canDie.get(target.getName()) && (playerInTeamRed(shooter) && playerInTeamBlue(target)) || (playerInTeamBlue(shooter) && playerInTeamRed(target))) {
				canDie.put(target.getName(), false);
				addKill(shooter, target, "Shotgun");
				preparePlayers(target, true);
				target.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 5, 1);

				getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
					if (getPlayerInGame(shooter)) {
						givePlayerAmmo(shooter);
					}
				});

				// Do not kill player too often
				getServer().getScheduler().scheduleSyncDelayedTask(this, () -> canDie.put(target.getName(), true), 20L);
			}
		} else if (e.getDamager() instanceof TNTPrimed && e.getEntity() instanceof Player && getPlayerInGame((Player) e.getEntity())) {
			// Boom! You were hit by a grenade!
			TNTPrimed tnt = (TNTPrimed) e.getDamager();
			final Player shooter = getServer().getPlayer(tnt.getMetadata("shooter").get(0).asString());
			final Player target = (Player) e.getEntity();
			e.setCancelled(true);

			// Give killer an other grenade
			if (canDie.get(target.getName()) && ((playerInTeamRed(shooter) && playerInTeamBlue(target)) || (playerInTeamBlue(shooter) && playerInTeamRed(target)))) {
				canDie.put(target.getName(), false);
				addKill(shooter, target, "Grenade");
				preparePlayers(target, true);
				target.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 5, 1);

				getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
					if (getPlayerInGame(shooter)) {
						givePlayerAmmo(shooter);
					}
				});

				// Do not kill player too often
				getServer().getScheduler().scheduleSyncDelayedTask(this, () -> canDie.put(target.getName(), true), 20L);
			}
		} else if (e.getDamager() instanceof Player && e.getEntity() instanceof Player && getPlayerInGame((Player) e.getEntity())) {
			// Stay clean!
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent e) {
		// Goodbye!
		Player p = e.getPlayer();
		playerLeave(p);
	}

	/**
	 * Remove player from arena and teleport him/her back to the lobby
	 *
	 * @param p Which player will be removed
	 */
	public void playerLeave(Player p) {
		int oldArena = getPlayersArena(p);
		if (oldArena == 0) return;

		// Announce event
		for (Player player : getPlayerInArena(getPlayersArena(p))) {
			player.sendMessage(Texts.PRE_TEXT + p.getDisplayName() + Texts.GAME_PLAYER_LEFT);
		}

		// Teleport player back and remove his/her stuff
		p.teleport(dtaSpawn());
		playersList.remove(p);
		playersMap.remove(p.getName());
		playersTeam.remove(p.getName());
		canDie.remove(p.getName());
		reloadTime.remove(p.getName());
		p.getInventory().clear();
		p.getInventory().setHelmet(new ItemStack(Material.AIR));
		p.setExp(0);
		p.setLevel(0);

		for (PotionEffect effect : p.getActivePotionEffects()) {
			p.removePotionEffect(effect.getType());
		}

		p.setScoreboard(manager.getNewScoreboard());

		// Does the game need to end?
		if (getPlayerInArena(oldArena).size() < 2 && getGameStarted(oldArena)) {
			for (Player player : getPlayerInArena(oldArena)) {
				player.sendMessage(Texts.PRE_TEXT + Texts.GAME_STOPPED);
			}
			stopGame(oldArena);
			return;
		}

		// Do the teams need to get balanced again?
		if (getPlayerInArena(oldArena).size() >= 2 && getGameStarted(oldArena)) {
			boolean teamsChanged = false;

			int inRed = 0;
			int inBlue = 0;
			for (Player player : getPlayerInArena(oldArena)) {
				if (playerInTeamRed(player)) {
					inRed++;
				} else if (playerInTeamBlue(player)) {
					inBlue++;
				}
			}

			if (getPlayerInArena(oldArena).size() % 2 == 0) {
				if (inRed != inBlue) {
					for (Player player : getPlayerInArena(oldArena)) {
						player.sendMessage(Texts.PRE_TEXT + Texts.GAME_BALANCING_TEAMS);
					}

					int s = 0;
					for (Player player : getPlayerInArena(oldArena)) {
						if (s % 2 == 0) {
							playersTeam.put(player.getName(), "red");
						} else if (s % 2 == 1) {
							playersTeam.put(player.getName(), "blue");
						}
						s++;
					}
					teamsChanged = true;
				}
			} else if (getPlayerInArena(oldArena).size() % 2 == 1) {
				if (inRed + 1 != inBlue || inBlue + 1 != inRed) {
					for (Player player : getPlayerInArena(oldArena)) {
						player.sendMessage(Texts.PRE_TEXT + Texts.GAME_BALANCING_TEAMS);
					}

					int s = 0;
					for (Player player : getPlayerInArena(oldArena)) {
						if (s % 2 == 0) {
							playersTeam.put(player.getName(), "red");
						} else if (s % 2 == 1) {
							playersTeam.put(player.getName(), "blue");
						}
						s++;
					}
					teamsChanged = true;
				}
			}

			// Announce new teams
			if (teamsChanged) {
				inRed = 0;
				inBlue = 0;
				for (Player player : getPlayerInArena(oldArena)) {
					if (playerInTeamRed(player)) {
						inRed++;
					} else if (playerInTeamBlue(player)) {
						inBlue++;
					}
				}

				for (Player player : getPlayerInArena(oldArena)) {
					player.getInventory().setHelmet(new ItemStack(Material.AIR));

					if (playerInTeamRed(player)) {
						player.getInventory().setHelmet(new ItemStack(Material.WOOL, 1, (byte) 14));
						player.sendMessage(Texts.PRE_TEXT + Texts.GAME_JOINED_TEAM_RED);
					} else if (playerInTeamBlue(player)) {
						player.getInventory().setHelmet(new ItemStack(Material.WOOL, 1, (byte) 11));
						player.sendMessage(Texts.PRE_TEXT + Texts.GAME_JOINED_TEAM_BLUE);
					}
					player.sendMessage(Texts.PRE_TEXT + Texts.GAME_TEAM_DISTRIBUTION.replace("%inRed", "" + inRed).replace("%inBlue", "" + inBlue));
				}
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		// No block-breaking in-game!
		if (getPlayerInGame(e.getPlayer())) e.setCancelled(true);
	}

	@EventHandler
	public void playerDropItem(PlayerDropItemEvent e) {
		// Why would you want to drop your weapons?!
		if (getPlayerInGame(e.getPlayer()) && getGameStarted(getPlayersArena(e.getPlayer()))) e.setCancelled(true);
	}

	/**
	 * Manages the countdowns in every arena
	 */
	private void countdown() {
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			for (int i = 1; i < 100; i++) {
				if (countdown.get(i) != -1) {
					if (countdown.get(i) != 0) {
						if (getPlayerInArena(i).size() < 2) {
							for (Player players : getPlayerInArena(i)) {
								players.sendMessage(Texts.PRE_TEXT + Texts.GAME_START_STOPPED);
								players.setLevel(0);
							}
							countdown.put(i, -1);
							return;
						}

						if (countdown.get(i) < 6) {
							for (Player p : getPlayerInArena(i)) {
								p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 3, 1);
							}
						}
						for (Player p : getPlayerInArena(i)) {
							p.setLevel(countdown.get(i));
						}
						countdown.put(i, countdown.get(i) - 1);
					} else {
						countdown.put(i, countdown.get(i) - 1);
						if (getPlayerInArena(i).size() < 2) {
							for (Player players : getPlayerInArena(i)) {
								players.sendMessage(Texts.PRE_TEXT + Texts.GAME_START_STOPPED);
								players.setLevel(0);
							}
							countdown.put(i, -1);
							return;
						}
						startGame(i, true);
					}
				}
			}
		}, 0, 20L);
	}

	/**
	 * Shows the time until player is able to shoot again in the exp-bar
	 */
	private void reloadTime() {
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			for (Player p : playersList) {
				long time = System.currentTimeMillis() - reloadTime.get(p.getName());

				if (p.isOp() && funForOPs) {
					p.setExp((float) 0.0);
				} else if (p.hasPermission(PERMISSION_PREMIUM)) {
					if (time < 100) {
						p.setExp((float) 0.8);
					} else if (time < 200) {
						p.setExp((float) 0.7);
					} else if (time < 300) {
						p.setExp((float) 0.6);
					} else if (time < 400) {
						p.setExp((float) 0.5);
					} else if (time < 500) {
						p.setExp((float) 0.4);
					} else if (time < 600) {
						p.setExp((float) 0.3);
					} else if (time < 700) {
						p.setExp((float) 0.2);
					} else if (time < 800) {
						p.setExp((float) 0.1);
					} else {
						p.setExp((float) 0.0);
					}
				} else {
					if (time < 100) {
						p.setExp((float) 0.9);
					} else if (time < 200) {
						p.setExp((float) 0.9);
					} else if (time < 300) {
						p.setExp((float) 0.8);
					} else if (time < 400) {
						p.setExp((float) 0.7);
					} else if (time < 500) {
						p.setExp((float) 0.6);
					} else if (time < 600) {
						p.setExp((float) 0.5);
					} else if (time < 700) {
						p.setExp((float) 0.4);
					} else if (time < 800) {
						p.setExp((float) 0.3);
					} else if (time < 900) {
						p.setExp((float) 0.2);
					} else if (time < 1000) {
						p.setExp((float) 0.1);
					} else {
						p.setExp((float) 0.0);
					}
				}
			}
		}, 0, 1);
	}

	/**
	 * Updates the signs every half second and shows game-status and player-count
	 */
	private void updateSigns() {
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			for (Map.Entry<Integer, Block> b : updateSigns.entrySet()) {
				if (isSign(b.getValue())) {
					// Load chunk, prevent NullPointerExceptions
					b.getValue().getChunk().load();

					Sign mySign;
					try {
						mySign = (Sign) b.getValue().getState();
					} catch (NullPointerException e) {
						Bukkit.getLogger().warning("Signs' chunk not loaded! Will update later...");
						continue;
					}

					if (!arenaConfig.get(b.getKey())) {
						mySign.setLine(1, ChatColor.RED + "Disabled");
						mySign.setLine(2, ChatColor.RED + "0 / " + maxPlayers);
						mySign.update();
					} else {
						mySign.setLine(1, getArenaStatus(b.getKey()));
						mySign.setLine(2, ChatColor.RED + "" + getPlayerInArena(b.getKey()).size() + " / " + maxPlayers);
						mySign.update();
					}
				}
			}
		}, 0, 10L);
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		// A DoomThemAll-sign? Change and save it!
		if (event.getLine(0).trim().equalsIgnoreCase("DoomThemAll")) {
			for (int i = 1; i < 100; i++) {
				if (event.getLine(2).equalsIgnoreCase("dta join " + i) && event.getPlayer().isOp()) {
					event.setLine(0, ChatColor.GREEN + "[DoomThemAll]");
					event.setLine(1, "");
					event.setLine(2, ChatColor.RED + "0  / " + maxPlayers);
					event.setLine(3, ChatColor.BLUE + "A" + i + ": " + event.getLine(3));
					List<Double> listPosition = Arrays.asList(event.getBlock().getLocation().getX(), event.getBlock().getLocation().getY(), event.getBlock().getLocation().getZ());
					getConfig().set("maps." + i + ".signWorld", event.getPlayer().getWorld().getName());
					getConfig().set("maps." + i + ".sign", listPosition);
					saveConfig();
					loadConfig();
				}
			}
		}

		// A DoomThemAll-lobby-sign? Change and save it!
		if (event.getLine(0).equalsIgnoreCase("DoomThemAll") && event.getLine(2).equalsIgnoreCase("dta lobby") && event.getPlayer().isOp()) {
			event.setLine(0, ChatColor.GREEN + "[DoomThemAll]");
			event.setLine(2, ChatColor.DARK_GREEN + "--> Lobby <--");
		}
	}

	/**
	 * A player killed an other player
	 *
	 * @param p      Player which shot
	 * @param target Player which died
	 * @param weapon Used weapon
	 */
	private void addKill(Player p, Player target, String weapon) {
		String message = "unknown error!";

		// Add 1 kill for the team and announce it
		if (playerInTeamRed(p)) {
			message = Texts.PRE_TEXT + "§c" + p.getDisplayName() + " §7killed §9" + target.getDisplayName() + " §7(" + weapon + ")";
			int arena = getPlayersArena(p);
			int score = redScore.get(arena) + 1;
			redScore.put(arena, score);
			scoreRed[arena].setScore(score);
		} else if (playerInTeamBlue(p)) {
			message = Texts.PRE_TEXT + "§9" + p.getDisplayName() + " §7killed §c" + target.getDisplayName() + " §7(" + weapon + ")";
			int arena = getPlayersArena(p);
			int score = blueScore.get(arena) + 1;
			blueScore.put(arena, score);
			scoreBlue[arena].setScore(score);
		}

		for (Player player : getPlayerInArena(getPlayersArena(p))) {
			player.sendMessage(message);
		}

		p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 3, 1);

		// Check for a killstreak
		if (playerOnKillstreak.get(getPlayersArena(p)).equals(p.getDisplayName())) {
			numberOfKillstreak.put(getPlayersArena(p), numberOfKillstreak.get(getPlayersArena(p)) + 1);

			if (playerInTeamRed(p)) {
				message = Texts.PRE_TEXT + "§c" + p.getDisplayName();
			} else {
				message = Texts.PRE_TEXT + "§9" + p.getDisplayName();
			}

			int kills = numberOfKillstreak.get(getPlayersArena(p));
			if (kills == 2) {
				message += "§7: §6Double Kill!";
			} else if (kills == 3) {
				message += "§7: §6Triple Kill!";
			} else if (kills == 4) {
				message += "§7: §6QUADRA KILL!";
			} else if (kills == 5) {
				message += "§7: §6PENTA KILL!!!";
			} else if (kills > 5) {
				message += "§7: §6CALL HIM \"THE SLAUGHTERER\"! (" + kills + " Kills)!";
			}

			for (Player player : getPlayerInArena(getPlayersArena(p))) {
				player.sendMessage(message);
			}
		} else {
			playerOnKillstreak.put(getPlayersArena(p), p.getDisplayName());
			numberOfKillstreak.put(getPlayersArena(p), 1);
		}

		if (blueScore.get(getPlayersArena(p)) == maxScore || redScore.get(getPlayersArena(p)) == maxScore) {
			stopGame(getPlayersArena(p));
		}
	}

	/**
	 * Create an ItemStack with more information
	 *
	 * @param is     New ItemStack
	 * @param name   Name of item
	 * @param lore   Sub-titles of item
	 * @param amount Amount of items
	 * @return Custom ItemStack
	 */
	private ItemStack setName(ItemStack is, String name, List<String> lore, int amount) {
		ItemMeta im = is.getItemMeta();
		if (name != null)
			im.setDisplayName(name);
		if (lore != null)
			im.setLore(lore);
		is.setItemMeta(im);
		is.setAmount(amount);
		return is;
	}

	/**
	 * Is this block a sign?
	 *
	 * @param theBlock Block to check
	 * @return Is this block a sign
	 */
	private boolean isSign(Block theBlock) {
		return theBlock.getType() == Material.SIGN || theBlock.getType() == Material.SIGN_POST || theBlock.getType() == Material.WALL_SIGN;
	}

	/**
	 * Kicks a random, non-premium player from the game
	 *
	 * @param mapId Which map / arena has to kick a player
	 * @param tries How often this will be tried (again)
	 * @return If a player was successfully kicked or not
	 */
	private boolean kickRandomPlayer(int mapId, int tries) {
		if (tries > 3) {
			return false;
		}
		List<Player> myPlayers = getPlayerInArena(mapId);
		Random r = new Random();
		int random = r.nextInt(maxPlayers);
		if (!myPlayers.get(random).hasPermission(PERMISSION_PREMIUM)) {
			myPlayers.get(random).performCommand("dta leave");
			myPlayers.get(random).sendMessage(Texts.PRE_TEXT + Texts.GAME_KICKED_FOR_PREMIUM);
			myPlayers.get(random).sendMessage(Texts.PRE_TEXT + Texts.GAME_BUY_PREMIUM);
			return true;
		} else {
			kickRandomPlayer(mapId, tries + 1);
			return false;
		}
	}

	/**
	 * Get a random spawn of an arena
	 *
	 * @param mapId Which map / arena
	 * @return A random spawn
	 */
	private Location randomSpawn(int mapId) {
		Random r = new Random();
		int random = r.nextInt(8) + 1;
		List<Double> spawns = this.getConfig().getDoubleList("maps." + mapId + "." + random);
		return new Location(getServer().getWorld(gameWorldPrefix + mapId), spawns.get(0), spawns.get(1), spawns.get(2));
	}

	/**
	 * Get the lobby-location
	 *
	 * @return Lobby-location
	 */
	public Location dtaSpawn() {
		List<Double> spawn = getConfig().getDoubleList("lobbySpawn");
		return new Location(Bukkit.getWorld(getConfig().getString("lobbyWorld")), spawn.get(0), spawn.get(1), spawn.get(2));
	}

	/**
	 * Did the game start?
	 *
	 * @param mapId Which map / arena will be checked
	 * @return If the game already started or not
	 */
	public boolean getGameStarted(int mapId) {
		return gameStarted.get(mapId);
	}

	/**
	 * Get a list of every player in an arena
	 *
	 * @param map Which map / arena will be checked
	 * @return A list of every player in the arena
	 */
	public List<Player> getPlayerInArena(int map) {
		List<Player> myPlayers = new ArrayList<Player>();
		for (Player p : playersList) {
			if (playersMap.get(p.getName()).equals(map)) {
				myPlayers.add(p);
			}
		}
		return myPlayers;
	}

	/**
	 * Get the arena of a specific player
	 *
	 * @param p Which player will be checked
	 * @return Arena of the player or 0 if there is none
	 */
	public int getPlayersArena(Player p) {
		if (playersMap.containsKey(p.getName())) {
			return playersMap.get(p.getName());
		}
		return 0;
	}

	/**
	 * Is the player currently in-game?
	 *
	 * @param p Which player will be checked
	 * @return If the player is currently in-game
	 */
	public boolean getPlayerInGame(Player p) {
		return playersList.contains(p);
	}

	/**
	 * Is the player in the red team?
	 *
	 * @param p Which player will be checked
	 * @return If the player is in the red team
	 */
	private boolean playerInTeamRed(Player p) {
		return playersTeam.containsKey(p.getName()) && playersTeam.get(p.getName()).equals("red");
	}

	/**
	 * Is the player in the blue team?
	 *
	 * @param p Which player will be checked
	 * @return If the player is in the blue team
	 */
	private boolean playerInTeamBlue(Player p) {
		return playersTeam.containsKey(p.getName()) && playersTeam.get(p.getName()).equals("blue");
	}

	/**
	 * Get the current game-status of an arena
	 *
	 * @param mapId Which map / arena will be checked
	 * @return State of game
	 */
	private String getArenaStatus(int mapId) {
		if (getGameStarted(mapId)) {
			return ChatColor.RED + "In-Game";
		}
		if (countdown.get(mapId) == -1) {
			return ChatColor.GREEN + "Waiting";
		}
		if (countdown.get(mapId) != -1) {
			return ChatColor.DARK_GREEN + "Countdown";
		}
		return "unknown";
	}
}
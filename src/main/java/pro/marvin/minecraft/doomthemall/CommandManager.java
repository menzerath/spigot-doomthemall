package pro.marvin.minecraft.doomthemall;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class CommandManager implements CommandExecutor {
	private DoomThemAll plugin;

	public CommandManager(DoomThemAll dta) {
		plugin = dta;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("dta")) {
			// No further arguments
			if (args.length == 0) {
				// Send list of available commands for OPs and normal Players
				sender.sendMessage(Texts.COMMANDS_HEAD);
				sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_COMMANDS);
				sender.sendMessage("§6     - dta join [arena]");
				sender.sendMessage("§6     - dta leave");
				sender.sendMessage("§6     - dta start");
				if (sender.isOp()) {
					sender.sendMessage("§6     - dta [start/stop] [arena]");
					sender.sendMessage("§6     - dta [enable/disable] [arena]");
					sender.sendMessage("§6     - dta setLobby");
					sender.sendMessage("§6     - dta setSpawn [arena] [spawn]");
					sender.sendMessage("§6     - dta setMaxScore [int > 0]");
					sender.sendMessage("§6     - dta setMaxPlayers [int > 1]");
					sender.sendMessage("§6     - dta funForOPs [true/false]");
					sender.sendMessage("§6     - dta reload");
				}
				sender.sendMessage(Texts.COPYRIGHT);
				return true;
			}

			// One additional argument
			if (args.length == 1) {
				// Teleport player to lobby if he/she is not in-game
				if (args[0].equalsIgnoreCase("lobby")) {
					Player player = (Player) sender;
					if (plugin.getPlayerInGame(player)) return true;
					player.teleport(plugin.dtaSpawn());
					return true;
				}

				// Player leaves arena
				if (args[0].equalsIgnoreCase("leave")) {
					Player player = (Player) sender;
					plugin.playerLeave(player);
					return true;
				}

				// Set lobby-spawn [OP only]
				if (args[0].equalsIgnoreCase("setLobby")) {
					if (sender.isOp()) {
						Player player = (Player) sender;
						List<Double> listPosition = Arrays.asList(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
						plugin.getConfig().set("lobbyWorld", player.getWorld().getName());
						plugin.getConfig().set("lobbySpawn", listPosition);
						plugin.saveConfig();
						plugin.loadConfig();
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_EXECUTED);
					} else {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_OP_ONLY);
					}

					return true;
				}

				// Start current arena [Premium / OP only]
				if (args[0].equalsIgnoreCase("start")) {
					if (sender.hasPermission(DoomThemAll.PERMISSION_PREMIUM)) {
						int map = plugin.getPlayersArena((Player) sender);
						if (map == 0) return true;

						if (plugin.getGameStarted(map)) {
							sender.sendMessage(Texts.PRE_TEXT + Texts.GAME_START_MULTIPLE);
							return true;
						}
						if (plugin.getPlayerInArena(map).size() < 2) {
							sender.sendMessage(Texts.PRE_TEXT + Texts.GAME_START_NO_PLAYERS);
							return true;
						}

						plugin.startGame(map, false);
					} else {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_VIP_ONLY);
					}
					return true;
				}

				// Reload config [OP only]
				if (args[0].equalsIgnoreCase("reload")) {
					if (!sender.isOp()) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_OP_ONLY);
						return true;
					}
					plugin.loadConfig();
					sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_EXECUTED);
					return true;
				}
			}

			// Two additional arguments
			if (args.length == 2) {
				// Player joins arena
				if (args[0].equalsIgnoreCase("join") && sender instanceof Player) {
					Player p = (Player) sender;

					int map;
					try {
						map = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_ARENA);
						return true;
					}

					if (map > 100 || map < 0 || !plugin.arenaConfig.get(map)) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INACTIVE_ARENA);
						return true;
					}

					if (plugin.getGameStarted(map)) {
						p.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_GAME_RUNNING);
						return true;
					}

					plugin.playerJoinedArena(p, map);
					return true;
				}

				// Start the specified arena [OP only]
				if (args[0].equalsIgnoreCase("start")) {
					if (!sender.isOp()) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_OP_ONLY);
						return true;
					}

					int map;
					try {
						map = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_ARENA);
						return true;
					}

					if (map > 100 || map < 0 || !plugin.arenaConfig.get(map)) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INACTIVE_ARENA);
						return true;
					}

					if (plugin.getGameStarted(map)) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.GAME_START_MULTIPLE);
						return true;
					}
					if (plugin.getPlayerInArena(map).size() < 2) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.GAME_START_NO_PLAYERS);
						return true;
					}

					plugin.startGame(map, false);
					return true;
				}

				// Stop the specified arena [OP only]
				if (args[0].equalsIgnoreCase("stop")) {
					if (!(sender instanceof Player)) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_PLAYER_ONLY);
						return true;
					}

					if (!sender.isOp()) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_OP_ONLY);
						return true;
					}

					int map;
					try {
						map = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_ARENA);
						return true;
					}

					if (map > 100 || map < 0 || !plugin.arenaConfig.get(map)) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INACTIVE_ARENA);
						return true;
					}

					if (!plugin.getGameStarted(map)) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.GAME_ALREADY_STOPPED);
						return true;
					}

					plugin.stopGame(map);
					return true;
				}

				// Enabled an arena [OP only]
				if (args[0].equalsIgnoreCase("enable")) {
					if (!sender.isOp()) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_OP_ONLY);
						return true;
					}

					int map;
					try {
						map = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_NUMBER);
						return true;
					}

					if (map < 1 || map > 100) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_NUMBER);
						return true;
					}

					plugin.getConfig().set("maps." + map + ".enabled", true);
					plugin.saveConfig();
					plugin.loadConfig();
					sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_EXECUTED);
					return true;
				}

				// Disables an arena [OP only]
				if (args[0].equalsIgnoreCase("disable")) {
					if (!sender.isOp()) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_OP_ONLY);
						return true;
					}

					int map;
					try {
						map = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_NUMBER);
						return true;
					}

					if (map < 1 || map > 100) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_NUMBER);
						return true;
					}

					plugin.getConfig().set("maps." + map + ".enabled", false);
					plugin.saveConfig();
					plugin.loadConfig();
					sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_EXECUTED);
					return true;
				}

				// Set maximum score [OP only]
				if (args[0].equalsIgnoreCase("setMaxScore")) {
					if (!sender.isOp()) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_OP_ONLY);
						return true;
					}

					int max;
					try {
						max = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_NUMBER);
						return true;
					}

					if (max < 1 || max > 100) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_NUMBER);
						return true;
					}

					plugin.getConfig().set("maxScore", max);
					plugin.saveConfig();
					DoomThemAll.maxScore = max;
					sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_EXECUTED);
					return true;
				}

				// Set maximum players [OP only]
				if (args[0].equalsIgnoreCase("setMaxPlayers")) {
					if (!sender.isOp()) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_OP_ONLY);
						return true;
					}

					int max;
					try {
						max = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_NUMBER);
						return true;
					}

					if (max < 2 || max > 100) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_NUMBER);
						return true;
					}

					plugin.getConfig().set("maxPlayers", max);
					plugin.saveConfig();
					DoomThemAll.maxPlayers = max;
					sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_EXECUTED);
					return true;
				}

				// Enable or Disable fun for OPs [OP only]
				if (args[0].equalsIgnoreCase("funForOPs")) {
					if (!sender.isOp()) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_OP_ONLY);
						return true;
					}

					if (args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
						boolean active = Boolean.parseBoolean(args[1]);
						DoomThemAll.funForOPs = active;
						plugin.getConfig().set("funForOPs", active);
						plugin.saveConfig();
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_EXECUTED);
					} else {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_INPUT);
					}
					return true;
				}

				return true;
			}

			// Three additional arguments
			if (args.length == 3) {
				// Set one spawn in a map [OP only]
				if (args[0].equalsIgnoreCase("setSpawn")) {
					if (!(sender instanceof Player)) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_PLAYER_ONLY);
						return true;
					}

					if (!sender.isOp()) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_OP_ONLY);
						return true;
					}

					int arg1; // Arena
					int arg2; // Spawn
					try {
						arg1 = Integer.parseInt(args[1]);
						arg2 = Integer.parseInt(args[2]);
					} catch (NumberFormatException e) {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_NUMBER);
						return true;
					}

					if (arg1 < 100 && arg1 > 0 && arg2 < 9 && arg2 > 0) {
						Player player = (Player) sender;
						List<Double> listPosition = Arrays.asList(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
						plugin.getConfig().set("maps." + arg1 + "." + arg2, listPosition);
						plugin.saveConfig();
						plugin.loadConfig();
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_EXECUTED);
						return true;
					} else {
						sender.sendMessage(Texts.PRE_TEXT + Texts.COMMANDS_INVALID_SPAWNS);
						return true;
					}
				}

			}
		}
		return false;
	}
}
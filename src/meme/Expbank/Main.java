package meme.Expbank;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

	boolean shouldCreate = false;

	public HashMap<String, Integer> playerID = new HashMap<String, Integer>();

	Configuration config;

	int[] expTab = new int[30];
	int[] steps = { 17, 34, 51, 68, 85, 102, 119, 136, 153, 170, 187, 204, 221, 238, 255, 272, 292, 315, 341, 370, 402, 437, 475, 516, 560, 607, 657, 710, 766, 825 };

	public Main() {

	}

	public void initializeExpTab() {
		System.arraycopy(steps, 0, expTab, 0, steps.length);
	}

	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		config = getConfig();
		loadIDS();
		initializeExpTab();
		getLogger().info("Expbank has been enabled!");
	}

	public void onDisable() {
		saveIDS();
		getLogger().info("Expbank has been disabled!");
	}

	public void saveIDS() {
		config.set("playerids", null);
		for (Map.Entry<String, Integer> entry : playerID.entrySet()) {
			config.set("playerids." + entry.getKey(), entry.getValue());
		}
		saveConfig();
	}

	public void loadIDS() {
		if (config.getConfigurationSection("playerids") == null)
			return;
		for (String key : config.getConfigurationSection("playerids").getKeys(false)) {
			playerID.put(key, config.getInt("playerids." + key));
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}

		if (player != null) {
			if (cmd.getName().equalsIgnoreCase("checkid")) {
				player.sendMessage("Your ID is: " + playerID.get(player.getName()));
				return true;
			} else if (cmd.getName().equalsIgnoreCase("gimme")) {
				player.setExp(player.getExp() + Float.parseFloat(args[0]));
				return true;
			} else if (cmd.getName().equalsIgnoreCase("gimmy")) {
				player.giveExp(Integer.parseInt(args[0]));
				return true;
			} else if (cmd.getName().equalsIgnoreCase("checkexp")) {
				player.sendMessage("" + player.getExp());
				return true;
			}
		}
		return false;
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		Player player = e.getPlayer();
		Sign sign = (Sign) e.getBlock().getState();
		if (Integer.parseInt(sign.getLine(3)) == playerID.get(player.getName())
				|| player.hasPermission("Expbank.override")) {
			int exp = Integer.parseInt(sign.getLine(2));
			player.giveExp(exp);
		} else {
			e.setCancelled(true);
		}
	}

	/**
	 * 
	 * @param e
	 */
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		if (e.getClickedBlock().getTypeId() == 63 || e.getClickedBlock().getTypeId() == 68) {
			Sign sign = (Sign) e.getClickedBlock().getState();
			if (!sign.getLine(0).equalsIgnoreCase("[expbank]"))
				return;
			if (!(Integer.parseInt(sign.getLine(3)) == playerID.get(player.getName()))) {
				player.sendMessage("This is not your sign!");
				return;
			}
			if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (player.getLevel() > 0) {
					if (addToSign(e, player)) {
						player.sendMessage("You stored 1 level!");
					} else {
						player.sendMessage("Expbank is full!");
					}
				} else {
					player.sendMessage("You don't have any levels to store");
				}
			} else if (e.getAction() == Action.RIGHT_CLICK_BLOCK)
				subtractFromSign(e, player);
		}
	}

	/**
	 * Subtracts experience from the Expbank for next level.
	 * @param e Data of the interaction.
	 * @param player Player which requested to get levels.
	 * @return Returns true when done.
	 */
	private boolean subtractFromSign(PlayerInteractEvent e, Player player) {
		Sign sign = (Sign) e.getClickedBlock().getState();
		int exp = Integer.parseInt(sign.getLine(2));
		if (exp > 0) {
			if (exp >= player.getExpToLevel()) {
				sign.setLine(2, Integer.toString(exp - player.getExpToLevel()));
				sign.update();
				player.giveExp(player.getExpToLevel());
			} else {
				sign.setLine(2, Integer.toString(0));
				sign.update();
				player.giveExp(exp);
			}
		} else {
			player.sendMessage("Expbank is empty!");
		}
		return true;
	}

	/**
	 * The method which adds experience to the bank.
	 * @param e Data of the interaction.
	 * @param player Player which requested to bank Exp.
	 * @return Returns true when done.
	 */
	private boolean addToSign(PlayerInteractEvent e, Player player) {
		Sign sign = (Sign) e.getClickedBlock().getState();
		int maxExp = 825;
		int exp = Integer.parseInt(sign.getLine(2));
		int level = player.getLevel();
		int toAdd = expTab[(level - (level - 1)) - 1];
		if (exp < maxExp) {
			if (exp < (exp + toAdd)) {
				sign.setLine(2, "Exp: " + Integer.toString(exp) + expTab[(level - 1)]);
				sign.update();
				float percentage = player.getExp();
				player.setLevel(player.getLevel() - 1);
				player.setExp(percentage);
				return true;
			} else {
				player.sendMessage("Not enough space to store exp of this level");
			}
		} else {
			return false;
		}
		return false;
	}

	/**
	 * 
	 * Checks if you entered the data for the expbank feature.
	 * @param e Data of event.
	 * 
	 */
	@EventHandler
	public void onSignChange(SignChangeEvent e) {
		Player player = e.getPlayer();
		if (e.getLine(0).equalsIgnoreCase("[expbank]")) {
			if (e.getLine(1).equals("") && e.getLine(2).equals("") && e.getLine(3).equals("")) {
				if (createSign(e, player)) {
					player.sendMessage("Expbank made!");
				}
			} else {
				player.sendMessage("Put it on the first line please!");
			}
		}
	}

	/**
	 * Method which makes the expbank.
	 * @param e Data of event.
	 * @param player The player where the expbank should be registered to.
	 * @return Returns true when it's been made.
	 */
	private boolean createSign(SignChangeEvent e, Player player) {
		e.setLine(0, "[ExpBank]");
		e.setLine(1, e.getPlayer().getName());
		e.setLine(2, "" + 0);
		if (!playerID.containsKey(player.getName())) {
			e.setLine(3, Integer.toString(createID(player)));
		} else {
			e.setLine(3, Integer.toString(playerID.get(player.getName())));
		}
		return true;
	}

	/**
	 * Registers the player to the IDBank with an ID.
	 * @param player Player to register to the IDBank.
	 * @return Returns the ID that's been assigned to the player.
	 */
	private int createID(Player player) {
		int value = playerID.size() + 1;
		playerID.put(player.getName(), value);
		return value;
	}

}

package com.github.dewfalse.MineAssist;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class MineAssist extends JavaPlugin {

	enum CommandType {ON, OFF, TOGGLE, TOOLS, ORES, RELOAD};

	class MineAssistConfig implements Cloneable {
		public boolean enable = false;

		public MineAssistConfig clone() {
			MineAssistConfig c;
			try {
				c = (MineAssistConfig)super.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}

			c.enable = this.enable;
			return c;
		}
	}

	Logger log;
	private Map<String, MineAssistConfig> userConfig = new LinkedHashMap<String, MineAssistConfig>();
	private MineAssistConfig globalConfig = new MineAssistConfig();

	public Set<Material> ore = new LinkedHashSet<Material>();
	public Set<Material> tool = new LinkedHashSet<Material>();
	public boolean use_permissions = false;
	public boolean full_damage = false;
	public boolean require_tools = false;
	public boolean enable_per_world = false;
	public Set<String> worldName = new LinkedHashSet<String>();

	@Override
	public void onDisable() {
		log.info("MineAssist disabled.");
	}

	@Override
	public void onEnable() {
		log = this.getLogger();
		log.info("MineAssist enabled.");
		getServer().getPluginManager().registerEvents(new BlockListener(this),
				this);
		init();
	}

	private void init() {
		if (!new File(getDataFolder(), "config.yml").exists()) {
			this.saveDefaultConfig();
		}

		this.reloadConfig();
		FileConfiguration config = this.getConfig();

		globalConfig.enable = config.getBoolean("Main.Automatic Ore Destruction");
		log.info("Main.Automatic Ore Destruction: " + String.valueOf(globalConfig.enable));

		use_permissions = config.getBoolean("Main.Use Permissions");
		log.info("Main.Use Permissions: " + String.valueOf(use_permissions));

		//mcMMO = config.getBoolean("Main.Use mcMMO if Available");
		//log.info("Main.Use mcMMO if Available: " + String.valueOf(mcMMO));

		for(Object o : config.getList("Ores.Ores List")) {
			ore.add(Material.valueOf((String) o));
		}
		log.info("Ores.Ores List: " + ore.toString());

		require_tools = config.getBoolean("Tools.Ore Destruction Require Tools");
		log.info("Tools.Ore Destruction Require Tools: " + String.valueOf(require_tools));

		full_damage = config.getBoolean("Tools.Apply Full Tool Damage");
		log.info("Tools.Apply Full Tool Damage: " + String.valueOf(full_damage));

		for(Object o : config.getList("Tools.Tools List")) {
			tool.add(Material.valueOf((String) o));
		}
		log.info("MineAssist Tools List: " + tool.toString());

		enable_per_world = config.getBoolean("Worlds.Enable Per World");
		log.info("Worlds.Enable Per World: " + String.valueOf(enable_per_world));

		for(Object o : config.getList("Worlds.Enabled Worlds")) {
			worldName.add((String) o);
		}

		this.saveConfig();
	}

	@EventHandler
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		if(cmd.getName().equalsIgnoreCase("MineAssist") == false) {
			return false;
		}

		if(args.length == 0) {
			return false;
		}

		int index = 0;
		boolean global = false;
		if(args[index].equalsIgnoreCase("Global")) {
			if(sender.hasPermission("mineassist.global") == false) {
				sender.sendMessage("You don't have mineassist.global");
				return true;
			}

			index++;
			global = true;
		}

		CommandType type = CommandType.valueOf(args[index++].toUpperCase());
		switch(type) {
		case RELOAD:
			if(sender.hasPermission("mineassist.global") == false) {
				sender.sendMessage("You don't have MineAssist.Global");
				return true;
			}
			if(sender.hasPermission("mineassist.reload") == false) {
				sender.sendMessage("You don't have mineassist.reload");
				return true;
			}
			init();
			return true;
		case ON:
		case OFF:
		case TOGGLE:
			if(sender.hasPermission("mineassist.toggle") == false) {
				sender.sendMessage("You don't have mineassist.toggle");
				return true;
			}
			if( (sender instanceof Player == false && index < args.length == false) || global) {
				switch(type) {
				case ON:
					this.globalConfig.enable = true;
					sender.sendMessage(ChatColor.GREEN + "[MineAssist] enabled for all");
					return true;
				case OFF:
					this.globalConfig.enable = false;
					sender.sendMessage(ChatColor.GREEN + "[MineAssist] disabled for all");
					return true;
				case TOGGLE:
					this.globalConfig.enable = !this.globalConfig.enable;
					sender.sendMessage(ChatColor.GREEN + "[MineAssist] " + (this.globalConfig.enable ? "enabled" : "disabled") + " for all");
					return true;
				}
			}
			else {
				String playerName = null;
				if(sender instanceof Player){
					playerName =sender.getName();
				}
				else {
					if(index < args.length == false) {
						return false;
					}
					Player player = getServer().getPlayer(args[index]);
					if(player == null) {
						sender.sendMessage(args[index] + " is not online");
						return true;
					}
					playerName = player.getName();
				}
				if(userConfig.get(playerName) == null) {
					userConfig.put(playerName, globalConfig.clone());
					userConfig.get(playerName).enable = false;
				}

				switch(type) {
				case ON:
					userConfig.get(playerName).enable = true;
					sender.sendMessage(ChatColor.GREEN + "[MineAssist] enabled for " + playerName);
					return true;
				case OFF:
					userConfig.get(playerName).enable = false;
					sender.sendMessage(ChatColor.GREEN + "[MineAssist] disabled for " + playerName);
					return true;
				case TOGGLE:
					userConfig.get(playerName).enable = !userConfig.get(playerName).enable;
					sender.sendMessage(ChatColor.GREEN + "[MineAssist] " + (userConfig.get(playerName).enable ? "enabled" : "disabled") + " for " + playerName);
					return true;
				}
			}
			break;
		case TOOLS:
			if(index < args.length == false) {
				sender.sendMessage("MineAssist Tools List: " + tool.toString());
				return true;
			}
			else {
				if(sender.hasPermission("mineassist.global") == false) {
					sender.sendMessage("You don't have mineassist.global");
					return true;
				}

				if(sender.hasPermission("mineassist.tools") == false) {
					sender.sendMessage("You don't have mineassist.tools");
					return true;
				}

				try {
					Material m = Material.valueOf(args[index]);
					if(tool.contains(m)) {
						tool.remove(m);
						sender.sendMessage(ChatColor.GREEN + "[MineAssist] Tools.Tools List remove:" + args[index]);
						sender.sendMessage(ChatColor.GREEN + "[MineAssist] Tools.Tools List:" + tool.toString());
					}
					else {
						tool.add(m);
						sender.sendMessage(ChatColor.GREEN + "[MineAssist] Tools.Tools List add:" + args[index]);
						sender.sendMessage(ChatColor.GREEN + "[MineAssist] Tools.Tools List:" + tool.toString());
					}
					return true;
				}
				catch(NullPointerException e) {
					sender.sendMessage(ChatColor.RED + "[MineAssist] Material " + args[index] + " not found");
				}
				catch(IllegalArgumentException e) {
					sender.sendMessage(ChatColor.RED + "[MineAssist] Material " + args[index] + " not found");
				}
			}
			break;
		case ORES:
			if(index < args.length == false) {
				sender.sendMessage("MineAssist Ores List: " + ore.toString());
				return true;
			}
			else {
				if(sender.hasPermission("mineassist.global") == false) {
					sender.sendMessage("You don't have mineassist.hlobal");
					return true;
				}

				if(sender.hasPermission("mineassist.ores") == false) {
					sender.sendMessage("You don't have mineassist.ores");
					return true;
				}

				try {
					Material m = Material.valueOf(args[index]);
					if(ore.contains(m)) {
						ore.remove(m);
						sender.sendMessage(ChatColor.GREEN + "[MineAssist] Ores.Ores List remove:" + args[index]);
						sender.sendMessage(ChatColor.GREEN + "[MineAssist] Ores.Ores List:" + ore.toString());
					}
					else {
						ore.add(m);
						sender.sendMessage(ChatColor.GREEN + "[MineAssist] Ores.Ores List add:" + args[index]);
						sender.sendMessage(ChatColor.GREEN + "[MineAssist] Ores.Ores List:" + ore.toString());
					}
					return true;
				}
				catch(NullPointerException e) {
					sender.sendMessage(ChatColor.RED + "[MineAssist] Material " + args[index] + " not found");
				}
				catch(IllegalArgumentException e) {
					sender.sendMessage(ChatColor.RED + "[MineAssist] Material " + args[index] + " not found");
				}
			}
			break;
		}

		return false;
	}

	public boolean getEnable(Player player) {
		if(globalConfig.enable == false) {
			return false;
		}
		if(userConfig.get(player.getName()) == null) {
			userConfig.put(player.getName(), globalConfig.clone());
			if(use_permissions) {
				userConfig.get(player.getName()).enable = player.hasPermission("mineassist.assist");
			}
		}

		return userConfig.get(player.getName()).enable;
	}

}

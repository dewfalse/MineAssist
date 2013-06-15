package com.github.dewfalse.MineAssist;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BlockListener implements Listener {

	public int max = 1000;
	private MineAssist plugin = null;

	public BlockListener(MineAssist instance) {
		plugin = instance;
		//plugin.log.info("init");
	}

	public Map<Player, BlockFace> clickedFace = new LinkedHashMap<Player, BlockFace>();

	public Map<String, LinkedHashSet<Position>> registered = new LinkedHashMap<String, LinkedHashSet<Position>>();
	public Map<String, Integer> expMap = new LinkedHashMap<String, Integer>();
	public List<Integer> tasks = new LinkedList<Integer>();

	public static boolean materialEqual(Material m1, Material m2) {
		if (m1 == m2) {
			return true;
		}
		if (m1 == Material.DIRT && m2 == Material.GRASS) {
			return true;
		}
		if (m1 == Material.GRASS && m2 == Material.DIRT) {
			return true;
		}
		if (m1 == Material.REDSTONE_ORE && m2 ==  Material.GLOWING_REDSTONE_ORE) {
			return true;
		}
		if (m1 ==  Material.GLOWING_REDSTONE_ORE && m2 == Material.REDSTONE_ORE) {
			return true;
		}
		return false;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockClicked(PlayerInteractEvent event) {
		BlockFace face = event.getBlockFace();
		Player player = event.getPlayer();
		clickedFace.put(player, face);
	}

	public Set<Position> getNext(Position pos, World world, Material type) {
		List<Integer> l = new LinkedList<Integer>();
		Set<Position> set = new LinkedHashSet<Position>();
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if (x == 0 && y == 0 && z == 0) {
						continue;
					}
					l.add(world.getBlockAt(pos.x + x, pos.y + y, pos.z + z)
							.getTypeId());
					if (materialEqual(
							world.getBlockAt(pos.x + x, pos.y + y, pos.z + z)
									.getType(), type)) {
						set.add(new Position(pos.x + x, pos.y + y, pos.z + z));
					}
				}
			}
		}
		// plugin.log.info(pos.toString() + ": " + l.toString());
		return set;
	}

	public boolean checkArround(Position pos, World world, Material type) {
		boolean bRet = false;
		List<Integer> l = new LinkedList<Integer>();
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if (x == 0 && y == 0 && z == 0) {
						continue;
					}
					l.add(world.getBlockAt(pos.x + x, pos.y + y, pos.z + z)
							.getTypeId());
					if (materialEqual(
							world.getBlockAt(pos.x + x, pos.y + y, pos.z + z)
									.getType(), type)) {
						bRet = true;
					}
				}
			}
		}
		// plugin.log.info(pos.toString() + ": " + l.toArray().toString());

		return bRet;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if(plugin.getEnable(event.getPlayer()) == false) {
			return;
		}

		Block block = event.getBlock();
		Material type = block.getType();
		if (plugin.ore.contains(type) == false) {
			return;
		}

		Player player = event.getPlayer();
		World world = player.getWorld();
		if(plugin.enable_per_world && plugin.worldName.contains(world.getName()) == false) {
			return;
		}

		if(plugin.use_permissions && player.hasPermission("mineassist.assist") == false) {
			return;
		}

		ItemStack itemStack = player.getItemInHand();
		if (itemStack == null) {
			return;
		}

		if(plugin.require_tools && plugin.tool.contains(itemStack.getType()) == false) {
			return;
		}

		if (block.getDrops(itemStack).isEmpty()) {
			return;
		}
		LinkedHashSet<Position> checked = new LinkedHashSet<Position>();

		{
			Queue<Position> queue = new LinkedList<Position>();

			queue.addAll(getNext(
					new Position(block.getX(), block.getY(), block.getZ()),
					world, type));
			while (queue.isEmpty() == false) {
				// plugin.log.info("queu size: " +
				// String.valueOf(queue.size()));
				Position c = queue.poll();
				if (c == null) {
					break;
				}

				checked.add(c);
				// plugin.log.info("checked add: " + c.toString());
				// plugin.log.info("getNext size: " + String.valueOf(getNext(c,
				// world, typeid).size()));

				for (Position pos : getNext(c, world, type)) {
					if (checked.contains(pos) || queue.contains(pos)) {
						// plugin.log.info("skip: " + pos.toString());
						continue;
					}
					queue.add(pos);
					// plugin.log.info("queue add: " + pos.toString());
				}

				if (checked.size() >= max) {
					break;
				}
			}
		}

		registered.put(player.getName(), checked);
		expMap.put(player.getName(), event.getExpToDrop());

		int taskID = plugin.getServer().getScheduler()
				.scheduleSyncRepeatingTask(plugin, new Runnable() {

					public void run() {
						int rest = 0;
						for (Map.Entry<String, LinkedHashSet<Position>> e : registered
								.entrySet()) {
							rest += e.getValue().size();
							Player player = plugin.getServer().getPlayer(
									e.getKey());
							// plugin.getServer().broadcastMessage("delay task: "
							// + player.getName() + " " + e.getValue().size());

							int n = 0;
							Set<Position> remove = new LinkedHashSet<Position>();
							for (Position pos : e.getValue()) {
								remove.add(pos);
								n++;
								// player.sendMessage(ChatColor.RED + "pos: " +
								// pos.toString());
								if(player == null) {
									continue;
								}
								if(player.getWorld() == null) {
									continue;
								}
								Block b = player.getWorld().getBlockAt(pos.x,
										pos.y, pos.z);
								if (b == null) {
									// player.sendMessage(ChatColor.RED +
									// "block null: " + pos.toString());
									continue;
								}

								ItemStack itemStack = player.getItemInHand();

								if (itemStack != null) {
									//player.sendMessage(ChatColor.RED + "breakNaturally: " + itemStack.toString());
									//b.breakNaturally(itemStack);
								} else {
									//b.breakNaturally();
								}
								// plugin.log.info("break: " + pos.toString());

								for(ItemStack drop : b.getDrops(itemStack)) {
									Location location = new Location(player.getWorld(), pos.x, pos.y, pos.z);
									player.getWorld().dropItemNaturally(location, drop);
								}
								b.setTypeId(0);

								int exp = expMap.get(player.getName());
								if (exp > 0) {
									ExperienceOrb orb = player.getWorld()
											.spawn(b.getLocation(),
													ExperienceOrb.class);
									orb.setExperience(exp);
								}
								if (itemStack != null) {
									if(plugin.full_damage) {
										 player.getItemInHand().setDurability((short)
												 ( player.getItemInHand().getDurability()
												 + 1));
												 //player.chat("durability: " +
												 //String.valueOf(player.getItemInHand().getDurability()));
									}
								}

								if (n > 10) {
									break;
								}
							}

							for (Position pos : remove) {
								e.getValue().remove(pos);
							}

						}

						if (rest == 0) {
							plugin.getServer()
									.getScheduler()
									.scheduleSyncDelayedTask(plugin,
											new Runnable() {

												public void run() {
													for (int i : tasks) {
														plugin.getServer()
																.getScheduler()
																.cancelTask(i);
													}
												}
											}, 1L);
						}
						// plugin.getServer().broadcastMessage("This message is broadcast by the main thread");
					}
				}, 1L, 1L);

		tasks.add(taskID);

		// plugin.log.info("checked size: " + String.valueOf(checked.size()));
		// player.sendMessage(ChatColor.RED + "checked: " +
		// String.valueOf(checked.size()));

	}
}

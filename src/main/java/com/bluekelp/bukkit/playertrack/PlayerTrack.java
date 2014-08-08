package com.bluekelp.bukkit.playertrack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.PersistenceException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerTrack extends JavaPlugin implements CommandExecutor, Listener {

	static final String CFG_HEATMAP = "player track.";
	static final String VALUE_MIN_DISTANCE = "min distance to record";
	static final String VALUE_FREQUENCY = "frequency";
	static final String VALUE_URL = "url";

	int minDistanceForNewPoint; // min distance (units=blocks) player must move to register a new location (else update time spent at last)
	int frequency; // # seconds between location polls and (possible) updating of player location

	Map<String, Long> playerTracking = new HashMap<String, Long>();

	Thread task;
	volatile boolean paused = false;
	volatile boolean stopTask = false;

	@Override
	public void onEnable() {
		ensureDatabaseSetup();
		loadConfig();
		getCommand("ptrack").setExecutor(this);
		registerEvents();
		startTask();
	}

	@Override
	public void onDisable() {
		stopTask = true;
		this.unregisterEvents();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("ptrack")) {
			if (args.length < 1) {
				return false; // usage?
			}

			if ("pause".equalsIgnoreCase(args[0])) {
				paused = true;
				// do *not* update config - always start task on plugin load
			}
			else if ("resume".equalsIgnoreCase(args[0])) {
				paused = false;
			}
			else if (args.length > 1 && ("freq".equalsIgnoreCase(args[0]) || "frequency".equalsIgnoreCase(args[0]))) {
				frequency = Integer.parseInt(args[1]);
				getConfig().set(CFG_HEATMAP+VALUE_FREQUENCY, frequency);
                saveConfig();
			}
			else if (args.length > 1 && ("dist".equalsIgnoreCase(args[0]) || "distance".equalsIgnoreCase(args[0])
										|| "minDist".equals(args[0]) || "minDistance".equalsIgnoreCase(args[0]))) {
				minDistanceForNewPoint = Integer.parseInt(args[1]);
				getConfig().set(CFG_HEATMAP+VALUE_MIN_DISTANCE, minDistanceForNewPoint);
                saveConfig();
			}

			return true;
		}

		return false;
	}

	@Override
	public List<Class<?>> getDatabaseClasses() {
		// Must register any "bean" classes we'll be using first
		List<Class<?>> classes = new LinkedList<Class<?>>();
		classes.add(PlayerLocationRow.class);
		return classes;
	}

	public void debug(String msg) {
		getLogger().fine(msg);
	}

	public void info(String msg) {
		getLogger().info(msg);
	}

	void registerEvents() {
		getServer().getPluginManager().registerEvents(this, this);
	}

	void loadConfig() {
        this.saveDefaultConfig();
		this.reloadConfig();

		this.minDistanceForNewPoint = this.getConfig().getInt(CFG_HEATMAP+VALUE_MIN_DISTANCE, 16);
		this.frequency = this.getConfig().getInt(CFG_HEATMAP+VALUE_FREQUENCY, 30);
	}

	void unregisterEvents() {
		HandlerList.unregisterAll((Listener) this);
	}

	private void ensureDatabaseSetup() {
		try {
			this.getDatabase().find(PlayerLocationRow.class).findRowCount();
		}
		catch (PersistenceException ex) {
			this.installDDL();
		}
	}

	// ----------

	private void recordPlayerLocations() {
		for (Player player: getServer().getOnlinePlayers()) {
			recordOrUpdatePlayerLocation(player);
		}
	}

	private void recordOrUpdatePlayerLocation(Player player) {
		PlayerLocationRow row = rowFromPlayer(player);

		if (row == null) {
			recordNewLocation(player);
			return;
		}

		Location playerLocation = player.getLocation();
		Location lastPlayerLocation = locationFromRow(row);

		double squaredDistance = playerLocation.distanceSquared(lastPlayerLocation);
		double squaredMinDistance = minDistanceForNewPoint * minDistanceForNewPoint;
		boolean farEnoughToReport = squaredDistance > squaredMinDistance;

		boolean changedWorlds = lastPlayerLocation.getWorld().getName().equals(playerLocation.getWorld().getName()) == false;

		updateStopTime(row);

		if (farEnoughToReport || changedWorlds) {
			recordNewLocation(player);
		}
	}

	PlayerLocationRow rowFromPlayer(Player player) {
		if (playerTracking.containsKey(player.getName()) == false) {
			return null;
		}

		long id = playerTracking.get(player.getName());
		return rowFromId(id);
	}

	PlayerLocationRow rowFromId(long id) {
		return getDatabase().find(PlayerLocationRow.class, id);
	}

	private void recordNewLocation(Player player) {
		PlayerLocationRow row;
		row = newRowFromPlayer(player);
		getDatabase().save(row);

		long id = row.getId();
		playerTracking.put(player.getName(), id);
	}

	private void updateStopTime(PlayerLocationRow row) {
		// Java uses "milliseconds since epoch" but almost everyone else uses
		// seconds since - so we /1000 to convert and record in seconds
		row.setStop(System.currentTimeMillis() / 1000);
		getDatabase().update(row);
	}

	private PlayerLocationRow newRowFromPlayer(Player player) {
		PlayerLocationRow row = getDatabase().createEntityBean(PlayerLocationRow.class);

		row.setPlayer(player.getName());
		row.setWorld(player.getWorld().getName());
		row.setX(player.getLocation().getBlockX());
		row.setY(player.getLocation().getBlockY());
		row.setZ(player.getLocation().getBlockZ());

		// Java uses "milliseconds since epoch" but almost everyone else uses
		// seconds since - so we /1000 to convert and record in seconds
		row.setStart(System.currentTimeMillis() / 1000);
		row.setStop(System.currentTimeMillis() / 1000);
		return row;
	}

	private void startTask() {
		stopTask = false;

		task = new Thread(new Runnable() {

			@Override
			public void run() {
				do {
					try {
						Thread.sleep(frequency*1000);
					}
					catch (InterruptedException ex) {}

					if (!paused) {
						recordPlayerLocations();
					}
				} while(!stopTask);
				// if exit from do-while above, exit thread
			}
		});

		task.setDaemon(true);
		task.start();
	}

	Location locationFromRow(PlayerLocationRow row) {
		if (row == null) {
			return null;
		}

		World world = getServer().getWorld(row.getWorld());
		double x = row.getX();
		double y = row.getY();
		double z = row.getZ();
		return new Location(world, x, y, z);
	}
}

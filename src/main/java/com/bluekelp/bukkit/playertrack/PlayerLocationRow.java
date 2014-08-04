package com.bluekelp.bukkit.playertrack;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="player_loc")
public class PlayerLocationRow {

	public PlayerLocationRow() {
		id = 0;
		player = world = "";
		x = y = z = 0;
		start = stop = 0L;
	}

	@Id private int id;
	@Column private String player;
	@Column private String world;
	@Column private int x;
	@Column private int y;
	@Column private int z;
	@Column private long start;
	@Column private long stop;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getPlayer() {
		return player;
	}
	public void setPlayer(String player) {
		this.player = player;
	}
	public String getWorld() {
		return world;
	}
	public void setWorld(String world) {
		this.world = world;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getZ() {
		return z;
	}
	public void setZ(int z) {
		this.z = z;
	}
	public long getStart() {
		return start;
	}
	public void setStart(long start) {
		this.start = start;
	}
	public long getStop() {
		return stop;
	}
	public void setStop(long stop) {
		this.stop = stop;
	}
}

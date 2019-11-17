package com.nullbyte.telepads;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

public class TelePad extends BukkitRunnable {
	private Location lapisLoc;
	private TelePad linkedPad;
	public boolean scheduleDestruction = false;
	private String owner;
	
	public TelePad(Location lapisLoc, String owner) {
		this.owner = owner;
		this.lapisLoc = lapisLoc;
		linkedPad = null;
	}
	
	public boolean cantInteract(Location l, Player p) {
		int cx = lapisLoc.getBlockX();
		int cy = lapisLoc.getBlockY();
		int cz = lapisLoc.getBlockZ();
		if(owner.equals(p.getName())) return true;
		if(l.getBlockX() >= cx-2 && l.getBlockX() <= cx+2 && l.getBlockZ() >= cz-2 && l.getBlockZ() <= cz+2 && l.getBlockY() >= cy && l.getBlockY() <= cy+2) return false;
		return true;
	}
	
	public void link(TelePad other) {
		linkedPad = other;
		other.linkedPad = this;
	}
	
	public TelePad getLinked() {
		return linkedPad;
	}
	
	public String getOwner() {
		return owner;
	}
	
	public String getIdentifier() {
		return "tpad@"+lapisLoc.getWorld().getName()+"."+lapisLoc.getBlockX()+"."+lapisLoc.getBlockY()+"."+lapisLoc.getBlockZ();
	}
	
	public static Location getLocationFromIdentifier(String identifier) {
		String locString = identifier.trim().split("@")[1];
		String worldString = "";
		String xString = "";
		String yString = "";
		String zString = "";
		
		int stage = 0;
		
		for(int i=0; i<locString.length(); i++) {
			if(locString.charAt(i) == '.') {
				stage++;
				continue;
			}
			if(stage == 0) {
				worldString += locString.charAt(i);
				continue;
			}
			if(stage == 1) {
				xString += locString.charAt(i);
				continue;
			}
			if(stage == 2) {
				yString += locString.charAt(i);
				continue;
			}
			if(stage == 3) {
				zString += locString.charAt(i);
				continue;
			}
		}
		
		if(stage != 3) {
			return null;
		}
		World w = Bukkit.getWorld(worldString);
		int x = Integer.parseInt(xString);
		int y = Integer.parseInt(yString);
		int z = Integer.parseInt(zString);
		return new Location(w,x,y,z);
	}
	
	public static boolean checkIntegrity(Location lapisBlock) {
		if(lapisBlock == null) return false;
		World w = lapisBlock.getWorld();
		int cx = lapisBlock.getBlockX();
		int cy = lapisBlock.getBlockY();
		int cz = lapisBlock.getBlockZ();
		
		
		// Check lapis
		if(!w.getBlockAt(lapisBlock).getType().equals(Material.LAPIS_BLOCK)) return false;
		
		
		// Check obsidian
		for(int dx=-1; dx<=1; dx++) {
			for(int dz=-1; dz<= 1; dz++) {
				if(dx == 0 && dz == 0) continue;
				if(!w.getBlockAt(cx+dx, cy, cz+dz).getType().equals(Material.OBSIDIAN)) return false;
			}
		}
		
		// Check diamond and gold
		for(int dx=-2; dx<=2; dx++) {
			for(int dz=-2; dz<=2; dz++) {
				if(!(dx == -2 || dx==2 || dz == -2 || dz == 2)) continue;
				if(dx >= -1 && dx <= -1 && dz >= -1 && dz <= 1) continue;
				
				// Check gold
				if( (dx == -2 && (dz == 2 || dz == -2)) || (dx == 2 && (dz == -2 || dz == 2)) ) {
					if(!w.getBlockAt(cx+dx,  cy, cz+dz).getType().equals(Material.GOLD_BLOCK)) return false;
				}
				
				else {
					// Check diamond blocks
					if(!w.getBlockAt(cx+dx, cy, cz+dz).getType().equals(Material.DIAMOND_BLOCK)) return false;
				}
			}
		}
		return true;
	}
	
	public static boolean isShulkerBox(Material mat) {
		return mat.equals(Material.SHULKER_BOX) || mat.equals(Material.BLACK_SHULKER_BOX) || mat.equals(Material.BLUE_SHULKER_BOX) || mat.equals(Material.BROWN_SHULKER_BOX) || mat.equals(Material.CYAN_SHULKER_BOX) || mat.equals(Material.GRAY_SHULKER_BOX) || mat.equals(Material.GREEN_SHULKER_BOX) || mat.equals(Material.LIGHT_BLUE_SHULKER_BOX) || mat.equals(Material.LIGHT_GRAY_SHULKER_BOX) || mat.equals(Material.LIME_SHULKER_BOX) || mat.equals(Material.MAGENTA_SHULKER_BOX) || mat.equals(Material.ORANGE_SHULKER_BOX) || mat.equals(Material.PINK_SHULKER_BOX) || mat.equals(Material.PURPLE_SHULKER_BOX) || mat.equals(Material.RED_SHULKER_BOX);
	}
	
	public boolean hasFuel() {
		World w = lapisLoc.getWorld();
		int cx = lapisLoc.getBlockX();
		int cy = lapisLoc.getBlockY();
		int cz = lapisLoc.getBlockZ();
		for(int dx=-2; dx<=2; dx++) {
			for(int dz=-2; dz<=2; dz++) {
				if(dx == 0 && dz == 0) continue;
				if(isShulkerBox(w.getBlockAt(cx+dx, cy+1, cz+dz).getType())) {
					Inventory chestInv = ((ShulkerBox)w.getBlockAt(cx+dx, cy+1, cz+dz).getState()).getInventory();
					if(chestInv.contains(Material.GOLD_BLOCK)) return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean consumeFuel() {
		World w = lapisLoc.getWorld();
		int cx = lapisLoc.getBlockX();
		int cy = lapisLoc.getBlockY();
		int cz = lapisLoc.getBlockZ();
		for(int dx=-2; dx<=2; dx++) {
			for(int dz=-2; dz<=2; dz++) {
				if(dx == 0 && dz == 0) continue;
				if(isShulkerBox(w.getBlockAt(cx+dx, cy+1, cz+dz).getType())) {
					Inventory chestInv = ((ShulkerBox)w.getBlockAt(cx+dx, cy+1, cz+dz).getState()).getInventory();
					if(chestInv.contains(Material.GOLD_BLOCK)) {
						chestInv.getItem(chestInv.first(Material.GOLD_BLOCK)).setAmount(chestInv.getItem(chestInv.first(Material.GOLD_BLOCK)).getAmount()-1);
					}
				}
			}
		}
		return false;
	}
	
	public void tp(Player p) {
		if(linkedPad != null && hasFuel() && checkIntegrity(linkedPad.lapisLoc)) {
			consumeFuel();
			showTPEffects();
			linkedPad.showTPEffects();
			p.setSneaking(false);
			p.teleport(new Location(linkedPad.lapisLoc.getWorld(), linkedPad.lapisLoc.getBlockX(), linkedPad.lapisLoc.getBlockY()+1, linkedPad.lapisLoc.getBlockZ()));
		}
		
		// Otherwise can't tp
	}
	
	public Player getPlayerOnPad() {
		for(Entity e : lapisLoc.getWorld().getNearbyEntities(lapisLoc, 3.0, 3.0, 3.0)) {
			if(e instanceof Player) {
				Player p = (Player)e;
				if(!p.isSneaking()) continue;
				if(p.getLocation().getBlockX() == lapisLoc.getBlockX() && p.getLocation().getBlockY() == lapisLoc.getBlockY()+1 && p.getLocation().getBlockZ() == lapisLoc.getBlockZ()) return p;
			}
		}
		return null;
	}
	
	public void showTPEffects() {
		World w = lapisLoc.getWorld();
		int cx = lapisLoc.getBlockX();
		int cy = lapisLoc.getBlockY();
		int cz = lapisLoc.getBlockZ();
		for(int dx=-2; dx<=2; dx++) {
			for(int dz=-2; dz<=2; dz++) {
				w.spawnParticle(Particle.PORTAL, new Location(w,cx+dx,cy+1,cz+dz), 1);
				w.playSound(new Location(w, cx+dx, cy+1, cz+dz)  ,Sound.ENTITY_ENDERMAN_TELEPORT , 1f , 1f);
			}
		}
	}
	
	
	public Location getLapisLocation() {
		return lapisLoc;
	}
	
	@Override
	public void run() {
		if(!checkIntegrity(lapisLoc)) {
			scheduleDestruction = true;
			if(linkedPad != null) linkedPad.scheduleDestruction = true;
		}
		else if(linkedPad != null) {
			Player p = getPlayerOnPad();
			if(p != null) {
				tp(p);
			}
		}
		

	}
	
	
}

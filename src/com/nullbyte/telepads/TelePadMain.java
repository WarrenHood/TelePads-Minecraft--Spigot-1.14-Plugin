package com.nullbyte.telepads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;


import net.md_5.bungee.api.ChatColor;



public class TelePadMain extends JavaPlugin implements Listener{
	
	JavaPlugin plugin;
	ArrayList<TelePad> telePads = new ArrayList<TelePad>();
	
	@Override
	public void onEnable() {
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		this.plugin = this;
		
		String fileSeparator = System.getProperty("file.separator");
		File pluginDir = new File("plugins" + fileSeparator + "TelePads");
		pluginDir.mkdir();
		
		loadTelePads();
		
		new TelePadCleaner().runTaskTimer(plugin, 5, 5);
		
	}
	
	public void saveTelePads() {
		String fileSeparator = System.getProperty("file.separator");
		String path = "plugins" + fileSeparator + "TelePads" + fileSeparator + "telepads.txt";
		File teleFile = new File(path);
		teleFile.delete();
		String fileString = "";
		for(TelePad t : telePads) {
			String thisLoc = t.getIdentifier();
			TelePad linkedPad = t.getLinked();
			String owner = t.getOwner();
			String linkedLoc = "null";
			if(linkedPad != null && !linkedPad.scheduleDestruction) linkedLoc = linkedPad.getIdentifier();
			fileString += thisLoc +";" + linkedLoc + ";" + owner+"\n";		
		}
		
		try {
			FileOutputStream fos = new FileOutputStream(path);
			fos.write(fileString.getBytes());
			fos.flush();
			fos.close();
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}
	
	public void loadTelePads() {
		String fileSeparator = System.getProperty("file.separator");
		String path = "plugins" + fileSeparator + "TelePads" + fileSeparator + "telepads.txt";
			
			BufferedReader inFile;
			try {
				inFile = new BufferedReader(new FileReader(path));
				String currentLine;
				String[] locString;
				Location currentLocation;
				Location linkedLocation;
				String ownerName;
				do {
					currentLine = inFile.readLine();
					if(currentLine == null) break;
					locString = currentLine.split(";");
					currentLocation = TelePad.getLocationFromIdentifier(locString[0]);
					linkedLocation = TelePad.getLocationFromIdentifier(locString[1]);
					ownerName = locString[2].trim();
					boolean currentAdded = addTelePad(currentLocation, ownerName);
					boolean linkedAdded = addTelePad(linkedLocation, ownerName);
					
					if(currentAdded && linkedAdded) {
						getTelePad(currentLocation).link(getTelePad(linkedLocation));
					}
				} while(currentLine != null);
				saveTelePads();
			} catch (IOException e) {
				//e.printStackTrace();
			}
			
		
	}
	
	public void removeTelePad(Location lapisLoc) {
		for(int i=0; i<telePads.size(); i++) {
			if(lapisLoc.equals(telePads.get(i).getLapisLocation())) {
				telePads.get(i).cancel();
				telePads.get(i).tellOwner(ChatColor.DARK_RED + "Your telePad at " + telePads.get(i).getLapisLocation().toVector().toString() + " has been destroyed." );
				telePads.remove(i);
				saveTelePads();
				return;
			}
		}
	}
	
	class TelePadCleaner extends BukkitRunnable {

		@Override
		public void run() {
			for(TelePad t : telePads) {
				if(t.scheduleDestruction) {
					removeTelePad(t.getLapisLocation());
					return;
				}
			}
		}
		
	}
	
	public TelePad getTelePad(Location lapisLoc) {
		for(TelePad tpad : telePads) {
			if(tpad.getLapisLocation().equals(lapisLoc)) return tpad;
		}
		return null;
	}
	
	public boolean addTelePad(Location lapisLoc, String owner) {
		if(lapisLoc == null) return false;
		if(TelePad.checkIntegrity(lapisLoc)) {
			TelePad tpad = getTelePad(lapisLoc);
			if(tpad == null) {
				tpad = new TelePad(lapisLoc, owner);
				telePads.add(tpad);
				tpad.runTaskTimer(plugin, 5, 5);
				return true;
			}
			return false;
		}
		return false;
	}
	
	public boolean canInteract(Location l, Player p) {
		for(TelePad t : telePads) {
			if(!t.cantInteract(l, p)) return false;
		}
		return true;
	}
	
	@EventHandler
	public void onRightClick(PlayerInteractEvent e) {
		if(e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if(!canInteract(e.getClickedBlock().getLocation(), e.getPlayer())) {
				e.getPlayer().sendMessage(ChatColor.DARK_RED + "This is not your telepad! You cannot do this here!");
				e.setCancelled(true);
				return;
			}
		}
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Block clicked = e.getClickedBlock();
			TelePad possibleTelepad = getTelePad(clicked.getLocation());
			//e.getPlayer().sendMessage("You right clicked a block");
			if(clicked.getType().equals(Material.LAPIS_BLOCK)) {
				//e.getPlayer().sendMessage("And it was a lapis block");
				
				if(!TelePad.checkIntegrity(clicked.getLocation())) {
					e.getPlayer().sendMessage(ChatColor.DARK_RED + "That isn't a valid telepad structure!");
					return;
				}
				ItemStack handItem = e.getPlayer().getInventory().getItemInMainHand();
				if(!handItem.getType().equals(Material.NETHER_STAR)) {
					return;
				}
				ItemMeta handMeta = handItem.getItemMeta();
				if(handMeta.getLore() != null && handMeta.getLore().size() > 0) {
					Location linkedLoc = TelePad.getLocationFromIdentifier(handMeta.getLore().get(0).toString());
					if(linkedLoc != null) {
						TelePad linkedPad = getTelePad(linkedLoc);
						if(linkedPad != null) {
							boolean success = addTelePad(clicked.getLocation(), e.getPlayer().getName());
							if(success) {
								getTelePad(clicked.getLocation()).link(linkedPad);
								e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "TelePads have been linked.");
								saveTelePads();
								handItem.setAmount(handItem.getAmount()-1);
							}
						}
						else {
							e.getPlayer().sendMessage(ChatColor.DARK_RED + "It appears that the telepad you are trying to link to has been destroyed... Unlinking it from this star");
							handMeta.setLore(null);
							handItem.setItemMeta(handMeta);
						}
					}
				}
				else {
					boolean success = addTelePad(clicked.getLocation(), e.getPlayer().getName());
					if(success) {
						TelePad thisPad = getTelePad(clicked.getLocation());
						ArrayList<String> newLore = new ArrayList<String>();
						newLore.add(thisPad.getIdentifier());
						handMeta.setLore(newLore);
						handItem.setItemMeta(handMeta);
						e.getPlayer().sendMessage(ChatColor.DARK_GREEN + "TelePad created. Link it to another TelePad with that same nether star");
						saveTelePads();
					}
				}
			}
			
		}
	}
}


package com.yottabyte.bukkit;

import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.entity.Player;

/**
 * Simple admin plugin for Bukkit.
 * @author yottabyte
 */
public class KiwiAdminPlayerListener extends PlayerListener {
    private final KiwiAdmin plugin;
    public KiwiAdminPlayerListener(KiwiAdmin instance) {
        plugin = instance;
    }
    
    
    public void onPlayerLogin(PlayerLoginEvent event){
    	Player player = event.getPlayer();
    	Boolean banned = KiwiAdmin.bannedPlayers.get(player.getName());
    	if(banned != null){
    		event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "You are banned from this server.");
    		System.out.println(player.getName() + " is banned! Deny!");
    	}else{
    		System.out.println(player.getName() + " is not banned. Allow!");
    	}
    }
}

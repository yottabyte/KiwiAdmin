
package se.kiwike.yottabyte;

import java.util.Date;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.entity.Player;

import se.kiwike.yottabyte.KiwiAdmin;


public class KiwiAdminPlayerListener extends PlayerListener {
	KiwiAdmin plugin;

	public KiwiAdminPlayerListener(KiwiAdmin instance) {
		this.plugin = instance;
	}
	public void onPlayerLogin(PlayerLoginEvent event){
		Player player = event.getPlayer();
		if(plugin.bannedPlayers.contains(player.getName().toLowerCase())){
			System.out.println("banned player joined");
			if(plugin.tempBans.get(player.getName().toLowerCase()) != null){
				long tempTime = plugin.tempBans.get(player.getName().toLowerCase());
				long now = System.currentTimeMillis()/1000;
				long diff = tempTime - now;
				if(diff <= 0){
					plugin.bannedPlayers.remove(player.getName().toLowerCase());
					plugin.tempBans.remove(player.getName().toLowerCase());
					return;
				}
				Date date = new Date();
				date.setTime(tempTime*1000);
				plugin.properties.load();
				String kickerMsg = plugin.formatMessage(plugin.properties.getNode("messages").getString("LoginTempban"));
				kickerMsg = kickerMsg.replaceAll("%time%", date.toString());
				kickerMsg = kickerMsg.replaceAll("%reason%", plugin.db.getBanReason(player.getName()));
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickerMsg);
				return;
			}
			plugin.properties.load();
			String kickerMsg = plugin.formatMessage(plugin.properties.getNode("messages").getString("LoginBan"));
			kickerMsg = kickerMsg.replaceAll("%reason%", plugin.db.getBanReason(player.getName()));
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickerMsg);
		}
	}
	public void onPlayerJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
		String ip = player.getAddress().getAddress().getHostAddress();
		System.out.println("connect from ip " + ip);
		if(plugin.bannedIPs.contains(ip)){
			System.out.println("ip is banned");
			event.setJoinMessage(null);
			plugin.properties.load();
			String kickerMsg = plugin.formatMessage(plugin.properties.getNode("messages").getString("LoginIPBan"));
			player.kickPlayer(kickerMsg);
		}
		if(!plugin.db.matchAddress(player.getName(), ip)){
			plugin.db.updateAddress(player.getName(), ip);
		}
	}
}

package com.yottabyte.bukkit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;

public class Database {

	public Database(Plugin plugin){

		if(KiwiAdmin.useMysql){
			Connection conn = SQLConnection.getSQLConnection();
			if (conn == null) {
				KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Could not establish SQL connection. Disabling KiwiAdmin");
				plugin.getServer().getPluginManager().disablePlugin(plugin);
				return;
			} else {

				PreparedStatement ps = null;
				ResultSet rs = null;	
				try {
					ps = conn.prepareStatement("SELECT * FROM " + KiwiAdmin.mysqlTable);
					rs = ps.executeQuery();
					while (rs.next()){
						String pName = rs.getString("name").toLowerCase();
						Timestamp pTime;
						try{
							pTime = rs.getTimestamp("temptime");
						} catch(SQLException ex){
							pTime = new Timestamp(0);
						}
						KiwiAdmin.bannedPlayers.add(pName.toLowerCase());
						if(pTime.getTime() != 0){
							KiwiAdmin.tempBans.put(rs.getString("name").toLowerCase(),pTime.getTime());
						}
					}
				} catch (SQLException ex) {
					KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
				} finally {
					try {
						if (ps != null)
							ps.close();
						if (conn != null)
							conn.close();
					} catch (SQLException ex) {
						KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
					}
				}	

				try {
					conn.close();
					KiwiAdmin.log.log(Level.INFO, "[KiwiAdmin] Initialized db connection" );
				} catch (SQLException e) {
					e.printStackTrace();
					plugin.getServer().getPluginManager().disablePlugin(plugin);
				}
			}		
		}
		else{ //if not using mysql, use flatfile instead!

			//read the banlist txt file
			try {

				File banlist = new File("plugins/KiwiAdmin/banlist.txt");

				if (banlist.exists()){ 
					BufferedReader in = new BufferedReader(new FileReader(banlist));
					String data = null;

					while ((data = in.readLine()) != null){
						//Checking for blank lines
						if (!data.startsWith("#")){
							if (data.length()>0){
								String[] values = data.split(">>");
								String player = values[0];
								try{
									Timestamp pTime = Timestamp.valueOf(values[4]);

									KiwiAdmin.tempBans.put(player.toLowerCase(),pTime.getTime());
								} catch(Exception e){

								}

								KiwiAdmin.bannedPlayers.add(player.toLowerCase());
							}
						}

					}
					in.close();
				}
			}
			catch (IOException e) {

				e.printStackTrace();

			} 
		}
	}

	/*
	 * Update old version of flatfile database
	 */

	public static void updateFlatFile(){
		try {
			File banlist = new File("plugins/KiwiAdmin/banlist.txt");
			if (banlist.exists()){
				BufferedReader in = new BufferedReader(new FileReader(banlist));
				File tempFile = new File(banlist.getAbsolutePath() + ".tmp");
				PrintWriter out = new PrintWriter(new FileWriter(tempFile));
				String data = null;
				while ((data = in.readLine()) != null){
					//Checking for blank lines
					if(data.length()>0){
						if (!data.contains(">>")){
							String player = data;
							out.println(player+">>undefined>>?>>?>>0");
						}else{
							out.println(data);
						}
					}
				}
				in.close();
				out.close();

				// Let's delete the old banlist.txt and change the name of our temporary list!
				banlist.delete();
				tempFile.renameTo(banlist);
			}
		}
		catch (IOException e) {

			e.printStackTrace();

		} 
	}


	/*
	 * Remove a player from banlist
	 */
	public static boolean removeFromBanlist(String p){
		if(!KiwiAdmin.bannedPlayers.contains(p.toLowerCase())){
			return false;
		}
		if(KiwiAdmin.useMysql){

			Connection conn = null;
			PreparedStatement ps = null;
			try {
				conn = SQLConnection.getSQLConnection();
				ps = conn.prepareStatement("DELETE FROM " + KiwiAdmin.mysqlTable + " WHERE name = ?");
				ps.setString(1, p.toLowerCase());
				ps.executeUpdate();
			} catch (SQLException ex) {
				KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
				return false;
			} finally {
				try {
					if (ps != null)
						ps.close();
					if (conn != null)
						conn.close();
				} catch (SQLException ex) {
					KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
				}
			}

		}else{
			//flatfile setup
			try {
				String file = "plugins/KiwiAdmin/banlist.txt";
				File banlist = new File(file);

				File tempFile = new File(banlist.getAbsolutePath() + ".tmp");

				BufferedReader br = new BufferedReader(new FileReader(file));
				PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

				String line = null;

				// Loops through the temporary file and deletes the player
				while ((line = br.readLine()) != null) {
					if (!line.trim().toLowerCase().startsWith(p.toLowerCase()) && line.length() > 0) {
						pw.println(line);
						pw.flush();
					}
				}
				// All done, SHUT. DOWN. EVERYTHING.
				pw.close();
				br.close();

				// Let's delete the old banlist.txt and change the name of our temporary list!
				banlist.delete();
				tempFile.renameTo(banlist);

				return true;

			}
			catch (FileNotFoundException ex) {
				ex.printStackTrace();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return true;

	}

	/*
	 * Add a new player, permanent
	 */
	public void addPlayer(String p, String reason, String kicker){

		addPlayer(p, reason, kicker, 0);
	}
	/*
	 * Add a new player
	 */
	public void addPlayer(String p, String reason, String kicker, long tempTime){

		java.util.Date date = new java.util.Date(); 
		Timestamp time = new Timestamp(date.getTime());
		Timestamp temptime = new Timestamp(tempTime);

		if(KiwiAdmin.useMysql){ // Using MySQL, bunch of crap here.

			Connection conn = null;
			PreparedStatement ps = null;
			try {
				conn = SQLConnection.getSQLConnection();
				if(tempTime > 0){
					ps = conn.prepareStatement("INSERT INTO " + KiwiAdmin.mysqlTable + " (name,reason,admin,time,temptime) VALUES(?,?,?,?,?)");
					ps.setTimestamp(5, temptime);
				}else
					ps = conn.prepareStatement("INSERT INTO " + KiwiAdmin.mysqlTable + " (name,reason,admin,time) VALUES(?,?,?,?)");
				ps.setString(1, p);
				ps.setString(2, reason);
				ps.setString(3, kicker);
				ps.setTimestamp(4, time);
				ps.executeUpdate();
			} catch (SQLException ex) {
				KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
			} finally {
				try {
					if (ps != null)
						ps.close();
					if (conn != null)
						conn.close();
				} catch (SQLException ex) {
					KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
				}
			}

		}else{ // Flatfile!
			String temptimeStr;
			if(tempTime <= 0)
				temptimeStr = "0";
			else
				temptimeStr = temptime.toString();
			try
			{
				BufferedWriter banlist = new BufferedWriter(new FileWriter("plugins/KiwiAdmin/banlist.txt",true));
				banlist.newLine();
				banlist.write(p+">>"+reason+">>"+kicker+">>"+time+">>"+temptimeStr);
				banlist.close();
			}
			catch(IOException e)          
			{
				KiwiAdmin.log.log(Level.SEVERE,"KiwiAdmin: Couldn't write to banlist.txt");
			}
		}
	}

	/*
	 * Get ban reason
	 */
	public static String getReason(String p){

		if(KiwiAdmin.useMysql){
			Connection conn = SQLConnection.getSQLConnection();
			PreparedStatement ps = null;
			ResultSet rs = null;	
			try {
				ps = conn.prepareStatement("SELECT * FROM " + KiwiAdmin.mysqlTable + " WHERE name = ?");
				ps.setString(1, p);
				rs = ps.executeQuery();
				while (rs.next()){
					String reason = rs.getString("reason");
					return reason;
				}
			} catch (SQLException ex) {
				KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
			} finally {
				try {
					if (ps != null)
						ps.close();
					if (conn != null)
						conn.close();
				} catch (SQLException ex) {
					KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
				}
			}
			return null;
		}else{
			try{
				BufferedReader in = new BufferedReader(new FileReader("plugins/KiwiAdmin/banlist.txt"));
				String data = null;

				while ((data = in.readLine()) != null){
					//Checking for blank lines
					if (!data.startsWith("#")){
						if (data.trim().toLowerCase().startsWith(p.toLowerCase()) && data.length()>0){

							String[] values = data.split(">>");

							return values[1];

						}
					}

				}
				in.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return null;
	}
}
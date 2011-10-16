package se.kiwike.yottabyte;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.util.config.Configuration;

public class MySQLDatabase{

	KiwiAdmin plugin;

	public static Connection getSQLConnection() {
		Configuration Config = new Configuration(new File("plugins/KiwiAdmin/config.yml"));
		Config.load();
		String mysqlDatabase = Config.getString("mysql-database","jdbc:mysql://localhost:3306/minecraft");
		String mysqlUser = Config.getString("mysql-user","root");
		String mysqlPassword = Config.getString("mysql-password","root");

		try {

			return DriverManager.getConnection(mysqlDatabase + "?autoReconnect=true&user=" + mysqlUser + "&password=" + mysqlPassword);
		} catch (SQLException ex) {
			KiwiAdmin.log.log(Level.SEVERE, "Unable to retreive connection", ex);
		}
		return null;
	}

	public void initialize(KiwiAdmin plugin){
		this.plugin = plugin;
		Connection conn = getSQLConnection();
		String mysqlTable = plugin.getConfiguration().getString("mysql-table");
		if (conn == null) {
			KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Could not establish SQL connection. Disabling KiwiAdmin");
			plugin.getServer().getPluginManager().disablePlugin(plugin);
			return;
		} else {

			PreparedStatement ps = null;
			ResultSet rs = null;
			try {

				ps = conn.prepareStatement("SELECT * FROM " + mysqlTable + " WHERE (type = 0 OR type = 1) AND (temptime > ? OR temptime = 0)");
				ps.setLong(1, System.currentTimeMillis()/1000);
				rs = ps.executeQuery();
				while (rs.next()){
					String pName = rs.getString("name").toLowerCase();
					long pTime = rs.getLong("temptime");
					plugin.bannedPlayers.add(pName);
					if(pTime != 0){
						plugin.tempBans.put(pName,pTime);
					}
					if(rs.getInt("type") == 1){
						System.out.println("Found IP ban!");
						String ip = getAddress(pName);
						plugin.bannedIPs.add(ip);
					}
				}
			} catch (SQLException ex) {
				KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
			} finally {
				try {
					if (ps != null)
						ps.close();
					if (rs != null)
						rs.close();
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

	public String getAddress(String pName) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("SELECT * FROM players WHERE name = ?");
			ps.setString(1, pName);
			rs = ps.executeQuery();
			while (rs.next()){
				String ip = rs.getString("lastip");
				return ip;
			}
		} catch (SQLException ex) {
			KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
				if (rs != null)
					rs.close();
			} catch (SQLException ex) {
				KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return null;
	}

	public boolean removeFromBanlist(String player) {

		String mysqlTable = plugin.getConfiguration().getString("mysql-table");

		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("DELETE FROM " + mysqlTable + " WHERE name = ? ORDER BY time DESC LIMIT 1");
			ps.setString(1, player);
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
		return true;

	}

	public void addPlayer(String player, String reason, String kicker, long tempTime , int type){

		String mysqlTable = plugin.getConfiguration().getString("mysql-table");

		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("INSERT INTO " + mysqlTable + " (name,reason,admin,time,temptime,type) VALUES(?,?,?,?,?,?)");
			ps.setLong(5, tempTime);
			ps.setString(1, player);
			ps.setString(2, reason);
			ps.setString(3, kicker);
			ps.setLong(4, System.currentTimeMillis()/1000);
			ps.setLong(6, type);
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
	}

	public String getBanReason(String player) {
		Connection conn = getSQLConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String mysqlTable = plugin.getConfiguration().getString("mysql-table");
		try {
			ps = conn.prepareStatement("SELECT * FROM " + mysqlTable + " WHERE name = ?");
			ps.setString(1, player);
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
	}

	public boolean matchAddress(String player, String ip) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("SELECT lastip FROM players WHERE name = ? AND lastip = ?");
			ps.setString(1, player);
			ps.setString(2, ip);
			rs = ps.executeQuery();
			while(rs.next()){
				return true;
			}
		} catch (SQLException ex) {
			KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
				if (rs != null)
					rs.close();
			} catch (SQLException ex) {
				KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return false;
	}

	public void updateAddress(String p, String ip) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			System.out.println("trying to update address.");
			conn = getSQLConnection();
			ps = conn.prepareStatement("UPDATE players SET lastip = ? WHERE name = ?");
			ps.setString(1, ip);
			ps.setString(2, p);
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
	}

	public List<EditBan> listRecords(String name, CommandSender sender) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("SELECT * FROM banlist WHERE name = ? ORDER BY time DESC LIMIT 10");
			ps.setString(1, name);
			rs = ps.executeQuery();
			List<EditBan> bans = new ArrayList<EditBan>();
			while(rs.next()){
				bans.add(new EditBan(rs.getInt("id"),rs.getString("name"),rs.getString("reason"),rs.getString("admin"),rs.getLong("time"),rs.getLong("temptime"),rs.getInt("type")));
			}
			return bans;
		} catch (SQLException ex) {
			KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
				if (rs != null)
					rs.close();
			} catch (SQLException ex) {
				KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return null;
	}

	public EditBan loadFullRecord(String pName) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("SELECT * FROM banlist WHERE name = ? ORDER BY time DESC LIMIT 1");
			ps.setString(1, pName);
			rs = ps.executeQuery();
			while (rs.next()){
				return new EditBan(rs.getInt("id"),rs.getString("name"),rs.getString("reason"),rs.getString("admin"),rs.getLong("time"),rs.getLong("temptime"),rs.getInt("type"));
			}
		} catch (SQLException ex) {
			KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
				if (rs != null)
					rs.close();
			} catch (SQLException ex) {
				KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return null;
	}

	public EditBan loadFullRecordFromId(int id) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("SELECT * FROM banlist WHERE id = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next()){
				return new EditBan(rs.getInt("id"),rs.getString("name"),rs.getString("reason"),rs.getString("admin"),rs.getLong("time"),rs.getLong("temptime"),rs.getInt("type"));
			}
		} catch (SQLException ex) {
			KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
				if (rs != null)
					rs.close();
			} catch (SQLException ex) {
				KiwiAdmin.log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return null;
	}

	public void saveFullRecord(EditBan ban){

		String mysqlTable = plugin.getConfiguration().getString("mysql-table");

		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("UPDATE " + mysqlTable + " SET name = ?, reason = ?, admin = ?, time = ?, temptime = ?, type = ? WHERE id = ?");
			ps.setLong(5, ban.endTime);
			ps.setString(1, ban.name);
			ps.setString(2, ban.reason);
			ps.setString(3, ban.admin);
			ps.setLong(4, ban.time);
			ps.setLong(6, ban.type);
			ps.setInt(7, ban.id);
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
	}

}

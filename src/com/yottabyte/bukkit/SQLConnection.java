package com.yottabyte.bukkit;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.logging.Level;

public class SQLConnection {

    public static Connection getSQLConnection() {
    	
        try {
        	
            return DriverManager.getConnection(LoadSettings.mysqlDatabase + "?autoReconnect=true&user=" + LoadSettings.mysqlUser + "&password=" + LoadSettings.mysqlPassword);
        } catch (SQLException ex) {
            KiwiAdmin.log.log(Level.SEVERE, "Unable to retreive connection", ex);
        }
        return null;
    }
	
}

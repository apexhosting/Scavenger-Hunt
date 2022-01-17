package dev.geri.scavenger.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Database {

    private final JavaPlugin plugin;
    private final Logger logger;
    private static Connection con;
    private boolean isSetup = false;

    public Database(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;

        try {
            this.getConnection();
        } catch (SQLException | ClassNotFoundException exception) {
            logger.severe("There was an error getting the database connection: " + exception.getMessage());
        }
    }

    private void getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().getPath() + File.separator + plugin.getName() + ".db");
        this.setup();
    }

    private void setup() throws SQLException {

        // Check and setup tables if not found
        if (!isSetup) {

            HashMap<String, String> tables = new HashMap<>();

            for (Map.Entry<String, String> table : tables.entrySet()) {

                // Check if it already exists
                PreparedStatement statement = con.prepareStatement("SELECT name FROM sqlite_master WHERE type=? AND name=?");
                statement.setString(1, "table");
                statement.setString(2, table.getKey());
                if (statement.executeQuery().next()) continue;

                // Create table
                try {
                    con.prepareStatement(table.getValue()).execute();
                } catch (Exception exception) {
                    logger.severe("There was an issue setting up table: " + table.getKey() + ": " + exception.getMessage());
                    continue;
                }

                logger.info("Successfully set up database table: " + table.getKey() + "!");

            }

            this.isSetup = true;
        }

    }

    private PreparedStatement createPreparedStatement(String sql, Object... arguments) throws SQLException, ClassNotFoundException {
        if (con == null) getConnection();

        PreparedStatement statement = con.prepareStatement(sql);

        int i = 1;
        if (arguments != null) {
            for (Object argument : arguments) {
                if (argument == null) {
                    i++;
                    continue;
                }

                try {
                    if (argument instanceof Integer) statement.setInt(i, Integer.parseInt(String.valueOf(argument)));
                    else if (argument instanceof Long) statement.setLong(i, Long.parseLong(String.valueOf(argument)));
                    else if (argument instanceof Boolean) statement.setBoolean(i, Boolean.parseBoolean(String.valueOf(argument)));
                    else if (argument instanceof String) statement.setString(i, String.valueOf(argument));
                    else statement.setObject(i, argument);
                } catch (Exception exception) {
                    logger.severe("There was an error setting one of the values for a database operation: " + exception.getMessage());
                }

                i++;
            }
        }

        return statement;
    }

}

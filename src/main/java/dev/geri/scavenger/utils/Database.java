package dev.geri.scavenger.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

            HashMap<String, String> tables = new HashMap<>() {{
                put("return_blocks", "CREATE TABLE IF NOT EXISTS return_blocks(id varchar PRIMARY KEY, world varchar, x integer, y integer, z integer)");
            }};

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

    public ArrayList<Block> getReturnBlocks() throws SQLException, ClassNotFoundException {
        if (con == null) getConnection();

        ResultSet resultSet = this.createPreparedStatement("SELECT * FROM return_blocks").executeQuery();

        ArrayList<Block> returnBlocks = new ArrayList<>();

        while (resultSet.next()) {
            int x = resultSet.getInt("x");
            int y = resultSet.getInt("y");
            int z = resultSet.getInt("z");

            Block block;
            try {
                block = Bukkit.getWorld(UUID.fromString(resultSet.getString("world"))).getBlockAt(x, y, z);
            } catch (Exception exception) {
                logger.warning("Unable to load a return block at X: " + x + " Y: " + y + " Z: " + z + ", skipping!");
                continue;
            }

            if (block.getType() == Material.AIR) {
                logger.warning("Unable to load a return block at X: " + x + " Y: " + y + " Z: " + z + ", skipping!"); // Todo: actually remove these
                continue;
            }

            returnBlocks.add(block);
        }

        return returnBlocks;
    }

    public void addReturnBlock(Block block) throws SQLException, ClassNotFoundException {
        if (con == null) getReturnBlocks();

        String worldId = block.getWorld().getUID().toString();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        this.createPreparedStatement("INSERT INTO return_blocks VALUES(?,?,?,?,?)", String.format("%s_%s_%s_%s", worldId, x, y, z), worldId, x, y, z).execute();
    }

    public void removeReturnBlock(Block block) throws SQLException, ClassNotFoundException {
        if (con == null) getReturnBlocks();

        String worldId = block.getWorld().getUID().toString();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        this.createPreparedStatement("DELETE from return_blocks WHERE id=?", String.format("%s_%s_%s_%s", worldId, x, y, z)).execute();
    }

}

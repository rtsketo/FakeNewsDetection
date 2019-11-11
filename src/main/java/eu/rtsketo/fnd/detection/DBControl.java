package eu.rtsketo.fnd.detection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// Database controlling class
public class DBControl {
    private Map<String, double[]> readCache = new HashMap<>();

    private String[] types = { "unigr", "unien", "bigr",
            "bien", "ttgr", "tten", "tqgr", "tqen",
            "syngr", "synen", "spell" };
    
    private final int numMeth = types.length;
    private int[] sumGood = new int[numMeth];
    private int[] sumFake = new int[numMeth];
    private boolean dbChanged = true;
    private int minValue = 0;
    private Connection conn;
        
    DBControl(String file) throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:"+file);
        Statement query = conn.createStatement();
        
        // The first batch of queries make accessing and 
        // editing the database a lot faster at the cost
        // of potential data loss of unsaved changes in 
        // a case of system failure.
        
        // main.case_size sets database's cache to 100MiB,
        // in case of low system memory it should be changed.
        query.execute("PRAGMA main.cache_size = -100000");
    
        // locking_mode grants exclusive access to this
        // connection, the database can't be accessed twice.
        query.execute("PRAGMA locking_mode = EXCLUSIVE");

        // journal_mode is set to memory, changes made to
        // the database are saved temporary on RAM which
        // saves a lot of HDD reads and writes.
        query.execute("PRAGMA journal_mode = MEMORY");
        
        // synchronous set to OFF makes commits a lot
        // faster at the cost of data corruption in
        // case of operating system failure.
        query.execute("PRAGMA synchronous = OFF");
        
        // Initialization of tables that are used by the
        // application, if they don't already exist.
        for (int t=0; t<types.length-3; t++)
            query.execute("CREATE TABLE IF NOT EXISTS " + types[t] +
                    " (word VARCHAR(255) UNIQUE, good INT, fake INT)");
        query.execute("CREATE TABLE IF NOT EXISTS spell "
                + "(wgood INT, wfake INT, good INT, fake INT)");
        query.executeUpdate("INSERT INTO spell VALUES (0,0,0,0)");
        query.close();
        
        // Getting the total number of words and PoS.
        updateMax();
    }
    
    void addWord(String word, int count,
                 boolean veracity, String table) {
        
        int good = 0;
        int fake = 0;
        dbChanged = true;
        
        try {
            word = word.toLowerCase();          
            if (veracity) good += count;
            else fake += count;
            
            updateQuery(table,word,good,fake);
        
        } catch (SQLException ex) {
            Logger.getLogger(DBControl.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }
    
    void addSpell(int errorCount,
                  int wordCount, boolean veracity) {
        int good = 0;
        int fake = 0;
        int wgood = 0;
        int wfake = 0;
        
        try (Statement query = conn.createStatement()) {
        
        if (veracity) {
            good += errorCount;
            wgood += wordCount;
        } else {
            fake += errorCount;
            wfake += wordCount;
        }
        
        query.executeUpdate("UPDATE spell SET" +
                " wgood = wgood + " + wgood + 
                ", wfake = wfake + " + wfake +
                ", fake = fake + " + fake + 
                ", good = good + " + good);
        
        } catch (SQLException ex) {
            Logger.getLogger(DBControl.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }
    
    public double[] getSpell() {
        int wgood = 0;
        int good = 0;
        int fake = 0;
        int wfake = 0;
        
        try (Statement query = conn.createStatement()){
            
            ResultSet res = query
                    .executeQuery("SELECT * FROM spell");
            
            fake = res.getInt("fake");
            good = res.getInt("good");
            wgood = res.getInt("wgood");
            wfake = res.getInt("wfake");
            query.close();
            
            return new double[] {
                (double)good/wgood,
                (double)fake/wfake
            };
        } catch (SQLException ex) {
            Logger.getLogger(DBControl.class.getName())
                    .log(Level.SEVERE, null, ex);
            return null;
        }
        
    }
    
    double[] getWord(String word, String table) {
        int type = Arrays.asList(types).indexOf(table);
        double[] result;
        int good = 0;
        int fake = 0;
        
        try {
            
            word = word.toLowerCase();            
            if (dbChanged) updateMax();
            
            result = readCache.get(word);
            if (result != null) return result;
            
            PreparedStatement pstat;
            String sqlSelect = "SELECT * FROM $ WHERE word = ?";
            pstat = conn.prepareStatement(
                    sqlSelect.replace("$",table));
            pstat.setString(1, word);
            
            ResultSet res = pstat.executeQuery();
            if (res.isBeforeFirst()) {
                fake = res.getInt("fake");        
                good = res.getInt("good");
            } pstat.close();
            
            result = new double[] {
                (double)good/sumGood[type],
                (double)fake/sumFake[type]
            };
            
            readCache.putIfAbsent(word, result);
            return result;
            
        } catch (SQLException ex) {
            Logger.getLogger(DBControl.class.getName())
                    .log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private void updateMax() throws SQLException {        
        for (int i=0; i<types.length-3; i++) {
            Statement query = conn.createStatement();
            ResultSet res = query.executeQuery(
                    "SELECT SUM(good), SUM(fake)"
                            + " FROM "+ types[i]);
            sumGood[i] = res.getInt("SUM(good)");
            sumFake[i] = res.getInt("SUM(fake)");
            query.close();
        }
        readCache.clear();
        dbChanged = false;
    }
    
    private void insertQuery(String table,
            String word) throws SQLException {
        PreparedStatement pstat;
        String sqlInsert = "INSERT OR IGNORE INTO $ VALUES (?,?,?)";
        pstat = conn.prepareStatement(
                sqlInsert.replace("$",table));
        pstat.setString(1, word);
        pstat.setInt(2, minValue);
        pstat.setInt(3, minValue);
        pstat.executeUpdate();
        pstat.close();
    }
    
    private void updateQuery(String table,
            String word, int good, int fake)
            throws SQLException {
        
        conn.setAutoCommit(false);
        insertQuery(table,word);
        
        PreparedStatement pstat;
        String sqlUpdate = "UPDATE $ SET good = good + ?,"
                + " fake = fake + ? WHERE word IS ?";
        pstat = conn.prepareStatement(
                sqlUpdate.replace("$", table));
        pstat.setInt(1, good);
        pstat.setInt(2, fake);
        pstat.setString(3, word);
        pstat.executeUpdate();
        pstat.close();
        
        conn.commit();
        conn.setAutoCommit(true);
    }
    
    void closeConnection()
            throws SQLException {
        conn.close();
    }
    
    void setMinValue(int val) {
        minValue = val;
    }
    public String[] getTypes() {
        return types;
    }
}

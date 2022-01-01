package cat.nyaa.playtimetracker;

import com.google.common.io.LineReader;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {
    private final File dbFile;
    private final Map<UUID, DatabaseRecord> recordMap;
    private final File recurrenceFile;
    public Map<UUID, Map<String, Long>> recurrenceMap; //Map<playerId, Map<ruleName, accumulatedTime-ms>>

    public DatabaseManager(File db_file, File recurFile) {
        dbFile = db_file;
        recurrenceFile = recurFile;
        recordMap = new HashMap<>();
        if (dbFile.isFile()) { // Read in
            ConfigurationSection cfg = YamlConfiguration.loadConfiguration(dbFile);
            for (String uuid_str : cfg.getKeys(false)) {
                ConfigurationSection sec = cfg.getConfigurationSection(uuid_str);
                UUID id = UUID.fromString(uuid_str);
                if (sec != null) {
                    recordMap.put(id, DatabaseRecord.deserialize(id, sec));
                }
            }
        }
        setupRecurrenceMap();
    }

    public DatabaseManager(File db_file, File old_db_file, File recurFile) {
        dbFile = db_file;
        recurrenceFile = recurFile;
        recordMap = new HashMap<>();
        if (old_db_file.isFile()) {
            FileReader fr = null;
            LineReader lr;
            try {
                fr = new FileReader(old_db_file);
                lr = new LineReader(fr);
                String line;
                while ((line = lr.readLine()) != null) {
                    String[] tmp = line.split(" ", 2);
                    if (tmp.length != 2) continue;
                    UUID a;
                    try {
                        a = UUID.fromString(tmp[0]);
                    } catch (IllegalArgumentException ex) {
                        PTT.log("Illegal data line: " + line);
                        continue;
                    }
                    recordMap.put(a, DatabaseRecord.deserialize_legacy(a, tmp[1]));
                }
            } catch (IOException ex) {
                PTT.log("Failed to parse legacy database");
                ex.printStackTrace();
            } finally {
                try {
                    if (fr != null) fr.close();
                } catch (IOException ex) {
                    PTT.log("Failed to parse legacy database");
                    ex.printStackTrace();
                }
            }
        }
        setupRecurrenceMap();
    }

    private static synchronized void synchronizeSave(final File dbFile, final Map<UUID, DatabaseRecord> records,
                                                     final File recFile, final Map<UUID, Map<String, Long>> recurrenceMap) {
        YamlConfiguration cfg = new YamlConfiguration();
        for (UUID id : records.keySet()) {
            records.get(id).serialize(cfg.createSection(id.toString()));
        }
        try {
            cfg.save(dbFile);
        } catch (IOException ex) {
            System.out.print(">>>>>> PlayTimeTracker Database Emergency Dump <<<<<<\n" +
                    cfg.saveToString() +
                    "\n>>>>>> Emergency dump ends <<<<<<"
            );
            ex.printStackTrace();
        }

        cfg = new YamlConfiguration();
        for (UUID id : recurrenceMap.keySet()) {
            if (recurrenceMap.get(id).size() <= 0) continue;
            cfg.createSection(id.toString(), recurrenceMap.get(id));
        }
        try {
            cfg.save(recFile);
        } catch (IOException ex) {
            System.out.print(">>>>>> PlayTimeTracker Database Emergency Dump <<<<<<\n" +
                    cfg.saveToString() +
                    "\n>>>>>> Emergency dump ends <<<<<<"
            );
            ex.printStackTrace();
        }
    }

    private void setupRecurrenceMap() {
        recurrenceMap = new HashMap<>();
        if (recurrenceFile.isFile()) {
            ConfigurationSection cfg = YamlConfiguration.loadConfiguration(recurrenceFile);
            for (String uuid_str : cfg.getKeys(false)) {
                ConfigurationSection sec = cfg.getConfigurationSection(uuid_str);
                UUID id = UUID.fromString(uuid_str);
                Map<String, Long> ruleMap = new HashMap<>();
                for (String ruleName : sec.getKeys(false)) {
                    ruleMap.put(ruleName, sec.getLong(ruleName));
                }
                recurrenceMap.put(id, ruleMap);
            }
        }
    }

    public void save() {
        final Map<UUID, DatabaseRecord> clonedMap = new HashMap<>();
        final File clonedFile = new File(dbFile.toURI());
        for (Map.Entry<UUID, DatabaseRecord> entry : recordMap.entrySet()) {
            clonedMap.put(entry.getKey(), entry.getValue().clone());
        }
        final File clonedRecFile = new File(recurrenceFile.toURI());
        final Map<UUID, Map<String, Long>> clonedRecurrence = new HashMap<>();
        for (UUID id : recurrenceMap.keySet()) {
            Map<String, Long> tmp = new HashMap<>(recurrenceMap.get(id));
            clonedRecurrence.put(id, tmp);
        }
        new Thread(() -> synchronizeSave(clonedFile, clonedMap, clonedRecFile, clonedRecurrence)).start();
    }

    public void synchronizeSave() {
        synchronizeSave(dbFile, recordMap, recurrenceFile, recurrenceMap);
    }

    public DatabaseRecord getRecord(UUID id) {
        return recordMap.get(id);
    }

    public void createRecord(UUID id, ZonedDateTime time) {
        recordMap.put(id, new DatabaseRecord(id, time));
    }

    public Map<UUID, DatabaseRecord> getAllRecords() {
        return recordMap;
    }

    public void setRecurrenceRule(String ruleName, UUID playerId) {
        Map<String, Long> tmp = recurrenceMap.computeIfAbsent(playerId, k -> new HashMap<>());
        tmp.put(ruleName, 0L);
    }
}

package de.ftscraft.ftskampf.main;

import de.ftscraft.ftsengine.main.Engine;
import de.ftscraft.ftskampf.commands.*;
import de.ftscraft.ftskampf.damageCalculators.DiceManager;
import de.ftscraft.ftskampf.db.DBManager;
import de.ftscraft.ftskampf.db.EffectManager;
import de.ftscraft.ftskampf.db.HpManager;
import de.ftscraft.ftskampf.db.SpellManager;
import de.ftscraft.ftskampf.listeners.InventoryListener;
import de.ftscraft.ftskampf.listeners.JoinListener;
import de.ftscraft.ftskampf.utils.HealthRunner;
import de.ftscraft.ftskampf.utils.LivingEffectRunner;
import de.ftscraft.ftskampf.utils.MappedInventory;
import de.ftscraft.ftskampf.utils.Race;
import de.ftscraft.ftskampf.utils.exceptions.RaceDoNotExistException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class FTSKampf extends JavaPlugin {

    private static FTSKampf plugin;
    private static DiceManager diceManager;
    public static List<Inventory> kampfInventorys;
    public static List<MappedInventory> spellChooseInventory;//contains inventory and zid mapping
    public static List<MappedInventory> spellCastInventory;
    public static HashMap<MappedInventory, Player> spellTargetInventory;
    public static HashMap<Inventory, Player> targetInventorys;
    public static List<String> raceList;
    private Engine engine;
    private DBManager dbManager;
    private HpManager hpManager;
    private EffectManager effectManager;
    private SpellManager spellManager;

    @Override
    public void onEnable() {
        plugin = this;
        kampfInventorys = new ArrayList<>();
        spellChooseInventory = new ArrayList<>();
        spellCastInventory = new ArrayList<>();
        spellTargetInventory = new HashMap<>();
        targetInventorys = new HashMap<>();

        if (!doConfigExist()) createInitialConfig();
        raceList = getRaceList();

        engine = (Engine) getServer().getPluginManager().getPlugin("FTSEngine");
        effectManager = new EffectManager();
        effectManager.init();
        spellManager = new SpellManager();
        try {
            spellManager.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        dbManager = new DBManager();
        hpManager = new HpManager();
        diceManager = new DiceManager();

        getCommand("kampf").setExecutor(new CMDKampf());
        getCommand("hp").setExecutor(new CMDHp());
        getCommand("heilen").setExecutor(new CMDHeal());
        getCommand("kampfskill").setExecutor(new CMDKampfskill());
        getCommand("hurt").setExecutor(new CMDHurt());
        getCommand("react").setExecutor(new CMDReact());
        getCommand("magie").setExecutor(new CMDMagie());
        getCommand("ftskampfdb").setExecutor(new CMDftskampfdb());

        InventoryListener inventoryListener = new InventoryListener();
        getServer().getPluginManager().registerEvents(inventoryListener, plugin);
        JoinListener joinListener = new JoinListener();
        getServer().getPluginManager().registerEvents(joinListener, plugin);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new HealthRunner(), 20L, 20L);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new LivingEffectRunner(), 20L, 20L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static FTSKampf getPlugin() {
        return plugin;
    }
    public DBManager getDB() {
        return dbManager;
    }
    public Engine getEngine() {
        return engine;
    }
    public HpManager getHpManager() { return hpManager;}
    public DiceManager getDiceManager() { return diceManager;}
    public EffectManager getEffectManager() { return effectManager;}
    public SpellManager getSpellManager() { return spellManager;}

    public Race getRaceOrDefault(Player player) {
        FileConfiguration config = plugin.getConfig();
        Race race;
        if(!engine.hasAusweis(player)) {
            return new Race(config.getString("Races.Default"));
        }
        try {
            race = getRace(player);
        } catch (RaceDoNotExistException e) {
            return new Race(config.getString("Races.Default"));
        }
        return race;
    }

    public Race getRace(Player player) throws RaceDoNotExistException {
        String race = findRace(engine.getAusweis(player).getRace());
        FileConfiguration config = plugin.getConfig();
        if ((config.getString("Races." + race) == null)) {
            throw new RaceDoNotExistException("Rasse nicht gefunden!");
        }
        return new Race(race);
    }

    private String findRace(String race) {
        int found = 0;
        String foundRace = null;
        for(String existingRace : raceList) {
            if(race.toLowerCase().contains(existingRace.toLowerCase())) {
                found++;
                foundRace = existingRace;
            }
        }
        if(found == 1) {
            return foundRace;
        }
        return null;
    }

    private boolean doConfigExist() {
        File file = new File(getPath() + "/" + "config.yml");
        return file.exists();
    }

    private File getPath() {
        String path = System.getProperty("user.dir");
        File directory = new File(path + "/plugins/" + plugin.getName());
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    private List<String> getRaceList() {
        List<String> raceList = new ArrayList<>();
        FileConfiguration config = plugin.getConfig();
        for(String race : config.getStringList("Races.List")) {
            raceList.add(race);
        }
        return raceList;
    }

    private void createInitialConfig() {
        FileConfiguration config = plugin.getConfig();

        config.set("Testmode", false);

        config.set("Permissions.UseDices.Name", "ftskampf.useDices");
        config.set("Permissions.UseDices.Required", false);
        config.set("Permissions.SetSkills.Name", "ftskampf.setSkills");
        config.set("Permissions.SetSkills.Required", false);
        config.set("Permissions.ResetSkills.Name", "ftskampf.resetSkills");
        config.set("Permissions.ResetSkills.Required", false);
        config.set("Permissions.ResetOtherSkills.Name", "ftskampf.resetOtherSkills");
        config.set("Permissions.ResetOtherSkills.Required", false);
        config.set("Permissions.Heal.Name", "ftskampf.heal");
        config.set("Permissions.Heal.Required", false);
        config.set("Permissions.DBAdmin.Name", "ftskampf.dbadmin");
        config.set("Permissions.DBAdmin.Required", true);

        config.set("Permissions.ChooseSpells.Name", "ftskampf.chooseSpells");
        config.set("Permissions.ChooseSpells.Required", false);
        config.set("Permissions.ResetSpells.Name", "ftskampf.resetSpells");
        config.set("Permissions.ResetSpells.Required", false);
        config.set("Permissions.ResetSpellsOther.Name", "ftskampf.resetOtherSpells");
        config.set("Permissions.ResetSpellsOther.Required", false);

        config.set("MaxSpells", 4);
        config.set("EffectLifetime", 30);

        config.set("DiceChatRange", 20);

        config.set("Dice.Melee.Max", 100);
        config.set("Dice.Melee.MaxSkillable", 100);
        config.set("Dice.Distance.Max", 100);
        config.set("Dice.Distance.MaxSkillable", 100);
        config.set("Dice.Magic.Max", 100);
        config.set("Dice.Magic.MaxSkillable", 100);
        config.set("Dice.Agility.Max", 100);
        config.set("Dice.Agility.MaxSkillable", 50);
        config.set("Dice.Action.Max", 100);

        config.set("Health.RegenRate", 30);
        config.set("Health.RegenPoints", 1);
        config.set("Health.SkillMultiplier", 5);
        config.set("MinimumDamage", 5);

        config.set("Armor.Leather", 20);
        config.set("Armor.Gold", 25);
        config.set("Armor.Chain", 25);
        config.set("Armor.Iron", 35);
        config.set("Armor.Diamond", 55);
        config.set("Armor.Netherite", 70);

        config.set("Armor.Shield", 8);

        config.set("Weapon.Melee.Wood", 10);
        config.set("Weapon.Melee.Stone", 15);
        config.set("Weapon.Melee.Gold", 20);
        config.set("Weapon.Melee.Iron", 35);
        config.set("Weapon.Melee.Diamond", 50);
        config.set("Weapon.Melee.Netherite", 65);
        config.set("Weapon.Melee.Trident", 40);

        config.set("Weapon.Melee.AxeDamageGain", 5);
        config.set("Weapon.Melee.AxePrecisionLoss", 10);

        config.set("Weapon.Distance.Bow", 30);
        config.set("Weapon.Distance.Crossbow", 45);
        config.set("Weapon.Distance.Trident", 40);

        config.set("Weapon.Distance.CrossbowPrecisionLoss", 10);

        config.set("Races.Default", "Mensch");
        config.set("Races.List", new String[]{"Mensch", "Elf", "Zwerg", "Ork"});

        config.set("Races.Mensch.MName", "Mensch");
        config.set("Races.Mensch.FName", "Menschenfrau");
        config.set("Races.Mensch.points", 100);
        config.set("Races.Mensch.InitialValues.Melee", 10);
        config.set("Races.Mensch.InitialValues.Distance", 15);
        config.set("Races.Mensch.InitialValues.Magic", -1);
        config.set("Races.Mensch.InitialValues.Agility", 8);
        config.set("Races.Mensch.InitialValues.Health", 100);

        config.set("Races.Elf.MName", "Elf");
        config.set("Races.Elf.FName", "Elfe");
        config.set("Races.Elf.points", 120);
        config.set("Races.Elf.InitialValues.Melee", 8);
        config.set("Races.Elf.InitialValues.Distance", 10);
        config.set("Races.Elf.InitialValues.Magic", 15);
        config.set("Races.Elf.InitialValues.Agility", 9);
        config.set("Races.Elf.InitialValues.Health", 80);

        config.set("Races.Zwerg.MName", "Zwerg");
        config.set("Races.Zwerg.FName", "Zwergin");
        config.set("Races.Zwerg.points", 120);
        config.set("Races.Zwerg.InitialValues.Melee", 12);
        config.set("Races.Zwerg.InitialValues.Distance", 12);
        config.set("Races.Zwerg.InitialValues.Magic", 8);
        config.set("Races.Zwerg.InitialValues.Agility", 7);
        config.set("Races.Zwerg.InitialValues.Health", 120);

        config.set("Races.Ork.MName", "Ork");
        config.set("Races.Ork.FName", "Orkin");
        config.set("Races.Ork.points", 120);
        config.set("Races.Ork.InitialValues.Melee", 18);
        config.set("Races.Ork.InitialValues.Distance",5);
        config.set("Races.Ork.InitialValues.Magic", 5);
        config.set("Races.Ork.InitialValues.Agility", 6);
        config.set("Races.Ork.InitialValues.Health", 150);
        plugin.saveConfig();
    }
}

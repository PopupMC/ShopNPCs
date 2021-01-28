package com.popupmc.shopnpcs;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class ShopNpcs extends JavaPlugin {
    @Override
    public void onEnable() {
        plugin = this;

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if(!setupCitizens()) {
            getLogger().severe(String.format("[%s] - Disabled due to Citizens 2.0 not found or not enabled!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("ShopNpcs is enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ShopNpcs is disabled");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            return false;
        }

        econ = rsp.getProvider();
        return true;
    }

    private boolean setupCitizens() {
        if(getServer().getPluginManager().getPlugin("Citizens") == null ||
                !Objects.requireNonNull(getServer().getPluginManager().getPlugin("Citizens")).isEnabled()) {
            return false;
        }

        //Register your trait with Citizens.
        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(Packager.class).withName("packager"));
        return true;
    }

    static JavaPlugin plugin;
    public static Economy econ = null;

    public static String udbLore = "Unused Disposable Box";
    public static String dbLore = "Disposable Box";
    public static String costLore = "Cost:";
}

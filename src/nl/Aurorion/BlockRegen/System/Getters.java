package nl.Aurorion.BlockRegen.System;

import nl.Aurorion.BlockRegen.Main;

public class Getters {

    private Main main;

    public Getters(Main instance) {
        this.main = instance;
    }

    // Getter Settings.yml
    public boolean updateChecker() {
        if (main.getFiles().getSettings().get("Update-Checker") != null) {
            return main.getFiles().getSettings().getBoolean("Update-Checker");
        }
        return true;
    }


    public boolean useTowny() {
        return main.getFiles().getSettings().getBoolean("Towny-Support");
    }

    public boolean useGP() {
        return main.getFiles().getSettings().getBoolean("GriefPrevention-Support");
    }

    public boolean dataRecovery() {
        return main.getFiles().getSettings().getBoolean("Data-Recovery");
    }
}

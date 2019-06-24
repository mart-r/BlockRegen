package nl.Aurorion.BlockRegen.BlockFormat;

import nl.Aurorion.BlockRegen.Main;

public class Amount {

    private int fixedValue;

    private int lowValue;
    private int highValue;

    private final boolean fixed;

    public Amount(int low, int high) {

        fixed = false;

        if (low > high) {
            lowValue = high;
            highValue = low;
        } else {
            lowValue = low;
            highValue = high;
        }
    }

    public Amount(int fixed) {
        fixedValue = fixed;
        this.fixed = true;
    }

    private int getRandom() {
        return Main.getInstance().getRandom().nextInt((highValue - lowValue) + 1) + lowValue;
    }

    public int getAmount() {
        return fixed ? fixedValue : getRandom();
    }

    public int low() {
        return lowValue;
    }

    public int high() {
        return highValue;
    }

    public String toString() {
        if (fixed)
            return String.valueOf(fixedValue);
        else
            return lowValue + " - " + highValue;
    }
}

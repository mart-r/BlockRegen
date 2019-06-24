package nl.Aurorion.BlockRegen.BlockFormat;

public class JobRequirement {

    private final String job;
    private final int level;

    public JobRequirement(String job, int level) {
        this.job = job;
        this.level = level;
    }

    public String getJob() {
        return job;
    }

    public int getLevel() {
        return level;
    }
}

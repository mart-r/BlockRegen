package nl.Aurorion.BlockRegen.BlockFormat;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import org.bukkit.entity.Player;

import java.util.List;

public class JobReward {

    private String jobName;
    private double exp;

    private boolean valid = false;

    public boolean isValid() {
        return valid;
    }

    public JobReward(String jobName, double exp) {
        for (Job job : Jobs.getJobs())
            if (job.getName().equalsIgnoreCase(jobName)) {
                this.jobName = job.getName();
                this.valid = true;
            }

        this.exp = exp;
    }

    public void reward(Player player) {
        List<JobProgression> jobs = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression();

        for (JobProgression job : jobs)
            if (job.getJob().getName().equalsIgnoreCase(jobName))
                job.addExperience(exp);
    }
}

package nl.aurorion.blockregen.api;

import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;
import nl.aurorion.blockregen.system.regeneration.struct.RegenerationProcess;
import org.bukkit.event.Cancellable;

/**
 * Event fired before a block is regenerated.
 * Cancelling this event causes the block not to regenerate into another, also deletes the regeneration process.
 */
public class BlockRegenBlockRegenerationEvent extends BlockRegenBlockEvent implements Cancellable {

    @Getter
    @Setter
    private boolean cancelled = false;

    /**
     * Regeneration process responsible for this block.
     */
    @Getter
    private final RegenerationProcess regenerationProcess;

    public BlockRegenBlockRegenerationEvent(RegenerationProcess regenerationProcess) {
        super(regenerationProcess.getBlock(), regenerationProcess.getPreset());
        this.regenerationProcess = regenerationProcess;
    }


    /*
     * Shortcuts.
     * */

    public TargetMaterial getRegenerateInto() {
        return regenerationProcess.getRegenerateInto();
    }

    public void setRegenerateInto(TargetMaterial material) {
        regenerationProcess.setRegenerateInto(material);
    }
}
package nl.aurorion.blockregen.system.preset.struct.material;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.version.api.INodeData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DynamicMaterial {

    @Getter
    @Setter
    private INodeData defaultMaterial;

    private final List<INodeData> valuedMaterials = new ArrayList<>();

    public DynamicMaterial(INodeData defaultMaterial) {
        this.defaultMaterial = defaultMaterial;
    }

    public DynamicMaterial(INodeData defaultMaterial, Collection<INodeData> valuedMaterials) {
        this.defaultMaterial = defaultMaterial;
        this.valuedMaterials.addAll(valuedMaterials);
    }

    public List<INodeData> getValuedMaterials() {
        return Collections.unmodifiableList(valuedMaterials);
    }

    public static DynamicMaterial fromString(BlockRegen plugin, String input) throws IllegalArgumentException {

        if (Strings.isNullOrEmpty(input))
            throw new IllegalArgumentException("Input string cannot be null");

        input = input.trim();

        List<String> materials;
        List<INodeData> valuedMaterials = new ArrayList<>();
        INodeData defaultMaterial = null;

        if (!input.contains(";")) {
            defaultMaterial = plugin.getVersionManager().obtainNodeData().fromString(input);

            if (defaultMaterial == null)
                throw new IllegalArgumentException("Invalid block material " + input);
            return new DynamicMaterial(defaultMaterial);
        }

        materials = Arrays.asList(input.split(";"));

        if (materials.isEmpty())
            throw new IllegalArgumentException("Dynamic material " + input + " doesn't have the correct syntax");

        if (materials.size() == 1) {
            defaultMaterial = plugin.getVersionManager().obtainNodeData().fromString(materials.get(0));

            if (defaultMaterial == null)
                throw new IllegalArgumentException("Invalid block material " + materials.get(0));

            return new DynamicMaterial(defaultMaterial);
        }

        int total = 0;

        for (String material : materials) {

            if (!material.contains(":")) {
                defaultMaterial = plugin.getVersionManager().obtainNodeData().fromMaterialName(material);

                if (defaultMaterial == null)
                    throw new IllegalArgumentException("Invalid block material " + material);

                continue;
            }

            int chance = Integer.parseInt(material.split(":")[1]);
            total += chance;

            INodeData mat = plugin.getVersionManager().obtainNodeData().fromString(material.split(":")[0]);

            if (mat == null)
                continue;

            for (int i = 0; i < chance; i++)
                valuedMaterials.add(mat);
        }

        if (defaultMaterial != null) {
            for (int i = 0; i < (100 - total); i++) valuedMaterials.add(defaultMaterial);
        }

        return new DynamicMaterial(defaultMaterial, valuedMaterials);
    }

    private INodeData pickRandom() {
        INodeData pickedMaterial = valuedMaterials.get(BlockRegen.getInstance().getRandom().nextInt(valuedMaterials.size()));
        return pickedMaterial != null ? pickedMaterial : defaultMaterial;
    }

    @NotNull
    public INodeData get() {
        return valuedMaterials.isEmpty() ? defaultMaterial : pickRandom();
    }
}

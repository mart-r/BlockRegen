package nl.aurorion.blockregen.system.preset.struct.material;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.util.ParseUtil;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DynamicMaterial {

    @Getter
    @Setter
    private TargetMaterial defaultMaterial;

    private final List<TargetMaterial> valuedMaterials = new ArrayList<>();

    public DynamicMaterial(TargetMaterial defaultMaterial) {
        this.defaultMaterial = defaultMaterial;
    }

    public DynamicMaterial(TargetMaterial defaultMaterial, Collection<TargetMaterial> valuedMaterials) {
        this.defaultMaterial = defaultMaterial;
        this.valuedMaterials.addAll(valuedMaterials);
    }

    public List<TargetMaterial> getValuedMaterials() {
        return Collections.unmodifiableList(valuedMaterials);
    }

    public static DynamicMaterial fromString(String input) throws IllegalArgumentException {

        if (Strings.isNullOrEmpty(input))
            throw new IllegalArgumentException("Input string cannot be null");
        if (StringUtils.isNumeric(input)) {
            return mmoItemFromString(Integer.valueOf(input));
        }

        input = input.replace(" ", "").trim().toUpperCase();

        List<String> materials;
        List<TargetMaterial> valuedMaterials = new ArrayList<>();
        XMaterial defaultMaterial = null;

        if (!input.contains(";")) {
            defaultMaterial = ParseUtil.parseMaterial(input, true);

            if (defaultMaterial == null)
                throw new IllegalArgumentException("Invalid block material " + input);
            return new DynamicMaterial(new RegularTargetMaterial(defaultMaterial));
        }

        materials = Arrays.asList(input.split(";"));

        if (materials.isEmpty())
            throw new IllegalArgumentException("Dynamic material " + input + " doesn't have the correct syntax");

        if (materials.size() == 1) {
            defaultMaterial = ParseUtil.parseMaterial(materials.get(0), true);

            if (defaultMaterial == null)
                throw new IllegalArgumentException("Invalid block material " + materials.get(0));

            return new DynamicMaterial(new RegularTargetMaterial(defaultMaterial));
        }

        int total = 0;

        for (String material : materials) {

            if (!material.contains(":")) {
                defaultMaterial = ParseUtil.parseMaterial(material, true);

                if (defaultMaterial == null)
                    throw new IllegalArgumentException("Invalid block material " + material);

                continue;
            }

            int chance = Integer.parseInt(material.split(":")[1]);
            total += chance;

            XMaterial mat = ParseUtil.parseMaterial(material.split(":")[0], true);

            if (mat == null)
                continue;

            for (int i = 0; i < chance; i++)
                valuedMaterials.add(new RegularTargetMaterial(mat));
        }

        if (defaultMaterial != null) {
            for (int i = 0; i < (100 - total); i++) valuedMaterials.add(new RegularTargetMaterial(defaultMaterial));
        }

        return new DynamicMaterial(new RegularTargetMaterial(defaultMaterial), valuedMaterials);
    }

    public static DynamicMaterial mmoItemFromString(int id) {
        return new DynamicMaterial(new MMOTargetMaterial(id));
    }

    private TargetMaterial pickRandom() {
        TargetMaterial pickedMaterial = valuedMaterials.get(BlockRegen.getInstance().getRandom().nextInt(valuedMaterials.size()));
        return pickedMaterial != null ? pickedMaterial : defaultMaterial;
    }

    @NotNull
    public TargetMaterial get() {
        return valuedMaterials.isEmpty() ? defaultMaterial : pickRandom();
    }
}

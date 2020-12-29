package nl.aurorion.blockregen;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.UUID;

@UtilityClass
public class BlockSkullUtil {

    private Field blockProfileField;
    private Method metaSetProfileMethod;
    private Field metaProfileField;

    @Deprecated
    public void blockWithName(Block block, String name) {
        notNull(block, "block");
        notNull(name, "name");

        Skull state = (Skull) block.getState();
        state.setOwningPlayer(Bukkit.getOfflinePlayer(name));
        state.update(false, false);
    }

    public void blockWithUuid(Block block, UUID id) {
        notNull(block, "block");
        notNull(id, "id");

        setToSkull(block);
        Skull state = (Skull) block.getState();
        state.setOwningPlayer(Bukkit.getOfflinePlayer(id));
        state.update(false, false);
    }

    public void blockWithUrl(Block block, String url) {
        notNull(block, "block");
        notNull(url, "url");

        blockWithBase64(block, urlToBase64(url));
    }

    public void blockWithBase64(Block block, String base64) {
        notNull(block, "block");
        notNull(base64, "base64");

        if (!(block.getState() instanceof Skull))
            return;

        //setToSkull(block);
        Skull state = (Skull) block.getState();
        mutateBlockState(state, base64);
        state.update(false, false);
    }

    @SuppressWarnings("deprecation")
    private void setToSkull(Block block) {
        try {
            block.setType(Material.valueOf("PLAYER_HEAD"), false);
        } catch (IllegalArgumentException e) {
            block.setType(Material.valueOf("SKULL"), false);
            Skull state = (Skull) block.getState();
            state.setSkullType(SkullType.PLAYER);
            state.update(false, false);
        }
    }

    private void notNull(Object o, String name) {
        if (o == null) {
            throw new NullPointerException(name + " should not be null!");
        }
    }

    private String urlToBase64(String url) {

        URI actualUrl;
        try {
            actualUrl = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String toEncode = "{\"textures\":{\"SKIN\":{\"url\":\"" + actualUrl.toString() + "\"}}}";
        return Base64.getEncoder().encodeToString(toEncode.getBytes());
    }

    private String base64FromProfile(GameProfile profile) {
        for (Property property : profile.getProperties().get("textures")) {
            if (property.getName().equals("textures")) {
                return property.getValue();
            }
        }
        return null;
    }

    private GameProfile makeProfile(String b64) {
        // random uuid based on the b64 string
        UUID id = new UUID(
                b64.substring(b64.length() - 20).hashCode(),
                b64.substring(b64.length() - 10).hashCode()
        );
        GameProfile profile = new GameProfile(id, "aaaaa");
        profile.getProperties().put("textures", new Property("textures", b64));
        return profile;
    }

    @Nullable
    public UUID uuidFromBlock(@NotNull Block block) {
        BlockState state = block.getState();

        if (!(state instanceof Skull))
            return null;

        Skull skull = (Skull) state;
        OfflinePlayer owner = skull.getOwningPlayer();
        return owner == null ? null : owner.getUniqueId();
    }

    @Nullable
    public String base64fromBlock(@NotNull Block block) {
        BlockState state = block.getState();

        if (!(state instanceof Skull))
            return null;

        Skull skull = (Skull) state;
        return queryFromBlockState(skull);
    }

    @Nullable
    private String queryFromBlockState(Skull skull) {
        try {
            if (blockProfileField == null) {
                blockProfileField = skull.getClass().getDeclaredField("profile");
                blockProfileField.setAccessible(true);
            }

            Object obj = blockProfileField.get(skull);

            if (!(obj instanceof GameProfile))
                return null;

            GameProfile profile = (GameProfile) obj;

            return base64FromProfile(profile);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void mutateBlockState(Skull block, String b64) {
        try {
            if (blockProfileField == null) {
                blockProfileField = block.getClass().getDeclaredField("profile");
                blockProfileField.setAccessible(true);
            }
            blockProfileField.set(block, makeProfile(b64));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void mutateItemMeta(SkullMeta meta, String b64) {
        try {
            if (metaSetProfileMethod == null) {
                metaSetProfileMethod = meta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
                metaSetProfileMethod.setAccessible(true);
            }
            metaSetProfileMethod.invoke(meta, makeProfile(b64));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            try {
                if (metaProfileField == null) {
                    metaProfileField = meta.getClass().getDeclaredField("profile");
                    metaProfileField.setAccessible(true);
                }
                metaProfileField.set(meta, makeProfile(b64));

            } catch (NoSuchFieldException | IllegalAccessException ex2) {
                ex2.printStackTrace();
            }
        }
    }
}

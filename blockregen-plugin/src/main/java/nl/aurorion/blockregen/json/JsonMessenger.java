package nl.aurorion.blockregen.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.StringUtil;
import nl.aurorion.blockregen.Reflection;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class JsonMessenger {

    private final BlockRegen plugin;

    private final Gson gson = new GsonBuilder().create();

    public JsonMessenger(BlockRegen plugin) {
        this.plugin = plugin;
    }

    private Class<?> componentClazz;
    private Method aMethod;
    private Constructor<?> packetConstructor;
    private Class<?> messageTypeClazz;

    public void sendJson(Player player, String json) {

        if (componentClazz == null) {
            componentClazz = Reflection.getNMSClass("IChatBaseComponent");

            Class<?> serializerClazz = Reflection.getNMSClass("IChatBaseComponent$ChatSerializer");
            aMethod = Reflection.getDeclaredMethod(serializerClazz, "a", String.class);

            messageTypeClazz = Reflection.getNMSClass("ChatMessageType");
            Class<?> packetClazz = Reflection.getNMSClass("PacketPlayOutChat");

            if (plugin.getVersionManager().isAbove("1.16", true))
                packetConstructor = Reflection.getConstructor(packetClazz, componentClazz, messageTypeClazz, UUID.class);
            else
                packetConstructor = Reflection.getConstructor(packetClazz, componentClazz);
        }

        try {
            Object iChatBaseComponent = aMethod.invoke(null, json);

            Object packet;
            if (plugin.getVersionManager().isAbove("1.16", true))
                packet = packetConstructor.newInstance(iChatBaseComponent, messageTypeClazz.getEnumConstants()[0], player.getUniqueId());
            else
                packet = packetConstructor.newInstance(iChatBaseComponent);

            Reflection.sendPacket(player, packet);
        } catch (NullPointerException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    public void sendCopyMessage(Player player, String message, String content) {
        JsonObject text = createText(message);
        sendJson(player, toString(attachClick(text, content)));
    }

    private String toString(JsonObject json) {
        return gson.toJson(json);
    }

    private JsonObject attachClick(JsonObject json, String value) {
        JsonObject click = new JsonObject();
        click.addProperty("action", "copy_to_clipboard");
        click.addProperty("value", StringUtil.color(value));
        json.add("clickEvent", click);
        return json;
    }

    private JsonObject createText(String text) {
        JsonObject component = new JsonObject();
        String content = StringUtil.color(text);
        component.addProperty("text", content);
        return component;
    }
}

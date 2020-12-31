package nl.aurorion.blockregen.version.legacy;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.reflect.TypeToken;
import nl.aurorion.blockregen.version.api.INodeData;
import nl.aurorion.blockregen.version.api.INodeDataJsonAdapter;

import java.lang.reflect.Type;

public class LegacyNodeDataAdapter implements INodeDataJsonAdapter {

    @Override
    public INodeData deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        INodeData data = context.deserialize(json, new TypeToken<LegacyNodeData>() {
        }.getType());

        if (data instanceof LegacyNodeData) {
            return data;
        }
        return null;
    }

    @Override
    public JsonElement serialize(INodeData nodeData, Type type, JsonSerializationContext context) {

        if (!(nodeData instanceof LegacyNodeData))
            return null;

        LegacyNodeData legacyNodeData = (LegacyNodeData) nodeData;
        return context.serialize(legacyNodeData);
    }
}

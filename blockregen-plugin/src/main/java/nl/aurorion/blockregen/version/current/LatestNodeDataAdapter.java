package nl.aurorion.blockregen.version.current;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import nl.aurorion.blockregen.version.api.INodeData;
import nl.aurorion.blockregen.version.api.INodeDataJsonAdapter;

import java.lang.reflect.Type;

public class LatestNodeDataAdapter implements INodeDataJsonAdapter {

    @Override
    public INodeData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return null;
    }
}

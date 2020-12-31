package nl.aurorion.blockregen.version.api;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

public interface INodeDataJsonAdapter extends JsonDeserializer<INodeData>, JsonSerializer<INodeData> {
}

package io.vertx.ext.web.handler.graphql.ws;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter and mapper for {@link io.vertx.ext.web.handler.graphql.ws.GraphQLWSOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.web.handler.graphql.ws.GraphQLWSOptions} original class using Vert.x codegen.
 */
public class GraphQLWSOptionsConverter {

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, GraphQLWSOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "connectionInitWaitTimeout":
          if (member.getValue() instanceof Number) {
            obj.setConnectionInitWaitTimeout(((Number)member.getValue()).longValue());
          }
          break;
      }
    }
  }

   static void toJson(GraphQLWSOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(GraphQLWSOptions obj, java.util.Map<String, Object> json) {
    json.put("connectionInitWaitTimeout", obj.getConnectionInitWaitTimeout());
  }
}

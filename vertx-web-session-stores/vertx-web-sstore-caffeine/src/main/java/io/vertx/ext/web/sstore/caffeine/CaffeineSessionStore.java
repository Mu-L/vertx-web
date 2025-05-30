package io.vertx.ext.web.sstore.caffeine;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.sstore.caffeine.impl.CaffeineSessionStoreImpl;

/**
 * A session store based on the Caffeine which is only available on a single node.
 * <p>
 * This store is appropriate if you have just a single Vert.x instance and you are using sticky sessions in your
 * application and have configured your load balancer to always route HTTP requests to the same Vert.x instance.
 *
 * @author <a href="mailto:lazarbulic@gmail.com">Lazar Bulic</a>
 */
@VertxGen
public interface CaffeineSessionStore extends SessionStore {

  /**
   * Default name for map used to store sessions
   */
  String DEFAULT_SESSION_CACHE_NAME = "vertx-web.caffeine.sessions";

  /**
   * Create a session store
   *
   * @param vertx the Vert.x instance
   * @return the session store
   */
  static CaffeineSessionStore create(Vertx vertx) {
    return create(vertx, DEFAULT_SESSION_CACHE_NAME);
  }

  /**
   * Create a session store
   *
   * @param vertx            the Vert.x instance
   * @param sessionCacheName name for map used to store sessions
   * @return the session store
   */
  static CaffeineSessionStore create(Vertx vertx, String sessionCacheName) {
    CaffeineSessionStoreImpl store = new CaffeineSessionStoreImpl();
    store.init(vertx, new JsonObject()
      .put("cacheName", sessionCacheName));
    return store;
  }
}

package io.vertx.ext.web.client.tests.cache;

import io.vertx.core.MultiMap;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.client.impl.cache.Vary;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * As Netty is always decompressing the response and removing Content-Encoding header, we ignore this kind of variation.
 * @author <a href="mailto:jllachf@gmail.com">Jordi Llach</a>
 */
public class VaryTest {

  private static final String ACCEPT_ENCODING = "Accept-Encoding";
  private static final String CONTENT_ENCODING = "Content-Encoding";
  private static final String VARY = "Vary";
  private static final String USER_AGENT = "User-Agent";

  @Test
  public void testVaryPerUserAgent() {
    MultiMap requestHeaders = MultiMap.caseInsensitiveMultiMap()
      .add(USER_AGENT, "Concrete Mobile User Agent");
    MultiMap responseHeaders = MultiMap.caseInsensitiveMultiMap()
      .add(VARY, USER_AGENT)
      .add(USER_AGENT, "Mobile");
    Vary instance = new Vary(requestHeaders, responseHeaders);

    RequestOptions requestMatches = new RequestOptions().addHeader(USER_AGENT, "Another Mobile User Agent");
    RequestOptions requestDoesNotMatch = buildEmptyRequestOptions(); // Desktop by default
    assertTrue("User Agent Vary should match", instance.matchesRequest(requestMatches));
    assertFalse("User Agent Vary should not match", instance.matchesRequest(requestDoesNotMatch));
  }

  @Test
  public void testVaryPerAcceptEncodingIsIgnored() {
    MultiMap requestHeaders = MultiMap.caseInsensitiveMultiMap()
      .add(ACCEPT_ENCODING, "gzip");
    MultiMap responseHeaders = MultiMap.caseInsensitiveMultiMap()
      .add(VARY, ACCEPT_ENCODING)
      .add(CONTENT_ENCODING, "gzip");
    Vary instance = new Vary(requestHeaders, responseHeaders);

    RequestOptions requestMatches = new RequestOptions().addHeader(ACCEPT_ENCODING, "gzip");
    RequestOptions requestMatchesToo = new RequestOptions().addHeader(ACCEPT_ENCODING, "deflate");
    RequestOptions requestMatchesTooo = buildEmptyRequestOptions();
    assertTrue("Encoding matches", instance.matchesRequest(requestMatches));
    assertTrue("Encoding deflate does not match but it is ok", instance.matchesRequest(requestMatchesToo));
    assertTrue("No encoding specified is also ok", instance.matchesRequest(requestMatchesTooo));
  }

  @Test
  public void testVaryForOtherCasesRequestMustMatch() {
    MultiMap requestHeaders = MultiMap.caseInsensitiveMultiMap()
      .add("X-Vertx", "jordi");
    MultiMap responseHeaders = MultiMap.caseInsensitiveMultiMap()
      .add("Vary", "X-Vertx")
      .add("X-Vertx", "jordi");
    Vary instance = new Vary(requestHeaders, responseHeaders);

    RequestOptions requestMatches = new RequestOptions().addHeader("X-Vertx", "jordi");
    RequestOptions requestFails = new RequestOptions().addHeader("X-Vertx", "llach");
    RequestOptions requestFailsToo = buildEmptyRequestOptions();
    assertTrue("Vary per custom header matches", instance.matchesRequest(requestMatches));
    assertFalse("Vary per custom header does not match", instance.matchesRequest(requestFails));
    assertFalse("Vary per custom header not present", instance.matchesRequest(requestFailsToo));
  }

  private RequestOptions buildEmptyRequestOptions() {
    return new RequestOptions().setHeaders(MultiMap.caseInsensitiveMultiMap());
  }

}

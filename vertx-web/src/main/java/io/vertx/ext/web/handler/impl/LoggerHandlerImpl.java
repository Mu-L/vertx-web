/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.web.handler.impl;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerFormatter;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.impl.Utils;

/** # Logger
 *
 * Logger for request. There are 4 formats included:
 * 1. DEFAULT
 * 2. COMBINED
 * 3. SHORT
 * 4. TINY
 *
 * Default tries to log in a format similar to Apache log format, while the other 2 are more suited to development mode.
 * The logging depends on Vert.x logger settings and the severity of the error, so for errors with status greater or
 * equal to 500 the fatal severity is used, for status greater or equal to 400 the error severity is used, for status
 * greater or equal to 300 warn is used and for status above 100 info is used.
 *
 * @author <a href="http://pmlopes@gmail.com">Paulo Lopes</a>
 */
public class LoggerHandlerImpl implements LoggerHandler {

  private static final Logger LOG = LoggerFactory.getLogger(LoggerHandler.class);

  /** log before request or after
   */
  private final boolean immediate;

  /** the current chosen format
   */
  private final LoggerFormat format;

  private LoggerFormatter logFormatter;

  public LoggerHandlerImpl(boolean immediate, LoggerFormat format) {
    this.immediate = immediate;
    this.format = format;
  }

  public LoggerHandlerImpl(LoggerFormat format) {
    this(false, format);
  }

  private String getClientAddress(SocketAddress inetSocketAddress) {
    if (inetSocketAddress == null) {
      return null;
    }
    return inetSocketAddress.host();
  }

  private void log(RoutingContext context, long timestamp, String remoteClient, HttpVersion version, HttpMethod method, String uri) {
    HttpServerRequest request = context.request();
    long contentLength = 0;
    if (immediate) {
      Object obj = request.headers().get("content-length");
      if (obj != null) {
        try {
          contentLength = Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
          // ignore it and continue
        }
      }
    } else {
      contentLength  = request.response().bytesWritten();
    }
    String versionFormatted = "-";
    switch (version){
      case HTTP_1_0:
        versionFormatted = "HTTP/1.0";
        break;
      case HTTP_1_1:
        versionFormatted = "HTTP/1.1";
        break;
      case HTTP_2:
        versionFormatted = "HTTP/2.0";
        break;
    }

    final MultiMap headers = request.headers();
    int status = request.response().getStatusCode();
    String message = null;
    String referrer = null;
    String userAgent = null;

    switch (format) {
      case DEFAULT:
        // as per RFC1945 the header is referer but it is not mandatory some implementations use referrer
        referrer = headers.contains("referrer") ? headers.get("referrer") : headers.get("referer");
        userAgent = request.headers().get("user-agent");
        referrer = referrer == null ? "-" : referrer;
        userAgent = userAgent == null ? "-" : userAgent;

        message = String.format("%s - - [%s] \"%s %s %s\" %d %d \"%s\" \"%s\"",
          remoteClient,
          Utils.formatRFC1123DateTime(timestamp),
          method,
          uri,
          versionFormatted,
          status,
          contentLength,
          referrer,
          userAgent);
        break;
      case COMBINED:
        // as per RFC1945 the header is referer but it is not mandatory some implementations use referrer
        referrer = headers.contains("referrer") ? headers.get("referrer") : headers.get("referer");
        userAgent = request.headers().get("user-agent");
        referrer = referrer == null ? "-" : referrer;
        userAgent = userAgent == null ? "-" : userAgent;

        User user = context.user();
        String userId = "-";
        // user may be null if no auth was performed
        if (user != null) {
          // subject may be null if no auth was performed
          if (user.subject() != null) {
            userId = user.subject();
            // if the userId contains spaces, we need to quote it
            if (userId.indexOf(' ') != -1) {
              userId = "\"" + userId + "\"";
            }
          }
        }

        message = String.format("%s - %s [%s] \"%s %s %s\" %d %d \"%s\" \"%s\"",
          remoteClient,
          userId,
          Utils.formatStrftimeDateTime(timestamp),
          method,
          uri,
          versionFormatted,
          status,
          contentLength,
          referrer,
          userAgent);
        break;
      case SHORT:
        message = String.format("%s - %s %s %s %d %d - %d ms",
          remoteClient,
          method,
          uri,
          versionFormatted,
          status,
          contentLength,
          (System.currentTimeMillis() - timestamp));
        break;
      case TINY:
        message = String.format("%s %s %d %d - %d ms",
          method,
          uri,
          status,
          contentLength,
          (System.currentTimeMillis() - timestamp));
        break;
      case CUSTOM:
        try {
          message = logFormatter.format(context, (System.currentTimeMillis() - timestamp));
        } catch (RuntimeException e) {
          // if an error happens at the user side
          // log it instead
          message = e.getMessage();
        }
    }
    doLog(status, message);
  }

  protected void doLog(int status, String message) {
    if (status >= 500) {
      LOG.error(message);
    } else if (status >= 400) {
      LOG.warn(message);
    } else {
      LOG.info(message);
    }
  }

  @Override
  public void handle(RoutingContext context) {
    // common logging data
    long timestamp = System.currentTimeMillis();
    String remoteClient = getClientAddress(context.request().remoteAddress());
    HttpMethod method = context.request().method();
    String uri = context.request().uri();
    HttpVersion version = context.request().version();

    if (immediate) {
      log(context, timestamp, remoteClient, version, method, uri);
    } else {
      context.addEndHandler(v -> log(context, timestamp, remoteClient, version, method, uri));
    }

    context.next();
  }

  @Override
  public LoggerHandler customFormatter(LoggerFormatter formatter) {
    if (format != LoggerFormat.CUSTOM) {
      throw new IllegalStateException("Setting a formatter requires the handler to be set to CUSTOM format");
    }

    this.logFormatter = formatter;
    return this;
  }
}

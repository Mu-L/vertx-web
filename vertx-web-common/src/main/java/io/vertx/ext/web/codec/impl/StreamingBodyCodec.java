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
package io.vertx.ext.web.codec.impl;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.codec.spi.BodyStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class StreamingBodyCodec implements BodyCodec<Void> {

  private final WriteStream<Buffer> stream;
  private final boolean close;
  private Throwable error;

  public StreamingBodyCodec(WriteStream<Buffer> stream) {
    this(stream, true);
  }

  public StreamingBodyCodec(WriteStream<Buffer> stream, boolean close) {
    this.stream = stream;
    this.close = close;
  }

  public void init() {
    stream.exceptionHandler(err -> {
      synchronized (StreamingBodyCodec.this) {
        error = err;
      }
    });
  }

  @Override
  public BodyStream<Void> stream() throws Exception {
    synchronized (this) {
      if (error != null) {
        if (error instanceof Exception) {
          throw ((Exception) error);
        } else {
          throw new VertxException(error);
        }
      } else {
        return new BodyStream<>() {

          final Promise<Void> promise = Promise.promise();

          @Override
          public Future<Void> result() {
            return promise.future();
          }

          @Override
          public void handle(Throwable cause) {
            promise.tryFail(cause);
          }

          @Override
          public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
            stream.exceptionHandler(handler);
            return this;
          }

          @Override
          public Future<Void> write(Buffer data) {
            return stream.write(data);
          }

          @Override
          public Future<Void> end() {
            if (close) {
              return stream
                .end()
                .onComplete(ar -> {
                  if (ar.succeeded()) {
                    promise.tryComplete();
                  } else {
                    promise.tryFail(ar.cause());
                  }
                });
            } else {
              promise.tryComplete();
              return Future.succeededFuture();
            }
          }

          @Override
          public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
            stream.setWriteQueueMaxSize(maxSize);
            return this;
          }

          @Override
          public boolean writeQueueFull() {
            return stream.writeQueueFull();
          }

          @Override
          public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
            stream.drainHandler(handler);
            return this;
          }
        };
      }
    }
  }
}

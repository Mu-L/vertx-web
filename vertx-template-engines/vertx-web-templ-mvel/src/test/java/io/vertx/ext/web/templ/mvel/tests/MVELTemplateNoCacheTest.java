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

package io.vertx.ext.web.templ.mvel.tests;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.common.template.TemplateEngine;
import org.junit.BeforeClass;
import org.junit.Test;

import io.vertx.ext.web.templ.mvel.MVELTemplateEngine;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@RunWith(VertxUnitRunner.class)
public class MVELTemplateNoCacheTest {

  private static Vertx vertx;

  @BeforeClass
  public static void before() {
    vertx = Vertx.vertx(new VertxOptions().setFileSystemOptions(new FileSystemOptions().setFileCachingEnabled(false)));
  }

  @Test
  public void testCachingDisabled(TestContext should) throws IOException {
    final Async test = should.async();

    System.setProperty("vertxweb.environment", "development");
    TemplateEngine engine = MVELTemplateEngine.create(vertx);

    PrintWriter out;
    File temp = File.createTempFile("template", ".templ", new File("target/classes"));
    temp.deleteOnExit();

    out = new PrintWriter(temp);
    out.print("before");
    out.flush();
    out.close();

    engine.render(new JsonObject(), temp.getParent() + "/" + temp.getName()).onComplete(render -> {
      should.assertTrue(render.succeeded());
      should.assertEquals("before", render.result().toString());
      // cache is enabled so if we change the content that should not affect the result

      try {
        PrintWriter out2 = new PrintWriter(temp);
        out2.print("after");
        out2.flush();
        out2.close();
      } catch (IOException e) {
        should.fail(e);
      }

      engine.render(new JsonObject(), temp.getParent() + "/" + temp.getName()).onComplete(render2 -> {
        should.assertTrue(render2.succeeded());
        should.assertEquals("after", render2.result().toString());
        test.complete();
      });
    });
    test.await();
  }
}

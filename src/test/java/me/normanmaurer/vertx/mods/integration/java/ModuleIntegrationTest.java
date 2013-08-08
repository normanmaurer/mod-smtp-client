package me.normanmaurer.vertx.mods.integration.java;
/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;
import scala.util.parsing.json.JSONArray;

import static org.vertx.testtools.VertxAssert.*;

/**
 * Example Java integration test that deploys the module that this project builds.
 *
 * Quite often in integration tests you want to deploy the same module for all tests and you don't want tests
 * to start before the module has been deployed.
 *
 * This test demonstrates how to do that.
 */
public class ModuleIntegrationTest extends TestVerticle {

  @Test
  public void testSmtpError() {
    JsonObject obj = new JsonObject();
    obj.putString("host", "localhost");
    obj.putNumber("port", 1025);
    obj.putString("helo", "localhost");
    obj.putString("pipeling", "TRY");
    obj.putNumber("connectTimeout", 5);
    obj.putString("sender", "foo@bar.de");
    JsonArray rcpts = new JsonArray();
    rcpts.addString("valid1@valid.de");
    rcpts.addString("valid2@valid.de");

    obj.putArray("recipients", rcpts);
    obj.putBinary("message", "Subject: test\r\n\r\nTest\r\n".getBytes());

    vertx.eventBus().send("vertx.smtpclient", obj, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> reply) {
        assertEquals("error", reply.body().getString("status"));
        assertNotNull(reply.body().getString("message"));

        testComplete();
      }
    });
  }



  @Override
  public void start() {
    // Make sure we call initialize() - this sets up the assert stuff so assert functionality works correctly
    initialize();
    // Deploy the module - the System property `vertx.modulename` will contain the name of the module so you
    // don't have to hardecode it in your tests
    container.deployModule(System.getProperty("vertx.modulename"), new AsyncResultHandler<String>() {
      @Override
      public void handle(AsyncResult<String> asyncResult) {
      // Deployment is asynchronous and this this handler will be called when it's complete (or failed)
      assertTrue(asyncResult.succeeded());
      assertNotNull("deploymentID should not be null", asyncResult.result());
      // If deployed correctly then start the tests!
      startTests();
      }
    });
  }

}

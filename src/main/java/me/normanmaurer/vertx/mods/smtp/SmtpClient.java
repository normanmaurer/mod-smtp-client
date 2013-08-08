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
 */
package me.normanmaurer.vertx.mods.smtp;


import io.netty.channel.EventLoop;
import me.normanmaurer.niosmtp.SMTPClientFuture;
import me.normanmaurer.niosmtp.SMTPClientFutureListener;
import me.normanmaurer.niosmtp.core.SMTPByteArrayMessageImpl;
import me.normanmaurer.niosmtp.delivery.DeliveryRecipientStatus;
import me.normanmaurer.niosmtp.delivery.SMTPDeliveryAgent;
import me.normanmaurer.niosmtp.delivery.SMTPDeliveryAgentConfig;
import me.normanmaurer.niosmtp.delivery.SMTPDeliveryEnvelope;
import me.normanmaurer.niosmtp.delivery.impl.SMTPDeliveryAgentConfigImpl;
import me.normanmaurer.niosmtp.delivery.impl.SMTPDeliveryEnvelopeImpl;
import me.normanmaurer.niosmtp.transport.FutureResult;
import me.normanmaurer.niosmtp.transport.SMTPClientTransport;
import me.normanmaurer.niosmtp.transport.netty.NettySMTPClientTransportFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.EventLoopContext;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Norman Maurer
 */
public class SmtpClient extends BusModBase implements Handler<Message<JsonObject>> {
    private SMTPDeliveryAgent agent = null;
    public void start() {
        super.start();
         String address = getOptionalStringConfig("address", "vertx.smtpclient");

        EventLoop eventLoop = ((EventLoopContext)vertx.currentContext()).getEventLoop();
        SMTPClientTransport transport = NettySMTPClientTransportFactory.createNio(eventLoop).createPlain();
        agent = new SMTPDeliveryAgent(transport);

        eb.registerHandler(address, this);
    }

    @Override
    public void handle(final Message<JsonObject> message) {
        JsonObject json = message.body();
        String host = getMandatoryString("host" ,message);
        int port = json.getInteger("port", 25);
        String helo = json.getString("helo", "localhost");
        String p = json.getString("pipeling", "TRY");
        SMTPDeliveryAgentConfig.PipeliningMode mode;
        switch (p) {
            case "TRY":
                mode = SMTPDeliveryAgentConfig.PipeliningMode.TRY;
                break;
            case "NO":
                mode = SMTPDeliveryAgentConfig.PipeliningMode.NO;
                break;
            case "DEPEND":
                mode = SMTPDeliveryAgentConfig.PipeliningMode.DEPEND;
                break;
            default:
                throw new IllegalArgumentException();
        }
        int connectTimeout = json.getInteger("connectTimeout", 10);
        SMTPDeliveryAgentConfigImpl config = new SMTPDeliveryAgentConfigImpl();
        config.setHeloName(helo);
        config.setPipeliningMode(mode);
        config.setConnectionTimeout(connectTimeout);


        final String sender = json.getString("sender", null);
        List<String> rcpts = new ArrayList<>();
        for(Object rctp: json.getArray("recipients")) {
            rcpts.add((String) rctp);
        }
        byte[] msg = json.getBinary("message");
        SMTPDeliveryEnvelope env = new SMTPDeliveryEnvelopeImpl(sender, rcpts, new SMTPByteArrayMessageImpl(msg));

        SMTPClientFuture<Collection<FutureResult<Iterator<DeliveryRecipientStatus>>>> f = agent.deliver(new InetSocketAddress(host, port),config, env);
        f.addListener(new SMTPClientFutureListener<Collection<FutureResult<Iterator<DeliveryRecipientStatus>>>>() {
            @Override
            public void operationComplete(SMTPClientFuture<Collection<FutureResult<Iterator<DeliveryRecipientStatus>>>> f) {
                FutureResult<Iterator<DeliveryRecipientStatus>> result = f.getNoWait().iterator().next();
                if (!result.isSuccess()) {
                    sendError(message, "Error while deliver messages", result.getException());
                } else {
                    JsonObject reply = new JsonObject();
                    reply.putString("sender", sender);

                    JsonArray array = new JsonArray();
                    Iterator<DeliveryRecipientStatus> statusIt = result.getResult();
                    while (statusIt.hasNext()) {
                        DeliveryRecipientStatus status = statusIt.next();
                        JsonObject jsonStatus = new JsonObject();
                        jsonStatus.putString("recipient", status.getAddress());
                        jsonStatus.putString("status", status.getStatus().name());
                        jsonStatus.putString("response", status.getResponse().toString());
                        array.addObject(jsonStatus);
                    }
                    reply.putArray("deliveryStatus", array);
                    sendOK(message, reply);
                }
            }
        });
    }
}

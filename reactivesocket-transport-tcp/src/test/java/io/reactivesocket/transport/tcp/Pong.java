/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivesocket.transport.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.reactivesocket.Payload;
import io.reactivesocket.RequestHandler;
import io.reactivesocket.transport.tcp.server.ReactiveSocketServerHandler;
import io.reactivesocket.test.TestUtil;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import rx.Observable;
import rx.RxReactiveStreams;

import java.nio.ByteBuffer;
import java.util.Random;

public class Pong {
    public static void main(String... args) throws Exception {
        byte[] response = new byte[1024];
        Random r = new Random();
        r.nextBytes(response);

        RequestHandler requestHandler = new RequestHandler() {
            @Override
            public Publisher<Payload> handleRequestResponse(Payload payload) {
                return subscriber -> {
                    Payload responsePayload = new Payload() {
                        ByteBuffer data = ByteBuffer.wrap(response);
                        ByteBuffer metadata = ByteBuffer.allocate(0);

                        public ByteBuffer getData() {
                            return data;
                        }

                        @Override
                        public ByteBuffer getMetadata() {
                            return metadata;
                        }
                    };

                    subscriber.onNext(responsePayload);
                    subscriber.onComplete();
                };
            }

            @Override
            public Publisher<Payload> handleRequestStream(Payload payload) {
                Payload response =
                    TestUtil.utf8EncodedPayload("hello world", "metadata");
                return RxReactiveStreams
                    .toPublisher(Observable
                        .range(1, 10)
                        .map(i -> response));
            }

            @Override
            public Publisher<Payload> handleSubscription(Payload payload) {
                Payload response =
                    TestUtil.utf8EncodedPayload("hello world", "metadata");
                return RxReactiveStreams
                    .toPublisher(Observable
                        .range(1, 10)
                        .map(i -> response));
            }

            @Override
            public Publisher<Void> handleFireAndForget(Payload payload) {
                return Subscriber::onComplete;
            }

            @Override
            public Publisher<Payload> handleChannel(Payload initialPayload, Publisher<Payload> inputs) {
                Observable<Payload> observable =
                    RxReactiveStreams
                        .toObservable(inputs)
                        .map(input -> input);
                return RxReactiveStreams.toPublisher(observable);
            }

            @Override
            public Publisher<Void> handleMetadataPush(Payload payload) {
                return null;
            }
        };

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    ReactiveSocketServerHandler serverHandler =
                        ReactiveSocketServerHandler.create((setupPayload, rs) -> requestHandler);
                    pipeline.addLast(serverHandler);
                }
            });

        Channel localhost = b.bind("localhost", 7878).sync().channel();
        localhost.closeFuture().sync();
    }
}

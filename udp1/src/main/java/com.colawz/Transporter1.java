package com.colawz;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 监听本地端口10000
 * 发送UDP消息给端口20000
 *
 * @author cola
 * @since 2022/3/2 5:00 下午
 */

@Slf4j
public class Transporter1 {

    private final int me;
    private final int another;
    private final Processor<Proto> processor;
    private final AtomicInteger payload = new AtomicInteger(0);

    private final EventLoopGroup boss = new NioEventLoopGroup();
    private final Bootstrap bootstrap = new Bootstrap();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    private static Channel channel;

    /**
     * 用户业务处理，防止阻塞netty线程
     */
    private final String CALLBACK_THREAD_NAME = "callback_%d";
    private final int CALLBACK_THREAD_COUNT = 3;
    private final ExecutorService callbackService = Executors.newFixedThreadPool(CALLBACK_THREAD_COUNT, new ThreadFactory(){

        private final AtomicInteger threadIndex = new AtomicInteger();

        @Override
        public Thread newThread(@Nullable Runnable r) {
            return new Thread(r, String.format(CALLBACK_THREAD_NAME, threadIndex.getAndIncrement()));
        }
    });

    public Transporter1(int me, int another, Processor<Proto> processor) {
        this.me = me;
        this.another = another;
        this.processor = processor;
    }

    /**
     * 自定协议
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Proto {
        private String name;
        private Integer age;
        private Integer opaque;
    }

    @ChannelHandler.Sharable
    public class ProtoHandler extends SimpleChannelInboundHandler<Proto> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Proto msg) throws Exception {
            // 用户业务处理抽出，并防止阻塞netty线程
            callbackService.execute(() -> processor.handle(ctx, msg));
        }
    }

    public void start() {
        bootstrap.group(boss)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .option(ChannelOption.SO_SNDBUF, 16384)
                .option(ChannelOption.SO_RCVBUF, 16384)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(16384))
                .handler(new ChannelInitializer<DatagramChannel>() {

                    @Override
                    protected void initChannel(DatagramChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast((new MessageToMessageDecoder<DatagramPacket>() {
                                    @Override
                                    protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {
                                        String s = packet.content().toString(StandardCharsets.UTF_8);
                                        Proto proto = JSONObject.parseObject(s, Proto.class);
                                        out.add(proto);
                                    }
                                }))
                                .addLast(new ProtoHandler())
                                .addLast((new MessageToMessageEncoder<Proto>() {
                                    @Override
                                    protected void encode(ChannelHandlerContext ctx, Proto proto, List<Object> out) throws Exception {
                                        DatagramPacket datagramPacket = new DatagramPacket(Unpooled.copiedBuffer(JSON.toJSONString(proto).getBytes(StandardCharsets.UTF_8)), new InetSocketAddress("localhost", another));
                                        out.add(datagramPacket);
                                    }
                                }));


                    }
                });

        ChannelFuture channelFuture = bootstrap.bind(me).syncUninterruptibly();
        if (channelFuture.isSuccess()) {
            log.info("==> udp1 start on {}", ((InetSocketAddress) channelFuture.channel().localAddress()).getPort());
            log.info("==> udp1: I will send to {}", another);
            channel = channelFuture.channel();
        } else {
            this.shutdown();
        }
    }

    public void shutdown() {
        this.boss.shutdownGracefully();
        this.callbackService.shutdown();
    }

    public void send() {
        channel.writeAndFlush(new Proto("transport1", 200, payload.get()));
        log.info("==> send success {}", payload.getAndIncrement());
    }

    public void startSend() {
        this.timer.scheduleWithFixedDelay(this::send, 1,3, TimeUnit.SECONDS);
    }

}

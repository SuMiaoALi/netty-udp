package com.colawz;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author cola
 * @date 2022/3/2 5:14 下午
 * @description
 */
public interface Processor<Proto> {

    /**
     * 业务处理抽象
     *
     * @param ctx ChannelHandlerContext
     * @param proto Proto-业务消息
     * @author cola
     * @since 2022/3/3 4:44 下午
     */
    void handle(ChannelHandlerContext ctx, Proto proto);

}

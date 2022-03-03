package com.colawz;

import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * @author cola
 * @since 2022/3/2 5:26 下午
 */

@Component
@Slf4j
public class StartUp implements InitializingBean, DisposableBean {

    @Value("${broadcast.me}")
    private Integer me;
    @Value("${broadcast.another}")
    private Integer another;

    private Processor<Transporter2.Proto> processor;
    private Transporter2 transporter2;

    @Override
    public void destroy() {
        this.transporter2.shutdown();
    }

    @Override
    public void afterPropertiesSet() {
        // 业务逻辑处理实现
        this.processor = (ctx, proto) -> {
            log.info("==> receive: {}", JSON.toJSONString(proto));
            if (StartUp.this.transporter2 != null)
                StartUp.this.transporter2.receiveAndSend(proto);
        };

        this.transporter2 = new Transporter2(me, another, this.processor);
        transporter2.start();
    }
}

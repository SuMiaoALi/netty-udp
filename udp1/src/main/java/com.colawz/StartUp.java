package com.colawz;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    private Processor<Transporter1.Proto> processor;
    private Transporter1 transporter1;

    @Override
    public void destroy() {
        this.transporter1.shutdown();
    }

    @Override
    public void afterPropertiesSet() {
        // 业务逻辑处理实现
        this.processor = (ctx, proto) -> {
            log.info("==> receive: {}", JSON.toJSONString(proto));
        };

        this.transporter1 = new Transporter1(me, another, this.processor);
        transporter1.start();
        transporter1.startSend();
    }
}

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author cola
 * @since 2022/3/2 4:47 下午
 */

public class TestClass {

    @Test
    @DisplayName("codec")
    public void codec() {
        System.out.println("测试API：");
        String str = "hello codec udp";
        ByteBuf byteBuf = Unpooled.copiedBuffer(str.getBytes(StandardCharsets.UTF_8));
        DatagramPacket datagramPacket = new DatagramPacket(byteBuf, new InetSocketAddress("127.0.0.1", 10086));

        ByteBuf content = datagramPacket.content();
        String toString = content.toString(StandardCharsets.UTF_8);
    }

}

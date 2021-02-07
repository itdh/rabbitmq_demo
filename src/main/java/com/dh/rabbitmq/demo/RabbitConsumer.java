package com.dh.rabbitmq.demo;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class RabbitConsumer {

    private static final String QUEUE_NAME = "queue_demo";
    private static final String IP_ADDRESS = "192.168.140.128";
    private static final int PORT = 5672;
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String VHOST = "my_vhost";
    private static Log log = LogFactory.getLog(RabbitConsumer.class);

    public static void main(String[] args) throws Exception {
        Address[] addresses = {new Address(IP_ADDRESS, PORT)};
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);
        factory.setVirtualHost(VHOST);
        Connection connection = factory.newConnection(addresses);// 创建连接
        Channel channel = connection.createChannel();// 创建信道
        channel.basicQos(1);// 设置客户端最多接收未被ack的消息个数
        // 定义队列的消费者
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                log.info("receive message: " + new String(body, StandardCharsets.UTF_8));
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    log.warn(e.getMessage(), e);
                }
                // 返回确认状态
                /*
                channel.basicQos(1);和channel.basicAck(envelope.getDeliveryTag(),false);是配套使用
                只有在channel.basicQos被使用的时候channel.basicAck(envelope.getDeliveryTag(),false)才起到作用。
                 */
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };
        // 监听队列，手动返回完成状态
        channel.basicConsume(QUEUE_NAME, consumer);
        // 等待回调函数执行完毕之后，关闭资源
        TimeUnit.SECONDS.sleep(5);
        channel.close();
        connection.close();
    }
}

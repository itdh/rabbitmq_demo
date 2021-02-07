## 一、安装RabbitMQ（基于centos7安装）

### 1. 手动下载RabbitMQ安装

由于RabbitMQ是由Erlang语言编写的，所以在安装RabbitMQ之前需要先安装Erlang。

#### 1.1 安装 Erlang

前往[官网](https://www.erlang.org/downloads)下载最新版本（目前最新版本为23.2），[点击此处下载](http://erlang.org/download/otp_src_23.2.tar.gz)，下载完成后开始安装。

- 第一步，解压安装包，并配置安装目录



### 2.在docker中安装RabbitMQ

#### 2.1 查看仓库中的RabbitMQ

```she
docker search rabbitmq
```

![1612667245927](./.assets\1612667245927.png)



#### 2.2 安装RabbitMQ

- 直接安装最新版本的RabbitMQ（如果需要安装其他版本，直接在rabbitmq后面加上`:版本号`即可）

```shell
docker pull rabbitmq
```

![1612667585482](./.assets\1612667585482.png)



- 然后查看所有镜像

```shell
docker images
```

![1612667700847](./.assets\1612667700847.png)



#### 2.3 启动容器

```shell
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 -v `pwd`/data:/var/lib/rabbitmq --hostname myRabbit -e RABBITMQ_DEFAULT_VHOST=my_vhost  -e RABBITMQ_DEFAULT_USER=admin -e RABBITMQ_DEFAULT_PASS=admin c05fdf32bdad
```

![1612668072696](./.assets\1612668072696.png)

- 说明：

  > -d 后台运行容器；
  >
  > --name 指定容器名；
  >
  > -p 指定服务运行端口（5672：应用访问端口；15672：控制台Web端口号）
  >
  > -v 映射目录或文件
  >
  > --hostname 主机名（RabbitMQ的一个重要注意事项是它根据所谓的 “节点名称” 存储数据，默认为主机名）；
  >
  > -e 指定环境变量（RABBITMQ_DEFAULT_VHOST：默认虚拟机名；RABBITMQ_DEFAULT_USER：默认的用户名；RABBITMQ_DEFAULT_PASS：默认用户名的密码）
  >
  > 最后的`c05fdf32bdad`为rabbitmq的镜像id



在外部访问需要注意放行对应的端口

#### 2.4 启动rabbitmq_management

```shell
docker exec -it rabbit rabbitmq-plugins enable rabbitmq_management
```

![1612668592076](./.assets\1612668592076.png)

然后通过浏览器访问web管理端

```ht
http://ip:15672
```

![1612670800306](./.assets\1612670800306.png)

使用启动容器时指定的用户名和密码登录即可



### 3. Hello World

代码演示：首先生产者发送一条消息"hello world!"至RabbitMQ，之后由消费者消费。

#### 3.1 生产者客户端代码

```java
package com.dh.rabbitmq.demo;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.charset.StandardCharsets;

public class RabbitProducer {

    private static final String EXCHANGE_NAME = "exchange_demo";
    private static final String ROUTING_KEY = "routingkey_demo";
    private static final String QUEUE_NAME = "queue_demo";
    private static final String IP_ADDRESS = "192.168.140.128";
    private static final int PORT = 5672;
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String VHOST = "my_vhost";
    private static Log log = LogFactory.getLog(RabbitProducer.class);

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(IP_ADDRESS);
        factory.setPort(PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);
        factory.setVirtualHost(VHOST);
        Connection connection = factory.newConnection();// 创建连接
        Channel channel = connection.createChannel();// 创建信道
        // 创建一个类型为direct、持久化的、非自动删除的交换器
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT, true, false, null);
        // 创建一个持久化、非排他的、非自动删除的队列
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        // 将交换器与队列通过路由键绑定
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
        // 发送一条持久化的消息
        String message = "Hello World!";
        log.info("send message：" + message);
        channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
        // 关闭资源
        channel.close();
        connection.close();
    }
}

```

#### 3.2 消费者客户端代码

```java
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

```

> RabbitMQ中的概念，channel.basicQos(1)指该消费者在接收到队列里的消息但没有返回确认结果之前,队列不会将新的消息分发给该消费者。队列中没有被消费的消息不会被删除，还是存在于队列中。
>
> channel.basicQos(1);和channel.basicAck(envelope.getDeliveryTag(),false);是配套使用，只有在channel.basicQos被使用的时候channel.basicAck(envelope.getDeliveryTag(),false)才起到作用。
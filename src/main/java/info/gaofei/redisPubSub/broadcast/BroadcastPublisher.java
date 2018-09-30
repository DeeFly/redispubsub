package info.gaofei.redisPubSub.broadcast;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class BroadcastPublisher {
    private static RedisTemplate<String, String> redisTemplate;
    private static String lock = "lock";
    private static Gson gsonUtil = new Gson();
    public static LinkedBlockingQueue<XueleMessage> sendQueue = new LinkedBlockingQueue();
    private static boolean isOkay = true;
    private static final Logger logger = LoggerFactory.getLogger(BroadcastPublisher.class);

    public static void publish(XueleMessage msg) {
        if(!isOkay)return;
        if (redisTemplate == null){
            isOkay=init();
            if(!isOkay)return;
        }
        if (msg.getTopic() == null)
            throw new RuntimeException("Message topic is not specifed.");
        if (msg.getPayload() == null)
            throw new RuntimeException("Message paload is not specifed.");
        if (msg.getType() < 1)
            throw new RuntimeException("Message type is not valid:" + msg.getType());
//        msg.setMessageTimestamp(System.currentTimeMillis());
        sendQueue.add(msg);
    }

    private static boolean init() {
        String keyIntern = lock.intern();
        synchronized (keyIntern) {
            if(redisTemplate==null){
                JedisConnectionFactory jedisConnectionFactory = getRedisFactory();
                if(jedisConnectionFactory==null)return false;
                redisTemplate = new RedisTemplate();
                redisTemplate.setConnectionFactory(jedisConnectionFactory);
                redisTemplate.setKeySerializer(new StringRedisSerializer());
                redisTemplate.setValueSerializer(new StringRedisSerializer());
                redisTemplate.afterPropertiesSet();
                new Thread(new SendThread(),"BroadcastPublisherSend").start();
            }
        }
        return true;
    }

    private static JedisConnectionFactory localFactory;
    final static JedisConnectionFactory getRedisFactory() {
        if(localFactory==null){
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(1024);
            poolConfig.setMaxIdle(200);
            poolConfig.setMaxWaitMillis(1000);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            localFactory = new JedisConnectionFactory(poolConfig);
            localFactory.setHostName("192.168.1.223");
            localFactory.setPort(6379);
            localFactory.setPassword("123456");
            localFactory.setTimeout(10000);
            localFactory.afterPropertiesSet();
            try {
                localFactory.getConnection().close();
            } catch (Exception e) {
                e.printStackTrace();
                localFactory=null;
                return null;
            }
        }
        return localFactory;
    }
    static ExecutorService executorPool = Executors.newFixedThreadPool(2, new CustomizableThreadFactory("broadcast-publisher-"));
    static class SendThread  implements Runnable{
        public void run() {
            while (true) {
                try {
                    final XueleMessage msg = sendQueue.take();
                    Runnable command=new Runnable(){
                        public void run() {
                            try {
                                Thread.sleep(30);
                                String messageJson = gsonUtil.toJson(msg);
                                logger.info("发布消息成功，主题[{}]。消息 : [{}]",msg.getTopic(),msg);
                                redisTemplate.convertAndSend(msg.getTopic(), messageJson);
                            } catch (Exception e) {
                                logger.error("发送广播失败", e);
                            }
                        }
                    };
                    executorPool.execute(command);
                } catch (Exception e) {
                    logger.error("从sendQueue广播发送通知失败", e);
                }
            }
        }
    }
}

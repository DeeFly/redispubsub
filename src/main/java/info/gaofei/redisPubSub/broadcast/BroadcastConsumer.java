package info.gaofei.redisPubSub.broadcast;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BroadcastConsumer {
    private static final Logger logger = LoggerFactory.getLogger(BroadcastConsumer.class);
    private static RedisMessageListenerContainer container;
    private static String lock = "lock";
    static Map<String, Set<XueleMessageListener>> registed_listeners = new ConcurrentHashMap<String, Set<XueleMessageListener>>();
    static Map<String, Set<XueleMessageListener>> once_registed_listeners = new ConcurrentHashMap<String, Set<XueleMessageListener>>();
    private static boolean isOkay = true;
    public static final void subscriber(String topic, XueleMessageListener listener) {
        logger.warn("subscriber订阅[{}]。currentThread : [{}]",topic,Thread.currentThread().getName());
        if(!isOkay)return;
        if (container == null){
            isOkay=init();
            if(!isOkay){
                logger.error("subscriber订阅失败[{}]。currentThread : [{}]",topic,Thread.currentThread().getName());
                return;
            }
        }
        Set<XueleMessageListener> listener_set = registed_listeners.get(topic);
        if (listener_set == null || listener_set.size() == 0) {
            registed_listeners.put(topic, new HashSet<XueleMessageListener>());
            // 创建一个新主题
            MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(new BroadcastReceiver(), "handleMessage");
            listenerAdapter.setSerializer(new StringRedisSerializer());
            listenerAdapter.afterPropertiesSet();
            container.addMessageListener(listenerAdapter, new PatternTopic(topic));
        }
        registed_listeners.get(topic).add(listener);
        logger.warn("subscriber订阅主题[{}]成功。currentThread : [{}]",topic,Thread.currentThread().getName());
    }
    public static final void subscriberOnce(String topic, XueleMessageListener listener) {
        if(!isOkay)return;
        if (container == null){
            isOkay=init();
            if(!isOkay)return;
        }
        Set<XueleMessageListener> listener_set = once_registed_listeners.get(topic);
        if (listener_set == null || listener_set.size() == 0) {
            once_registed_listeners.put(topic, new HashSet<XueleMessageListener>());
            MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(new OnceReceiver(), "handleMessage");
            listenerAdapter.setSerializer(new StringRedisSerializer());
            listenerAdapter.afterPropertiesSet();
            container.addMessageListener(listenerAdapter, new PatternTopic(topic));
        }
        once_registed_listeners.get(topic).add(listener);
        logger.warn("subscriberOnce订阅主题[{}]成功。currentThread : [{}]",topic,Thread.currentThread().getName());
    }
    static RedisTemplate<String, String> redisTemplate;
    static boolean isOnline=false;
    static String zkAdrr="127.0.0.1";
    private static boolean init() {
        String keyIntern = lock.intern();
        synchronized (keyIntern) {
            container = new RedisMessageListenerContainer();
            JedisConnectionFactory redisFactory = BroadcastPublisher.getRedisFactory();
            if(redisFactory==null)return false;
            container.setConnectionFactory(redisFactory);
            container.afterPropertiesSet();
            container.start();
            redisTemplate = new RedisTemplate();
            redisTemplate.setConnectionFactory(redisFactory);
            redisTemplate.setKeySerializer(new StringRedisSerializer());
            redisTemplate.setValueSerializer(new StringRedisSerializer());
            redisTemplate.afterPropertiesSet();
            String profile=System.getProperty("superdiamond.profile");
            if(profile!=null&&profile.equalsIgnoreCase("production")){
                isOnline=true;
                try {
                    InetAddress address = InetAddress.getByName("zk.infra.middleware.xueleyun.com");
                    zkAdrr=address.getHostAddress().toString();
                } catch (UnknownHostException e) {
                    logger.error("获取zk.infra.middleware.xueleyun.com具体ip失败",e);
                }
            }
        }
        return true;
    }
}

class BroadcastReceiver {
    private static Gson gsonUtil = new Gson();
    private static final Logger logger = LoggerFactory.getLogger(BroadcastReceiver.class);
    public void handleMessage(String messsage, String topic) {
        Set<XueleMessageListener> listenersOfTopic = BroadcastConsumer.registed_listeners.get(topic);
        if (listenersOfTopic == null || listenersOfTopic.isEmpty())
            return;
        XueleMessage msg = gsonUtil.fromJson(messsage, XueleMessage.class);
        logger.info("BroadcastReceiver消费主题消息[{}]成功。当前线程 : [{}]，消息: [{}]",topic,Thread.currentThread().getName(),msg);
        for (XueleMessageListener each : listenersOfTopic) {
            try {
                each.handleMessage(msg);
            } catch (Exception e) {
               e.printStackTrace();
            }
        }
    }
}

class OnceReceiver {
    private static Gson gsonUtil = new Gson();
    private static final Logger logger = LoggerFactory.getLogger(OnceReceiver.class);
    public void handleMessage(String messsage, String topic) {
        Set<XueleMessageListener> onceListenersOfTopic = BroadcastConsumer.once_registed_listeners.get(topic);
        if (onceListenersOfTopic == null || onceListenersOfTopic.isEmpty())
            return;
        Map<String,Set<XueleMessageListener>> groupMap=new HashMap();
        for(XueleMessageListener item:onceListenersOfTopic){
            String listenClass = item.getClass().toString();
            if(groupMap.containsKey(listenClass)){
                groupMap.get(listenClass).add(item);
            }else{
                Set<XueleMessageListener> tempSet=new HashSet();
                tempSet.add(item);
                groupMap.put(listenClass,tempSet);
            }
        }
        for(String key:groupMap.keySet()){
            String uniqueKey = topic + key + messsage.hashCode();
            if(!BroadcastConsumer.isOnline){
                uniqueKey=uniqueKey+BroadcastConsumer.zkAdrr;
            }
            BoundValueOperations<String, String> boundValueOps = BroadcastConsumer.redisTemplate.boundValueOps(uniqueKey);
            String obj = boundValueOps.getAndSet("1");
            try {
                if (obj == null) {
                    for (XueleMessageListener each : groupMap.get(key)) {
                        XueleMessage msg = gsonUtil.fromJson(messsage, XueleMessage.class);
                        logger.info("OnceReceiver消费主题消息[{}]成功。当前线程 : [{}]，消息: [{}], listener is [{}]",topic,Thread.currentThread().getName(),msg,key);
                        each.handleMessage(msg);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            boundValueOps.expire(3, TimeUnit.SECONDS);
        }

    }
}

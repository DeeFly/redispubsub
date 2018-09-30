package info.gaofei.redisPubSub.usecase;


import info.gaofei.redisPubSub.broadcast.BroadcastPublisher;
import info.gaofei.redisPubSub.broadcast.RedisBroadCastSendService;
import info.gaofei.redisPubSub.broadcast.XueleMessage;
import info.gaofei.redisPubSub.broadcast.XueleMessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 客户端发送消息
 */
@Service
public class RedisBroadCastSendServiceImpl implements RedisBroadCastSendService {
	
    private static Logger logger = LoggerFactory.getLogger(RedisBroadCastSendServiceImpl.class);

	public boolean sendMessage(){
		boolean result = false;
		try{
				XueleMessage msg = new XueleMessage(XueleMessageConstants.TOPIC_TEACH_ACTION, "jsonString");
				BroadcastPublisher.publish(msg);
				result =  true;
		  } catch (Exception e) {
		        return false;
		  }
		return result;
	}
}

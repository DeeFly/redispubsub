package info.gaofei.redisPubSub.broadcast;

import java.io.Serializable;

@SuppressWarnings("serial")
public class XueleMessage implements Serializable {

    /**
     * Message topic. The message substriber only handle the message matches
     * with specific topic.
     */
    private String topic;

    /**
     * message type. it can be: 0 All 1 add 2 update 3 delete 4 notify
     */
    private byte type = XueleMessageConstants.MESSAGE_TYPE_NOTIFY;

    /**
     * The data body of this message. json format
     */
    private String payload;
    
//    private long messageTimestamp=0l;

    public XueleMessage(String topic, byte type, String payload) {
        super();
        this.topic = topic;
        this.type = type;
        this.payload = payload;
    }

    public XueleMessage(String topic, String payload) {
        super();
        this.topic = topic;
        this.payload = payload;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "XueleMessage [topic=" + topic + ", type=" + type + ", payload=" + payload + "]";//+ ", messageTimestamp=" + messageTimestamp + "]";
    }

//    public long getMessageTimestamp() {
//        return messageTimestamp;
//    }
//
//    public void setMessageTimestamp(long messageTimestamp) {
//        this.messageTimestamp = messageTimestamp;
//    }

}

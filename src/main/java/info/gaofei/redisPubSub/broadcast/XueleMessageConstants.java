package info.gaofei.redisPubSub.broadcast;

/**
 * All member module supported message topic MUST be defined in this file.
 * */
public class XueleMessageConstants {
    public final static byte MESSAGE_TYPE_ADD = 1;
    public final static byte MESSAGE_TYPE_UPDATE = 2;
    public final static byte MESSAGE_TYPE_DELETE = 3;
    public final static byte MESSAGE_TYPE_NOTIFY = 4;
    public final static byte MESSAGE_TYPE_CUSTOM = 5;

    /**
     * broadcast topic,need confirm platform team when add
     */
    public static final String TOPIC_USER_LOGIN = "mem:login";//用户登陆
	public static final String TOPIC_MEMBER_USER = "mem:user";//用户变动
	public static final String TOPIC_MEMBER_SCHOOL = "mem:shcool";//学校变动
	public static final String TOPIC_MEMBER_CLASS = "mem:class";//班级变动
    public static final String TOPIC_MEMBER_FAMILY = "mem:family";//家庭邀请
    public static final String TOPIC_SYNC_TRAIN = "sync:train";//同步训练
    public static final String TOPIC_COMMETITION_TRAIN = "commetition:train";//金榜题名
    public static final String TOPIC_MAGICWORK_TRAIN = "magicwork:train";//提分宝
    public static final String TOPIC_HOMEWORK_TRAIN = "homework:train";//作业
    public static final String TOPIC_OPENAPI_USER_TOKEN = "openapi:user:token";//删除移动端session,强制移动端退出
    /**
     * 联合学校
     */
    public static final String TOPIC_MEMBER_UNITEDSCHOOL = "mem:unsch";//联合学校
    /**
     *
     */
    public static final String TOPIC_EXAM_STUDENT = "exam:student";//学生考试
    public static final String TOPIC_MEMBER_VCLASS = "mem:vclass";//走读班班级变动
    public static final String TOPIC_TEACH_ACTION = "teach:action";//教学行为
    public static final String TOPIC_CIRCLE_PRAISE = "circle:praise";//空间点赞
    public static final String TOPIC_TEACH_SHARE_RESOURCE="teach:share:resource";//智慧课堂分享资源
    public static final String TOPIC_MEMBER_EDU = "mem:edu";//教育机构变动
}

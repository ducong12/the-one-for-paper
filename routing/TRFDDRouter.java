package routing;

import core.*;
import util.Tuple;

import java.util.*;

/**
 * Created by ducong on 2018/5/5.
 */
public class TRFDDRouter extends ActiveRouter{

    public static final double E1_OTHER_INIT = 1/3.0 ;
    public static final double E2_OTHER_INIT = 1/3.0 ;
    public static final double E3_OTHER_INIT = 1/3.0 ;

    public static final double E1_FRIEND_INIT = 2/3.0 ;
    public static final double E2_FRIEND_INIT = 1/6.0 ;
    public static final double E3_FRIEND_INIT = 1/6.0 ;

    public static final double E1_STRANGER_INIT = 1/2.0 ;
    public static final double E2_STRANGER_INIT = 1/4.0 ;
    public static final double E3_STRANGER_INIT = 1/4.0 ;

    public static final double DEFAULT_MATCH = 1/3.0;
    public static final double DEFAULT_WEIGHT=0.8;
    public static final double DEFAULT_BETA = 0.4;
    public static final double DEFAULT_MU = 0.25;

    public static final double P_THRESHOLD=0.01;
    public static final double S_THRESHOLD=0.1;

    public static final double OTHER_GAMMA=0.4;
    public static final double STRANGER_GAMMA=0.6;
    public static final double FRIEND_GAMMA=0.8;

    public static final double BASIC_RATE=1.0;
    public static final double DOUBLE_RATE=2.0;
    public static final double HALF_RATE=0.5;

    public static final int NORMAL=0;
    public static final int SELFISH=1;
    public static final int MALICIOUS=2;

    public static final int HALF=0;
    public static final int BASIC=0;
    public static final int DOUBLE=0;

    public static final String TRFDD_NS = "TRFDDRouter";
    public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";

    public static final String MATCH_S = "match";
    public static final String WEIGHT_S = "weight";
    public static final String BETA_S = "beta";
    public static final String MU_S = "mu";

    public static final String P_THRESHOLD_S="p_th";
    public static final String S_THRESHOLD_S="s_th";

    public static final String OTHER_GAMMA_S="other_gamma";
    public static final String STRANGER_GAMMA_S="stranger_gamma";
    public static final String FRIEND_GAMMA_S="friend_gamma";

    private Map<DTNHost, Double> sims;
    //记录历史接触结果
    private Map<DTNHost, Double> preds;
    //记录最近接触结果
    private Map<DTNHost, Double> effectContact;

    private int secondsInTimeUnit;

    private double match;
    private double weight;
    private double beta;
    private double mu;

    private double p_th;
    private double s_th;

    private double other_gamma;
    private double stranger_gamma;
    private double friend_gamma;

    private Map<DTNHost, Double> normalAction;
    private Map<DTNHost, Double> selfishAction;
    private Map<DTNHost, Double> maliciousAction;

    private double lastAgeUpdate;

    public Map<DTNHost, double[]> eva_other;
    public Map<DTNHost, double[]> eva_friend;
    public Map<DTNHost, double[]> eva_stranger;

    public Map<DTNHost, double[]> getEva_other() {
        return eva_other;
    }

    public Map<DTNHost, double[]> getEva_friend() {
        return eva_friend;
    }

    public Map<DTNHost, double[]> getEva_stranger() {
        return eva_stranger;
    }

    public TRFDDRouter(Settings s) {
        super(s);
        Settings TRFDDSettings = new Settings(TRFDD_NS);
        secondsInTimeUnit = TRFDDSettings.getInt(SECONDS_IN_UNIT_S);
        if (TRFDDSettings.contains(MATCH_S)) {
            match = TRFDDSettings.getDouble(MATCH_S);
        }
        else {
            match = DEFAULT_MATCH;
        }
        if (TRFDDSettings.contains(OTHER_GAMMA_S)){
            other_gamma = TRFDDSettings.getDouble(OTHER_GAMMA_S);
        }else {
            other_gamma = OTHER_GAMMA;
        }
        if (TRFDDSettings.contains(STRANGER_GAMMA_S)){
            stranger_gamma = TRFDDSettings.getDouble(STRANGER_GAMMA_S);
        }else {
            stranger_gamma = STRANGER_GAMMA;
        }
        if (TRFDDSettings.contains(FRIEND_GAMMA_S)){
            friend_gamma = TRFDDSettings.getDouble(FRIEND_GAMMA_S);
        }else {
            friend_gamma = FRIEND_GAMMA;
        }
        if (TRFDDSettings.contains(WEIGHT_S)){
            weight=TRFDDSettings.getDouble(WEIGHT_S);
        }else {
            weight=DEFAULT_WEIGHT;
        }
        if (TRFDDSettings.contains(P_THRESHOLD_S)){
            p_th=TRFDDSettings.getDouble(P_THRESHOLD_S);
        }else {
            p_th=P_THRESHOLD;
        }
        if (TRFDDSettings.contains(S_THRESHOLD_S)){
            s_th=TRFDDSettings.getDouble(S_THRESHOLD_S);
        }else {
            s_th=S_THRESHOLD;
        }
        if (TRFDDSettings.contains(BETA_S)){
            beta=TRFDDSettings.getDouble(BETA_S);
        }else {
            beta=DEFAULT_BETA;
        }
        if (TRFDDSettings.contains(MU_S)){
            mu=TRFDDSettings.getDouble(MU_S);
        }else {
            mu=DEFAULT_MU;
        }
        initActions();
        initEffectContact();
        initPreds();
        initSims();
        initEvas();
    }

    protected TRFDDRouter(TRFDDRouter r) {
        super(r);
        this.secondsInTimeUnit = r.secondsInTimeUnit;
        this.match = r.match;
        this.weight=r.weight;
        this.other_gamma=r.other_gamma;
        this.stranger_gamma=r.stranger_gamma;
        this.friend_gamma=r.friend_gamma;
        this.p_th=r.p_th;
        this.s_th=r.s_th;
        this.beta=r.beta;
        this.mu=r.mu;
        initActions();
        initEffectContact();
        initPreds();
        initSims();
        initEvas();
    }

    public double getTimeDiff(){
        return (SimClock.getTime() - this.lastAgeUpdate) /
                secondsInTimeUnit;
    }

    //每隔一段时间更新一次，在每次get各种metric之前更新，比较被动
    public void updateMetrics(){
        double timeDiff = getTimeDiff();
        if (timeDiff < 10) {
            return;
        }
        for (Map.Entry<DTNHost, Double> e : preds.entrySet()){
            updatePreds(e.getKey(),e.getValue(),timeDiff);
        }
        for (Map.Entry<DTNHost, Double> e : effectContact.entrySet()){
            if (!preds.containsKey(e.getKey()))
                updateEffectPredFor(e.getKey(),e.getValue(),timeDiff);
        }
        initEffectContact();
        for (Map.Entry<DTNHost, double[]> e : eva_friend.entrySet()){
            updateDirectEvaluation(e.getKey(),e.getValue());
        }
        for (Map.Entry<DTNHost, double[]> e : eva_stranger.entrySet()){
            updateDirectEvaluation(e.getKey(),e.getValue());
        }
        for (Map.Entry<DTNHost, double[]> e : eva_other.entrySet()){
            updateDirectEvaluation(e.getKey(),e.getValue());
        }
        this.lastAgeUpdate = SimClock.getTime();
    }

    public void initPreds() {
        this.preds = new HashMap<DTNHost, Double>();
    }

    public double getPredFor(DTNHost host) {
        updateMetrics();
        if (preds.containsKey(host)) {
            return preds.get(host);
        }
        else {
            return 0;
        }
    }

    public void updateEffectPredFor(DTNHost des,double newPred,double timeDiff){
        newPred=newPred/timeDiff;
        preds.put(des,newPred);
        //resetEffectContact(des);
    }

    public void updatePreds(DTNHost des,double oldPred,double timeDiff){
        double newPred=getEffectContact(des);
        newPred=beta*oldPred+(1-beta)*newPred/timeDiff;
        preds.put(des,newPred);
        //resetEffectContact(des);
    }

    public double calProb(DTNHost des){
        return 1-Math.exp(-getPredFor(des)*secondsInTimeUnit);
    }

    public void initSims() {
        this.sims = new HashMap<DTNHost, Double>();
    }

    //获取结果
    public double getSimFor(DTNHost host) {
        //updateMetrics();
        return societySim(host);
    }

    public void updateSim(DTNHost des){
        sims.put(des,societySim(des));
    }

    public int match_x(String s1,String s2){
        String[] str1=s1.split(",");
        String[] str2=s2.split(",");
        int[] a1=new int[str1.length];
        int[] a2=new int[str2.length];
        for (int i=0;i<str1.length;i++) {
            a1[i]=Integer.parseInt(str1[i]);
        }
        for (int i=0;i<str2.length;i++) {
            a2[i]=Integer.parseInt(str2[i]);
        }
        for (int i=0;i<str1.length;i++) {
            for (int j=0;j<str2.length;j++) {
                if (a1[i]==a2[j]) {
                    return 1;
                }
            }
        }
        return 0;
    }

    //每次遇到全新的节点，先判断是否是朋友关系之后更新社交圈子，已用
    public boolean isFriend(DTNHost other){
        int times=0;
        times+=match_x(getHost().getAffiliation(),other.getAffiliation());
        times+=match_x(getHost().getCity(),other.getCity());
        times+=match_x(getHost().getNationality(),other.getNationality());
        times+=match_x(getHost().getLanguage(),other.getLanguage());
        times+=match_x(getHost().getCountry(),other.getCountry());
        times+=match_x(getHost().getPosition(),other.getPosition());
        return times/6.0>=match;
    }

    //更新社交圈子,已用
    public void updateSociety(DTNHost other){
        int[][] old_society=getHost().getSociety();
        int[][] new_society=new int[6][50];
        new_society[0]=societyAttr(other.getAffiliation());
        new_society[1]=societyAttr(other.getCity());
        new_society[2]=societyAttr(other.getNationality());
        new_society[3]=societyAttr(other.getLanguage());
        new_society[4]=societyAttr(other.getCountry());
        new_society[5]=societyAttr(other.getPosition());
        for (int i=0;i<6;i++){
            for (int j=0;j<50;j++){
                new_society[i][j]+=old_society[i][j];
            }
        }
        getHost().setSociety(new_society);
    }
    //获得检测节点的对应社交属性，已用
    public int[] societyAttr(String s){
        String[] str=s.split(",");
        int[] a=new int[50];
        int[] b=new int[str.length];
        for (int i=0;i<str.length;i++) {
            b[i]=Integer.parseInt(str[i]);
            a[b[i]]=1;
        }
        return a;
    }
    //本地社交圈子与目的节点之间用欧几里得相似性计算
    public double societySim(DTNHost des){
        int[][] localSociety=getHost().getSociety();
        double[] percent=new double[6];
        double result=0.0;
        percent[0]=samePercent(localSociety[0],societyAttr(des.getAffiliation()));
        percent[1]=samePercent(localSociety[1],societyAttr(des.getCity()));
        percent[2]=samePercent(localSociety[2],societyAttr(des.getNationality()));
        percent[3]=samePercent(localSociety[3],societyAttr(des.getLanguage()));
        percent[4]=samePercent(localSociety[4],societyAttr(des.getCountry()));
        percent[5]=samePercent(localSociety[5],societyAttr(des.getPosition()));
        for (int i=0;i<6;i++){
            result+=Math.pow(percent[i]-1,2);
        }
        result=Math.sqrt(result/6.0);
        return 1-result;
    }
    //计算每个种类的相似性
    public double samePercent(int[] local,int[] test){
        double same=0;
        double total=0;
        for (int i=0;i<50;i++){
            if (test[i]==1){
                same+=local[i];
            }
            total+=local[i];
        }
        if (total>0){
            return same/total;
        }else {
            return 0.0;
        }
    }

    public void initEvas() {
        this.eva_other = new HashMap<DTNHost, double[]>();
        this.eva_friend = new HashMap<DTNHost, double[]>();
        this.eva_stranger = new HashMap<DTNHost, double[]>();
    }

    public double[] getEvaFor(DTNHost host) {
        updateMetrics();
        if (eva_friend.containsKey(host)) {
            return eva_friend.get(host);
        } else if (eva_stranger.containsKey(host)){
            return eva_stranger.get(host);
        } else if (eva_other.containsKey(host)){
                return eva_other.get(host);
        }
        double[] initOther={E1_OTHER_INIT,E2_OTHER_INIT,E3_OTHER_INIT};
        return initOther;
    }

    public void updateDirectEvaluation(DTNHost other,double[] oldValue){
        double[] newValue = new double[3];
        newValue[0] = (oldValue[0]+getNormalAction(other))/(1+getNormalAction(other)+getSelfishAction(other)+
                getMaliciousAction(other));
        newValue[1] = (oldValue[1]+getSelfishAction(other))/(1+getNormalAction(other)+getSelfishAction(other)+
                getMaliciousAction(other));
        newValue[2] = (oldValue[2]+getMaliciousAction(other))/(1+getNormalAction(other)+getSelfishAction(other)+
                getMaliciousAction(other));
        if (eva_friend.containsKey(other)) {
            eva_friend.put(other, newValue);
        } else if(eva_stranger.containsKey(other)){
            eva_stranger.put(other, newValue);
        } else {
            eva_other.put(other, newValue);
        }
        resetActions(other);
    }

    //初次见面的候选人，判别关系，写进本地的society的数组里，分组初始化或者获取实验结果进行直接评估的更新
    public void updateCandidate (DTNHost other){
        //若在其他节点中，则删除掉，杜绝这种情况
        if (eva_other.containsKey(other)){
            eva_other.remove(other);
        }
        //如果之前不在朋友或陌生人集合中
        if (!eva_friend.containsKey(other)&&!eva_stranger.containsKey(other)){
            //朋友关系判定
            if (isFriend(other)){
                //全新的初始化
                double[] initFriend={E1_FRIEND_INIT,E2_FRIEND_INIT,E3_FRIEND_INIT};
                eva_friend.put(other,initFriend);
                //更新本地社交圈子
                updateSociety(other);
            }else {
                //初始化
                double[] initStranger={E1_STRANGER_INIT,E2_STRANGER_INIT,E3_STRANGER_INIT};
                eva_stranger.put(other,initStranger);
            }
        }
        //获取建立连接的节点的身份
        if (getIndentity(other)==SELFISH){
            setNormalAction(other,DOUBLE);
        }
        //在初始化的同时获取对方的评价，相当于建议数据包，简化该过程
        updateIndirectEva(other);
    }

    public void updateIndirectEva(DTNHost other){
        TRFDDRouter othRouter = (TRFDDRouter)other.getRouter();
        //获取该节点的对三种身份节点的所有评价结果，作为间接评价
        Map<DTNHost, double[]> oth_eva_other=othRouter.getEva_other();
        Map<DTNHost, double[]> oth_eva_stranger=othRouter.getEva_stranger();
        Map<DTNHost, double[]> oth_eva_friend=othRouter.getEva_friend();
        //以之为基础进行间接评价的本地更新
        int flag1=updateIndirect(other,oth_eva_other);
        int flag2=updateIndirect(other,oth_eva_stranger);
        int flag3=updateIndirect(other,oth_eva_friend);
        if (flag1==1&&flag2==1&&flag3==1){
            setMaliciousAction(other,DOUBLE);
        }else if (flag1!=1&&flag2!=1&&flag3!=1){
            setNormalAction(other,BASIC);
        }else {
            setMaliciousAction(other,BASIC);
        }
    }

    //间接评价本地更新
    public int updateIndirect(DTNHost other,Map<DTNHost, double[]> oth_eva){
        int flag=0;
        for (Map.Entry<DTNHost, double[]> eva : oth_eva.entrySet()){
            double simi;
            double[] sim1,sim2;
            sim2=eva.getValue();
            //获得信息的节点是本节点的朋友
            if(this.eva_friend.containsKey(eva.getKey())){
                simi=friend_gamma*getTanimoto(this.eva_friend.get(eva.getKey()),sim2);
                if (simi>=s_th){
                    sim1=this.eva_friend.get(eva.getKey());

                    sim1[0]=(sim1[0]+sim2[0]*simi)/(1+simi);
                    sim1[1]=(sim1[1]+sim2[1]*simi)/(1+simi);
                    sim1[2]=(sim1[2]+sim2[2]*simi)/(1+simi);

                    this.eva_friend.put(eva.getKey(),sim1);
                }else {
                    flag=1;
                }
            }else if (this.eva_stranger.containsKey(eva.getKey())){
                simi=stranger_gamma*getTanimoto(this.eva_stranger.get(eva.getKey()),sim2);
                if (simi>=s_th){
                    sim1=this.eva_stranger.get(eva.getKey());
                    sim1[0]=(sim1[0]+sim2[0]*simi)/(1+simi);
                    sim1[1]=(sim1[1]+sim2[1]*simi)/(1+simi);
                    sim1[2]=(sim1[2]+sim2[2]*simi)/(1+simi);
                    this.eva_stranger.put(eva.getKey(),sim1);
                }else {
                    flag=1;
                }
            }else{
                if (!this.eva_other.containsKey(eva.getKey())){
                    double[] initOther={E1_OTHER_INIT,E2_OTHER_INIT,E3_OTHER_INIT};
                    this.eva_other.put(eva.getKey(),initOther);
                }
                simi=other_gamma*getTanimoto(this.eva_other.get(eva.getKey()),sim2);
                if (simi>=s_th){
                    sim1=this.eva_other.get(eva.getKey());
                    sim1[0]=(sim1[0]+sim2[0]*simi)/(1+simi);
                    sim1[1]=(sim1[1]+sim2[1]*simi)/(1+simi);
                    sim1[2]=(sim1[2]+sim2[2]*simi)/(1+simi);
                    this.eva_other.put(eva.getKey(),sim1);
                }else {
                    flag=1;
                }
            }
        }
        return flag;
    }
    //计算tanimoto相似性结果
    public double getTanimoto(double[] eva_1, double[] eva_2){
        return this.weight*getResult(eva_1,eva_2)/(getResult(eva_1,eva_1)+getResult(eva_2,eva_2)-getResult(eva_1,eva_2));
    }
    //计算向量相乘的结果
    public double getResult(double[] eva_1, double[] eva_2){
        double result=0;
        for (int i=0;i<3;i++){
            result+=eva_1[i]*eva_2[i];
        }
        return result;
    }

    //准确返回节点身份
    public int getIndentity(DTNHost other){
        return getMaxIndex(getEvaFor(other));
    }

    public int getMaxIndex(double[] d){
        double max=d[0];
        if (max>=0.35){
            return 0;
        }
        int index=1;
        for (int i=1;i<d.length;i++) {
            if (d[i]>max) {
                max=d[i];
                index=i;
            }
        }
        return index;
    }

    public void initActions() {
        this.normalAction=new HashMap<DTNHost, Double>();;
        this.selfishAction=new HashMap<DTNHost, Double>();;
        this.maliciousAction=new HashMap<DTNHost, Double>();;
    }

    public void resetActions(DTNHost host){
        normalAction.remove(host);
        selfishAction.remove(host);
        maliciousAction.remove(host);
    }

    public double getNormalAction(DTNHost other){
        return normalAction.containsKey(other)?normalAction.get(other):0;
    }

    public void setNormalAction(DTNHost other,int i){
        if (!normalAction.containsKey(other)){
            normalAction.put(other,0.0);
        }
        normalAction.put(other,countAction(normalAction.get(other),i));
    }

    public double getSelfishAction(DTNHost other){
        return selfishAction.containsKey(other)?selfishAction.get(other):0;
    }

    public void setSelfishAction(DTNHost other,int i){
        if (!selfishAction.containsKey(other)){
            selfishAction.put(other,0.0);
        }
        selfishAction.put(other,countAction(selfishAction.get(other),i));
    }

    public double getMaliciousAction(DTNHost other){
        return maliciousAction.containsKey(other)?maliciousAction.get(other):0;
    }

    public void setMaliciousAction(DTNHost other,int i){
        if (!maliciousAction.containsKey(other)){
            maliciousAction.put(other,0.0);
        }
        maliciousAction.put(other,countAction(maliciousAction.get(other),i));
    }

    public double countAction(double temp,int i){
        switch (i){
            case 0:
                temp+=HALF_RATE;
                break;
            case 1:
                temp+=BASIC_RATE;
                break;
            case 2:
                temp+=DOUBLE_RATE;
                break;
        }
        return temp;
    }

    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);

        if (con.isUp()) {
            DTNHost otherHost = con.getOtherNode(getHost());
            updateCandidate(otherHost);
        }
    }

    @Override
    public void update() {
        super.update();

        if (!canStartTransfer() ||isTransferring()) {
            return; // nothing to transfer or is currently transferring
        }

        // try messages that could be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return;
        }

        tryOtherMessages();

    }

    public Tuple<Message, Connection> tryOtherMessages() {
        List<Tuple<Message, Connection>> messages =
                new ArrayList<Tuple<Message, Connection>>();

        Collection<Message> msgCollection = getMessageCollection();

		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
        for (Connection con : getConnections()) {

            DTNHost other = con.getOtherNode(getHost());
            TRFDDRouter othRouter = (TRFDDRouter)other.getRouter();

            if (othRouter.isTransferring()) {
                continue; // skip hosts that are transferring
            }

            for (Message m : msgCollection) {
                if (othRouter.hasMessage(m.getId())) {
                    continue; // skip messages that the other one has
                }
                if (m.isResponse()){
                    continue;
                }
                if (othRouter.getPredFor(m.getTo()) >= getPredFor(m.getTo())||
                        othRouter.getSimFor(m.getTo()) >= getSimFor(m.getTo())) {
                    messages.add(new Tuple<Message, Connection>(m,con));
                }
            }
        }

        if (messages.size() == 0) {
            return null;
        }
        Collections.sort(messages, new TRFDDRouter.TupleComparator());
        return tryMessagesForConnected(messages);	// try to send messages
    }

    private class TupleComparator implements Comparator
            <Tuple<Message, Connection>> {
        public int compare(Tuple<Message, Connection> tuple1,
                           Tuple<Message, Connection> tuple2) {
            // delivery probability of tuple1's message with tuple1's connection
            double p1 = ((TRFDDRouter)tuple1.getValue().
                    getOtherNode(getHost()).getRouter()).getSimFor(
                    tuple1.getKey().getTo());
            // -"- tuple2...
            double p2 = ((TRFDDRouter)tuple2.getValue().
                    getOtherNode(getHost()).getRouter()).getSimFor(
                    tuple2.getKey().getTo());

            return p1 == p2 ? 0 :
                    (p1 > p2 ? 1 : -1);
        }
    }

    @Override
    public int startTransfer(Message m, Connection con) {
        if (getIndentity(m.getTo())==MALICIOUS||getIndentity(m.getTo())==SELFISH&&isShortage()){
            return DENIED_IDENTITY;
        }
        int retVal=super.startTransfer(m, con);

        if (retVal==RCV_OK){
            setNormalAction(con.getOtherNode(getHost()),BASIC);
        }
        if (retVal==TRY_LATER_BUSY||retVal==DENIED_NO_SPACE||retVal==DENIED_OLD||
                retVal==DENIED_TTL||retVal==DENIED_LOW_RESOURCES||retVal==DENIED_POLICY){
            setSelfishAction(con.getOtherNode(getHost()),HALF);
        }
        return retVal;
    }

    @Override
    protected int checkReceiving(Message m, DTNHost from) {
        int recvCheck = super.checkReceiving(m, from);

        if (isBadIndentity(m,from)||isSelfIndentity(m,from)){
            return DENIED_IDENTITY;
        }

        if (recvCheck==RCV_OK){
            //还需再添加收到反馈时的行为记录
            if (m.getTo()==getHost()&&m.isResponse()){
                int count=m.getRequest().getHopCount();
                /*if (m.getId().contains(RESPONSE_PREFIX)){
                    for (int i=0;i<count;i++){
                        DTNHost test=m.getRequest().getHops().get(i);
                        setNormalAction(test,DOUBLE);
                    }
                    if (m.isRequestDropped()){
                        setMaliciousAction(m.getFrom(),HALF);
                    }
                }else*/if (m.getId().contains(RESPONSE_PREFIX_TWO_HOPS)){
                    int index=count-1;
                    DTNHost test=m.getRequest().getHops().get(index);
                    setNormalAction(test,DOUBLE);
                    if (getIndentity(m.getFrom())==MALICIOUS||getIndentity(m.getFrom())==SELFISH){
                        setNormalAction(m.getFrom(),DOUBLE);
                    }
                    if (m.isRequestDropped()){
                        setMaliciousAction(m.getFrom(),HALF);
                    }
                }
            }else {
                awardRelay(m);
                updateEffectContact(m);
            }
        }

        return recvCheck;
    }

    public boolean isBadIndentity(Message m, DTNHost from){
        return getIndentity(from)==MALICIOUS||getIndentity(m.getFrom())==MALICIOUS||getIndentity(m.getTo())==MALICIOUS;
    }

    public boolean isSelfIndentity(Message m, DTNHost from){
        return getIndentity(from)==SELFISH&&isShortage()||getIndentity(m.getFrom())==SELFISH&&isShortage()
                ||getIndentity(m.getTo())==SELFISH&&isShortage();
    }

    public void initEffectContact(){
        this.effectContact=new HashMap<DTNHost,Double>();
    }

    public double getEffectContact(DTNHost host) {
        return effectContact.containsKey(host)?effectContact.get(host):0;
    }

    public void resetEffectContact(DTNHost host){
        effectContact.remove(host);
    }

    public void updateEffectContact(Message message){
        List<DTNHost> list=message.getHops();
        int length=list.size();
        for (int i=1;i<length;i++){
            double newPred=getEffectContact(list.get(i));
            int distance=length-i;
            newPred+=Math.exp(-mu*(distance-1));
            effectContact.put(list.get(i),newPred);
        }
    }

    //半速奖励中继节点
    public void awardRelay(Message message){
        for (DTNHost relay:message.getHops()){
            setNormalAction(relay,BASIC);
        }
    }

    public boolean isShortage(){
        if (getFreeBufferSize()<0.6*getBufferSize()){
            return true;
        }
        return false;
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message m=super.messageTransferred(id, from);
        feedbackMessage(m);
        return m;
    }

    public void feedbackMessage(Message m){
        /*List<DTNHost> list=m.getHops();
        if (m.getTo() != getHost() && m.getResponseSize() > 0&&list.size()>2){
            Message res = new Message(this.getHost(),list.get(list.size()-3),
                    RESPONSE_PREFIX_TWO_HOPS+m.getId(), m.getResponseSize());
            createNewMessage(res);
            getMessage(RESPONSE_PREFIX_TWO_HOPS+m.getId()).setRequest(m);
            //System.out.println("did it");
        }*/
    }

    @Override
    public MessageRouter replicate() {
        TRFDDRouter r = new TRFDDRouter(this);
        return r;
    }

}

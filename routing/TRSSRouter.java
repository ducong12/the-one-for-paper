package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import util.Tuple;

import java.util.*;

/**
 * Created by ducong on 2018/5/14.
 */
public class TRSSRouter extends ActiveRouter{

    public static final double DEFAULT_BETA = 2;
    public static final double DEFAULT_ALPHA = 0.5;
    public static final double T_THRESHOLD=0.5;
    public static final double DEFAULT_MATCH = 1/3.0;

    public static final String TRSS_NS = "TRSSRouter";
    public static final String BETA_S = "beta";
    public static final String ALPHA_S = "alpha";
    public static final String T_THRESHOLD_S="t_th";
    public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";
    public static final String MATCH_S = "match";

    private Map<DTNHost, Double> sims;
    private Map<DTNHost, Double> trusts;

    private double beta;
    private double alpha;
    private double t_th;
    private double match;

    private int secondsInTimeUnit;

    public TRSSRouter(Settings s) {
        super(s);
        Settings trssSettings = new Settings(TRSS_NS);
        secondsInTimeUnit = trssSettings.getInt(SECONDS_IN_UNIT_S);
        if (trssSettings.contains(BETA_S)) {
            beta = trssSettings.getDouble(BETA_S);
        }
        else {
            beta = DEFAULT_BETA;
        }
        if (trssSettings.contains(ALPHA_S)) {
            alpha = trssSettings.getDouble(ALPHA_S);
        }
        else {
            alpha = DEFAULT_ALPHA;
        }
        if (trssSettings.contains(T_THRESHOLD_S)) {
            t_th = trssSettings.getDouble(T_THRESHOLD_S);
        }
        else {
            t_th = T_THRESHOLD;
        }
        if (trssSettings.contains(MATCH_S)) {
            match = trssSettings.getDouble(MATCH_S);
        }
        else {
            match = DEFAULT_MATCH;
        }
        initSims();
        initTrusts();
    }


    protected TRSSRouter(TRSSRouter r) {
        super(r);
        this.beta=r.beta;
        this.alpha=r.alpha;
        this.t_th=r.t_th;
        initSims();
        initTrusts();
    }

    private void initTrusts() {
        this.trusts = new HashMap<DTNHost, Double>();
    }

    private void initSims() {
        this.sims = new HashMap<DTNHost, Double>();
    }
    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);

        if (con.isUp()) {
            DTNHost otherHost = con.getOtherNode(getHost());
            updateSociety(otherHost);
            updateTransitivePreds(otherHost);
        }
    }

    public void updateDeliveryTrustFor(DTNHost host){
        double oldValue = getDeliveryTrustFor(host);
        double re_oldValue=((TRSSRouter)host.getRouter()).getTrustFor(getHost());
        double newValue;
        double x;
        if (oldValue>=0.5){
            x=Math.log(2*(1-oldValue))/(-1*beta)+re_oldValue;
        }else {
            x=Math.log(2*oldValue)/beta+re_oldValue;
        }
        if (x>=0){
            newValue=1-(1-oldValue)*Math.exp(beta*re_oldValue);
        }else {
            newValue=oldValue*Math.exp(beta*re_oldValue);
        }
        trusts.put(host,newValue);
    }

    public double getTrustFor(DTNHost host) {
        if (trusts.containsKey(host)) {
            return trusts.get(host);
        }
        else {
            return 0.5;
        }
    }

    public double getDeliveryTrustFor(DTNHost host) {
        if (trusts.containsKey(host)) {
            return trusts.get(host);
        }
        else {
            return getSimFor(host)>0.5?getSimFor(host):0.5;
        }
    }
    public double getSimFor(DTNHost host) {
        return societySim(host);
    }

    public Map<DTNHost, Double> getDeliveryTrusts() {
        return this.trusts;
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

    public void updateTransitivePreds(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof ProphetRouter : "PRoPHET only works " +
                " with other routers of same type";

        double pForHost = getTrustFor(host); // P(a,b)
        Map<DTNHost, Double> othersTrusts =
                ((TRSSRouter)otherRouter).getDeliveryTrusts();

        for (Map.Entry<DTNHost, Double> e : othersTrusts.entrySet()) {
            if (e.getKey() == getHost()) {
                continue; // don't add yourself
            }

            double pOld = getTrustFor(e.getKey()); // P(a,c)_old
            double pNew = (e.getValue()-pOld)*pForHost+pOld;
            trusts.put(e.getKey(), pNew);
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
            TRSSRouter othRouter = (TRSSRouter)other.getRouter();

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
                if (othRouter.getTrustFor(m.getTo())>0.5||othRouter.getSimFor(m.getTo()) > getSimFor(m.getTo())) {
                    // the other node has higher probability of delivery
                    messages.add(new Tuple<Message, Connection>(m,con));
                }
            }
        }

        if (messages.size() == 0) {
            return null;
        }

        // sort the message-connection tuples
        Collections.sort(messages, new TupleComparator());
        return tryMessagesForConnected(messages);	// try to send messages
    }

    private class TupleComparator implements Comparator
            <Tuple<Message, Connection>> {
        public int compare(Tuple<Message, Connection> tuple1,
                           Tuple<Message, Connection> tuple2) {
            // delivery probability of tuple1's message with tuple1's connection
            double p1 = ((TRSSRouter)tuple1.getValue().
                    getOtherNode(getHost()).getRouter()).getTrustFor(
                    tuple1.getKey().getTo());
            // -"- tuple2...
            double p2 = ((TRSSRouter)tuple2.getValue().
                    getOtherNode(getHost()).getRouter()).getTrustFor(
                    tuple2.getKey().getTo());

            return p1 == p2 ? 0 :
                    (p1 > p2 ? 1 : -1);
        }
    }

    @Override
    protected int checkReceiving(Message m, DTNHost from) {
        int recvCheck = super.checkReceiving(m, from);
        if (recvCheck==RCV_OK){
            //还需再添加收到反馈时的行为记录
            if (m.getTo()==getHost()&&m.isResponse()){
                int count=m.getRequest().getHopCount();
                if (m.getId().contains(RESPONSE_PREFIX)){
                    updateDeliveryTrustFor(m.getFrom());
                }else if (m.getId().contains(RESPONSE_PREFIX_TWO_HOPS)){
                    int index=count-1;
                    DTNHost test=m.getRequest().getHops().get(index);
                    updateDeliveryTrustFor(test);
                }
            }
        }
        return recvCheck;
    }

    @Override
    public MessageRouter replicate() {
        return new TRSSRouter(this);
    }
}

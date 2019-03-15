package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import util.Tuple;

import java.util.*;

/**
 * Created by ducong on 2018/5/5.
 */
public class MaliciousRouter extends TRFDDRouter{

    private double[] good;
    private double[] medium;
    private double[] bad;

    public MaliciousRouter(Settings s) {
        super(s);
        initMevas();
    }

    public MaliciousRouter(TRFDDRouter r) {
        super(r);
        initMevas();
    }

    @Override
    public void initPreds() {

    }

    @Override
    public void initSims() {

    }

    @Override
    public void initActions() {

    }

    @Override
    public void initEffectContact() {

    }

    private void initMevas(){
        good=new double[]{1, 0, 0};
        medium=new double[]{0, 1, 0};
        bad=new double[]{0, 0, 1};
    }

    @Override
    public void updateDirectEvaluation(DTNHost other, double[] oldValue) {
        return;
    }

    @Override
    protected int checkReceiving(Message m, DTNHost from) {
        return RCV_OK;
    }

    @Override
    public void updateCandidate(DTNHost other) {
        if (isFriend(other)){
            super.eva_friend.put(other,good);
        }else {
            int rand=(int)(Math.random()*2);
            if (rand==1){
                super.eva_other.put(other,bad);
            }else {
                super.eva_stranger.put(other,medium);
            }
        }
    }

    @Override
    public double getPredFor(DTNHost host) {
        return 10;
    }

    @Override
    public double getSimFor(DTNHost host) {
        return 1;
    }

    @Override
    public void updateMetrics() {
        return;
    }

    @Override
    public void feedbackMessage(Message m){


        if (getMessageCollection().contains(m)){
            deleteMessage(m.getId(),true);
        }
    }

    @Override
    public int startTransfer(Message m, Connection con) {
        int retVal;

        if (!con.isReadyForTransfer()) {
            return TRY_LATER_BUSY;
        }

        if (!super.getPolicy().acceptSending(getHost(),
                con.getOtherNode(getHost()), con, m)) {
            return MessageRouter.DENIED_POLICY;
        }

        retVal = con.startTransfer(getHost(), m);
        if (retVal == RCV_OK) { // started transfer
            addToSendingConnections(con);
        }
        else if (deleteDelivered && retVal == DENIED_OLD &&
                m.getTo() == con.getOtherNode(this.getHost())) {
			/* final recipient has already received the msg -> delete it */
            this.deleteMessage(m.getId(),true);
        }

        return retVal;
    }

    @Override
    public Tuple<Message, Connection> tryOtherMessages() {
        return null;
    }

    @Override
    public MaliciousRouter replicate() {
        MaliciousRouter r=new MaliciousRouter(this);
        return r;
    }
}

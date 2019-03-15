package routing;

import core.*;
import util.Tuple;

/**
 * Created by ducong on 2018/5/18.
 */
public class MaliciousRouterT extends TRSSRouter{

    public MaliciousRouterT(Settings s) {
        super(s);
    }

    protected MaliciousRouterT(TRSSRouter r) {
        super(r);
    }

    @Override
    public void updateDeliveryTrustFor(DTNHost host) {
        if (Math.random()>0.5){
            getDeliveryTrusts().put(host,0.0);
        }else {
            getDeliveryTrusts().put(host,0.5);
        }

    }

    @Override
    public void updateTransitivePreds(DTNHost host) {

    }

    @Override
    public double getSimFor(DTNHost host) {
        return 1;
    }

    @Override
    public double getTrustFor(DTNHost host) {
        return 1;
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message m = super.messageTransferred(id, from);

        /*if (m.getTo() != getHost() && m.getResponseSize() > 0&&m.getHopCount()>2){
            Message res = new Message(this.getHost(),m.getHops().get(m.getHopCount()-2),
                    RESPONSE_PREFIX_TWO_HOPS+m.getId(), m.getResponseSize());
            createNewMessage(res);
            getMessage(RESPONSE_PREFIX_TWO_HOPS+m.getId()).setRequest(m);
        }*/
        if (getMessageCollection().contains(m)&&Math.random()>=0.7){
            deleteMessage(m.getId(),true);
        }
        return m;
    }

    @Override
    public Tuple<Message, Connection> tryOtherMessages() {
        return null;
    }

    @Override
    public MaliciousRouterT replicate() {
        return new MaliciousRouterT(this);
    }

}

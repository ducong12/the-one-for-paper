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
public class SelfishRouter extends TRFDDRouter{

    public SelfishRouter(Settings s) {
        super(s);
    }

    public SelfishRouter(TRFDDRouter r) {
        super(r);
    }

    @Override
    protected int checkReceiving(Message m, DTNHost from) {
        if (m.getTo() == getHost()){
            return RCV_OK;
        }
        if (!isFriend(from)){
            int num=(int)(Math.random()*5);
            if (num==0){
                return TRY_LATER_BUSY;
            }else if (num==1){
                return DENIED_NO_SPACE;
            }else if (num==2){
                return DENIED_OLD;
            }else if (num==3){
                return DENIED_TTL;
            }else if (num==4){
                return DENIED_LOW_RESOURCES;
            }else
                return DENIED_POLICY;
        }
        return super.checkReceiving(m,from);
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
            this.deleteMessage(m.getId(), false);
        }

        return retVal;
    }

    @Override
    public Tuple<Message, Connection> tryOtherMessages() {
        List<Tuple<Message, Connection>> messages =
                new ArrayList<Tuple<Message, Connection>>();

        Collection<Message> msgCollection = getMessageCollection();
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
                if (othRouter.getPredFor(m.getTo()) >= getPredFor(m.getTo())&&
                        othRouter.getSimFor(m.getTo()) >= getSimFor(m.getTo())) {
                    messages.add(new Tuple<Message, Connection>(m,con));
                }
            }
        }
        if (messages.size() == 0) {
            return null;
        }
        return tryMessagesForConnected(messages);	// try to send messages
    }

    @Override
    public void feedbackMessage(Message m){
        /*List<DTNHost> list=m.getHops();
        if (m.getTo() == getHost() && m.getResponseSize() > 0&&list.size()>2) {
            // generate a response message
            for (int i=1;i<list.size();i++){
                Message res = new Message(this.getHost(),list.get(i),
                        RESPONSE_PREFIX+m.getId(), m.getResponseSize());
                res.setRequest(m);
                res.setRequestIgnored(true);
                createNewMessage(res);
            }
        }*/
        if (getMessageCollection().contains(m)&&Math.random()>=0.7){
            deleteMessage(m.getId(),true);
        }
    }

    @Override
    public SelfishRouter replicate() {
        SelfishRouter r = new SelfishRouter(this);
        return r;
    }
}

package routing;

import core.*;
import util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by ducong on 2018/5/14.
 */
public class MaliciousRouterE extends EpidemicRouter{

    public MaliciousRouterE(Settings s) {
        super(s);
    }

    public MaliciousRouterE(MaliciousRouterE r) {
        super(r);
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
            this.deleteMessage(m.getId(), true);
        }

        return retVal;
    }

    @Override
    public int receiveMessage(Message m, DTNHost from) {
        Message newMessage = m.replicate();

        this.putToIncomingBuffer(newMessage, from);
        newMessage.addNodeOnPath(getHost());

        for (MessageListener ml : this.mListeners) {
            ml.messageTransferStarted(newMessage, from, getHost());
        }

        return RCV_OK; // superclass always accepts messages
    }

    @Override
    public void sendMessage(String id, DTNHost to) {
        Message m = getMessage(id);
        Message m2;
        if (m == null) throw new SimError("no message for id " +
                id + " to send at " + getHost());

        m2 = m.replicate();	// send a replicate of the message
        if (m.getTo()==to&&m.getFrom()==getHost())
            to.receiveMessage(m2, getHost());
    }



    public Message messageTransferred(String id, DTNHost from) {
        Message incoming = removeFromIncomingBuffer(id, from);
        boolean isFinalRecipient;
        boolean isFirstDelivery; // is this first delivered instance of the msg


        if (incoming == null) {
            throw new SimError("No message with ID " + id + " in the incoming "+
                    "buffer of " + getHost());
        }

        incoming.setReceiveTime(SimClock.getTime());

        // Pass the message to the application (if any) and get outgoing message
        Message outgoing = incoming;
        for (Application app : getApplications(incoming.getAppID())) {
            // Note that the order of applications is significant
            // since the next one gets the output of the previous.
            outgoing = app.handle(outgoing, getHost());
            if (outgoing == null) break; // Some app wanted to drop the message
        }

        Message aMessage = (outgoing==null)?(incoming):(outgoing);
        // If the application re-targets the message (changes 'to')
        // then the message is not considered as 'delivered' to this host.
        isFinalRecipient = aMessage.getTo() == getHost();
        isFirstDelivery = isFinalRecipient &&
                !isDeliveredMessage(aMessage);

        if (!isFinalRecipient && outgoing!=null) {
            // not the final recipient and app doesn't want to drop the message
            // -> put to buffer
            addToMessages(aMessage, false);
            deleteMessage(aMessage.getId(),true);
        } else if (isFirstDelivery) {
            this.deliveredMessages.put(id, aMessage);
        } else if (outgoing == null) {
            // Blacklist messages that an app wants to drop.
            // Otherwise the peer will just try to send it back again.
            this.blacklistedMessages.put(id, null);
        }

        for (MessageListener ml : this.mListeners) {
            ml.messageTransferred(aMessage, from, getHost(),
                    isFirstDelivery);
        }

        return aMessage;
    }

    @Override
    public MaliciousRouterE replicate() {
        return new MaliciousRouterE(this);
    }

}

package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by ducong on 2018/5/16.
 */
public class SelfishRouterP extends ProphetRouter{

    public SelfishRouterP(Settings s) {
        super(s);
    }

    protected SelfishRouterP(ProphetRouter r) {
        super(r);
    }

    @Override
    protected int checkReceiving(Message m, DTNHost from) {
        if (m.getTo()==getHost()){
            return RCV_OK;
        }else {
            return DENIED_IDENTITY;
        }
    }

    @Override
    public void update() {
        return;
    }

    @Override
    public Tuple<Message, Connection> tryOtherMessages() {
        List<Tuple<Message, Connection>> messages =
                new ArrayList<Tuple<Message, Connection>>();

        Collection<Message> msgCollection = getMessageCollection();

		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
        for (Connection con : getConnections()) {
            DTNHost other = con.getOtherNode(getHost());
            ProphetRouter othRouter = (ProphetRouter)other.getRouter();


            if (othRouter.isTransferring()) {
                continue; // skip hosts that are transferring
            }

            for (Message m : msgCollection) {
                if (othRouter.hasMessage(m.getId())) {
                    continue; // skip messages that the other one has
                }
                if (m.getTo()==other) {
                    // the other node has higher probability of delivery
                    messages.add(new Tuple<Message, Connection>(m,con));
                }
            }
        }

        if (messages.size() == 0) {
            return null;
        }

        // sort the message-connection tuples
        //Collections.sort(messages, new TupleComparator());
        return tryMessagesForConnected(messages);
    }
}

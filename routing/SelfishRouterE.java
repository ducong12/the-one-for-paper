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
 * Created by ducong on 2018/5/14.
 */
public class SelfishRouterE extends EpidemicRouter{

    public SelfishRouterE(Settings s) {
        super(s);
    }

    public SelfishRouterE(SelfishRouterE r) {
        super(r);
    }

    @Override
    protected int checkReceiving(Message m, DTNHost from) {

            return DENIED_IDENTITY;
    }

    @Override
    public int startTransfer(Message m, Connection con) {
        return DENIED_IDENTITY;
    }


    @Override
    public Message messageTransferred(String id, DTNHost from) {
        return null;
    }

    @Override
    public SelfishRouterE replicate() {
        return new SelfishRouterE(this);
    }

}

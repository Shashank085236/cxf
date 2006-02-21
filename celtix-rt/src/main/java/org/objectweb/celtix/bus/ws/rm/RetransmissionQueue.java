package org.objectweb.celtix.bus.ws.rm;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.handler.MessageContext;

import org.objectweb.celtix.ws.rm.Identifier;
import org.objectweb.celtix.ws.rm.SequenceType;

public class RetransmissionQueue {
    private Map<Identifier, List<MessageContext>> map;

    public RetransmissionQueue() {
        map = new HashMap<Identifier, List<MessageContext>>();
    }

    /**
     * Store the message context in the list associated with the sequence's
     * identifier.
     * 
     * @param ctx the message context.
     */
    public void put(MessageContext ctx) {
        SequenceType st = RMContextUtils.retrieveSequence(ctx);
        Identifier sid = st.getIdentifier();
        List<MessageContext> items = map.get(sid);
        if (null == items) {
            items = new ArrayList<MessageContext>();
            map.put(sid, items);
        }
        items.add(ctx);
    }

    /**
     * Evicts all messages for the sequence specified by the identifier that
     * have been acknowledged.
     * 
     * @param sid the sequence identifier.
     * @param seq the sequence object.
     */

    void evict(Sequence seq) {
        List<MessageContext> unacked = map.get(seq.getIdentifier());
        if (null != unacked) {
            for (int i = unacked.size() - 1; i >= 0; i--) {
                MessageContext ctx = unacked.get(i);
                SequenceType st = RMContextUtils.retrieveSequence(ctx);
                BigInteger m = st.getMessageNumber();
                if (seq.isAcknowledged(m)) {
                    unacked.remove(i);
                }
            }
        }
    }

    public int countUnacknowledged(Sequence seq) {
        List<MessageContext> unacked = map.get(seq.getIdentifier());
        return unacked == null ? 0 : unacked.size();
    }
}

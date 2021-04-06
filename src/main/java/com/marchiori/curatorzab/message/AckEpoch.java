/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

import com.marchiori.curatorzab.Zxid;
import java.util.List;

/**
 * ACKEPOCH message.
 *
 * @author Matteo Marchiori
 */
public class AckEpoch extends Message {

    /**
     * Epoch of the message.
     */
    private final int epoch;

    /**
     * History of the sender.
     */
    private final List<Transaction> history;

    /**
     * last ZXID.
     */
    private final Zxid zxid;

    /**
     * Message constructor.
     *
     * @param e epoch of the message
     * @param h history of the sender.
     * @param z zxid.
     * @param s sender.
     */
    public AckEpoch(
            final int e,
            final List<Transaction> h,
            final Zxid z,
            final String s
    ) {
        super(MessageType.ACKEPOCH, s);
        epoch = e;
        history = h;
        zxid = z;
    }

    /**
     * Epoch getter.
     *
     * @return epoch.
     */
    public final int getEpoch() {
        return epoch;
    }

    /**
     * History getter.
     *
     * @return history.
     */
    public final List<Transaction> getHistory() {
        return history;
    }

    /**
     * ZXID getter.
     *
     * @return zxid.
     */
    public final Zxid getZxid() {
        return zxid;
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

import java.util.List;

/**
 * ACKNEWLEADER message.
 *
 * @author Matteo Marchiori
 */
public class AckNewLeader extends Message {

    /**
     * Epoch of the message.
     */
    private final int epoch;

    /**
     * History of the sender.
     */
    private final List<Transaction> history;

    /**
     * Constructor for ACKNEWLEADER.
     *
     * @param e epoch of the message.
     * @param h history of the sender.
     * @param sender sender.
     */
    public AckNewLeader(
            final int e,
            final List<Transaction> h,
            final String sender
    ) {
        super(MessageType.ACKNEWLEADER, sender);
        this.epoch = e;
        this.history = h;
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

}

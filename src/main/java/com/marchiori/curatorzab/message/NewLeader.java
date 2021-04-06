/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

import java.util.List;

/**
 * NEWLEADER message.
 *
 * @author Matteo Marchiori
 */
public class NewLeader extends Message {

    /**
     * Epoch of the message.
     */
    private final int epoch;

    /**
     * History of the sender.
     */
    private final List<Transaction> history;

    /**
     * NEWLEADER cnstructor.
     *
     * @param e epoch of the message.
     * @param h history of the sender.
     * @param sender sender.
     */
    public NewLeader(
            final int e,
            final List<Transaction> h,
            final String sender
    ) {
        super(MessageType.NEWLEADER, sender);
        epoch = e;
        history = h;
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

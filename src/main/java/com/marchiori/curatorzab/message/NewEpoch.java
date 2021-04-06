/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

/**
 * NEWEPOCH message.
 *
 * @author Matteo Marchiori
 */
public class NewEpoch extends Message {

    /**
     * Epoch of the message.
     */
    private final int epoch;

    /**
     * NEWEPOCH constructor.
     *
     * @param e epoch of the message.
     * @param sender sender of the message.
     */
    public NewEpoch(final int e, final String sender) {
        super(MessageType.NEWEPOCH, sender);
        epoch = e;
    }

    /**
     * Epoch getter.
     *
     * @return epoch.
     */
    public final int getEpoch() {
        return epoch;
    }

}

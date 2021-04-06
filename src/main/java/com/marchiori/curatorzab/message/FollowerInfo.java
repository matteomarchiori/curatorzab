/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

/**
 * FOLLOWERINFO message.
 *
 * @author Matteo Marchiori
 */
public class FollowerInfo extends Message {

    /**
     * Epoch of the message.
     */
    private final int epoch;

    /**
     * FOLLOWERINFO constructor.
     *
     * @param sender sender of the message.
     * @param e epoch of the message.
     */
    public FollowerInfo(final String sender, final int e) {
        super(MessageType.FOLLOWERINFO, sender);
        epoch = e;
    }

    /**
     * Epoch getter.
     *
     * @return epoch.
     */
    public final int getAcceptedEpoch() {
        return epoch;
    }

}

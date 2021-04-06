/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

/**
 * PROPOSE message.
 *
 * @author Matteo Marchiori
 */
public class Propose extends Message {

    /**
     * Value of propose.
     */
    private final int v;

    /**
     * Constructor of PROPOSE.
     *
     * @param value value of propose.
     * @param sender sender of message.
     */
    public Propose(final int value, final String sender) {
        super(MessageType.PROPOSE, sender);
        this.v = value;
    }

    /**
     * Value getter.
     *
     * @return value.
     */
    public final int getV() {
        return v;
    }

}

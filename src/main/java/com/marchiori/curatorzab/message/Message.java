/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

import java.io.Serializable;

/**
 * Message of ZAB.
 *
 * @author Matteo Marchiori
 */
public abstract class Message implements Serializable {

    /**
     * Type of the message.
     */
    private final MessageType type;

    /**
     * Sender of the message.
     */
    private final String sender;

    /**
     * Message constructor.
     *
     * @param t type of the message.
     * @param s sender of the message.
     */
    public Message(final MessageType t, final String s) {
        type = t;
        sender = s;
    }

    /**
     * Type of message.
     *
     * @return type of message.
     */
    public final MessageType getType() {
        return type;
    }

    /**
     * Sender of message.
     *
     * @return sender of message.
     */
    public final String getSender() {
        return sender;
    }
}

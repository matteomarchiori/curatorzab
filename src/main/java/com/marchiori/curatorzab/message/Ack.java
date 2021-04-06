/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

/**
 * ACK message.
 *
 * @author Matteo Marchiori
 */
public class Ack extends Message {

    /**
     * Message contructor.
     *
     * @param s sender of the message.
     */
    public Ack(final String s) {
        super(MessageType.ACK, s);
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

/**
 * CLEAR message.
 *
 * @author Matteo Marchiori
 */
public class Clear extends Message {

    /**
     * CLEAR constructor.
     *
     * @param sender sender of the message.
     */
    public Clear(final String sender) {
        super(MessageType.CLEAR, sender);
    }
}

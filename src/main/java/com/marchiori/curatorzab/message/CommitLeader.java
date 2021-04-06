/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

/**
 * COMMITLEADER message.
 *
 * @author Matteo Marchiori
 */
public class CommitLeader extends Message {

    /**
     * COMMMITLEADER contructor.
     *
     * @param sender sender of the message.
     */
    public CommitLeader(final String sender) {
        super(MessageType.COMMITLEADER, sender);
    }
}

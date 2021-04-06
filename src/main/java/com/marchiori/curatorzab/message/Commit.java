/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

/**
 * COMMIT message.
 *
 * @author Matteo Marchiori
 */
public class Commit extends Message {

    /**
     * COMMIT constructor.
     *
     * @param sender sender of the message.
     */
    public Commit(final String sender) {
        super(MessageType.COMMIT, sender);
    }

}

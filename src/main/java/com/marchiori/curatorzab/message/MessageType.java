/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

import java.io.Serializable;

/**
 * Message types.
 *
 * @author Matteo Marchiori
 */
public enum MessageType implements Serializable {
    /**
     * Message types.
     */
    ACK, ACKEPOCH, ACKNEWLEADER, CLEAR, COMMIT, COMMITLEADER, FOLLOWERINFO,
    /**
     * Message types.
     */
    NEWEPOCH, NEWLEADER, PROPOSE, RETRY, TRANSACTION;
}

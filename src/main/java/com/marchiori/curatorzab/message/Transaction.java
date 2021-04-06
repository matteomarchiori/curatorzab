/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.message;

import com.marchiori.curatorzab.Zxid;

/**
 * Transaction in ZAB.
 *
 * @author Matteo Marchiori
 */
public class Transaction extends Message {

    /**
     * Value of transaction.
     */
    private final int v;

    /**
     * ZXID of transaction.
     */
    private final Zxid z;

    /**
     * Constructor of transaction.
     *
     * @param value value of transaction.
     * @param zxid zxid of transaction.
     * @param sender sender of transaction.
     */
    public Transaction(
            final int value,
            final Zxid zxid,
            final String sender
    ) {
        super(MessageType.TRANSACTION, sender);
        v = value;
        z = zxid;
    }

    /**
     * Value getter.
     *
     * @return value.
     */
    public final int getV() {
        return v;
    }

    /**
     * ZXID getter.
     *
     * @return ZXID.
     */
    public final Zxid getZ() {
        return z;
    }

}

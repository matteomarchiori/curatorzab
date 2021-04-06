/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab;

import java.io.Serializable;

/**
 * ZXID representation.
 *
 * @author Matteo Marchiori
 */
public class Zxid implements Serializable, Comparable<Zxid> {

    /**
     * Epoch and counter.
     */
    private final int epoch, counter;

    /**
     * Constructor for new ZXID.
     *
     * @param e epoch.
     * @param c counter.
     */
    public Zxid(final int e, final int c) {
        epoch = e;
        counter = c;
    }

    /**
     * Epoch getter.
     *
     * @return epoch of the ZXID.
     */
    public final int getE() {
        return epoch;
    }

    /**
     * Counter getter.
     *
     * @return counter of the ZXID.
     */
    public final int getC() {
        return counter;
    }

    /**
     * Comparer for two ZXID.
     *
     * @param t the other ZXID.
     * @return -1 if current ZXID lower than t, 1 if current ZXID higher, 0
     * otherwise.
     */
    @Override
    public final int compareTo(final Zxid t) {
        if (epoch < t.epoch) {
            return -1;
        }
        if (epoch > t.epoch) {
            return 1;
        }
        if (counter < t.counter) {
            return -1;
        }
        if (counter > t.counter) {
            return 1;
        }
        return 0;
    }

}

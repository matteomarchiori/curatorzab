/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Peer launcher.
 *
 * @author Matteo Marchiori
 */
public final class Zab {

    /**
     * Hiding useless constructor.
     */
    private Zab() {
    }

    /**
     * Executable.
     *
     * @param args args.
     */
    public static void main(String args[]) {
        try {
            new Peer();
        } catch (Exception ex) {
            Logger.getLogger(Zab.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

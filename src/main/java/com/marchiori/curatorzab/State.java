/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab;

/**
 * Enum to represent the state of a node during ZAB algorithm.
 *
 * @author Matteo Marchiori
 */
public enum State {
    /**
     * States in ZAB algorithm.
     */
    FOLLOWING, LEADING, ELECTION;
}

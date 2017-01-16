/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.api.processor.utils;

/**
 *
 * @author dancc
 * @param <K>
 * @param <V>
 */
public class Pair<K, V> {

    private final K left;
    private final V right;

    public Pair(K left, V right) {
        this.left = left;
        this.right = right;
    }

    public K left() {
        return left;
    }

    public V right() {
        return right;
    }
    
    @Override
    public String toString() {
        return "left=" + left + ", right=" + right;
    }
    
    public static <K, V> Pair<K, V> of(K left, V right) {
        return new Pair(left, right);
    }
}

package ru.ifmo.ctddev.yaglamunov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        try {
//            new Implementor().implement(javax.imageio.IIOException.class, Paths.get("implementor"));
//            new Implementor().implement(javax.management.ImmutableDescriptor.class, Paths.get("implementor"));
//            new Implementor().implement(javax.naming.ldap.LdapReferralException.class, Paths.get("implementor"));
//            new Implementor().implement(javax.annotation.Generated.class, Paths.get("implementor"));
//            new Implementor().implement(javax.sql.rowset.CachedRowSet.class, Paths.get("implementor"));
            new Implementor().implement(org.omg.CORBA_2_3.ORB.class, Paths.get("implementor"));
        } catch (ImplerException e) {
            e.printStackTrace();
        }
    }
}

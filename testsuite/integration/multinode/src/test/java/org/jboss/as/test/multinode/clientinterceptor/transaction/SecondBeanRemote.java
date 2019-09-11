package org.jboss.as.test.multinode.clientinterceptor.transaction;

public interface SecondBeanRemote {

    void clear();
    void enlistResource();
    boolean areResourcesRollbacked();
    String secondBeanEcho(String echoMsh);
}

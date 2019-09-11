package org.jboss.as.test.multinode.clientinterceptor.transaction;

import java.util.ArrayList;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class MyXAResource implements XAResource {

    public enum State {
        COMMIT,
        END,
        FORGET,
        START,
        ROLLBACK,
        RECOVER,
        PREPARE,
    }

    private ArrayList<State> phases = new ArrayList<>();
    private int timeout;

    public ArrayList<State> getPhases() {
        return phases;
    }

    @Override
    public void commit(Xid xid, boolean b) throws XAException {
        phases.add(State.COMMIT);
    }

    @Override
    public void end(Xid xid, int i) throws XAException {
        phases.add(State.END);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        phases.add(State.FORGET);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return timeout;
    }

    @Override
    public boolean isSameRM(XAResource xaResource) throws XAException {
        return false;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        phases.add(State.PREPARE);
        return XA_OK;
    }

    @Override
    public Xid[] recover(int i) throws XAException {
        phases.add(State.RECOVER);
        return new Xid[0];
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        phases.add(State.ROLLBACK);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        this.timeout = seconds;
        return true;
    }

    @Override
    public void start(Xid xid, int i) throws XAException {
        phases.add(State.START);
    }
}

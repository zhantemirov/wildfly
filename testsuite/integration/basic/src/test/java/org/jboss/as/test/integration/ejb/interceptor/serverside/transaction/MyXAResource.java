/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.interceptor.serverside.transaction;

import java.util.ArrayList;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
public class MyXAResource implements XAResource {

    private ArrayList<State> phases = new ArrayList<>();
    private int timeout;

    public enum State {
        COMMIT, END, FORGET, START, ROLLBACK, RECOVER, PREPARE
    }

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

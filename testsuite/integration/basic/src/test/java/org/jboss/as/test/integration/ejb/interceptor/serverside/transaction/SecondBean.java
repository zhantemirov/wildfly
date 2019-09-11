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
import java.util.List;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.transaction.TransactionManager;
import org.jboss.logging.Logger;

/**
 * A simple stateless bean enlisting XA resources and checking whether they are rollbacked.
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@Stateless
public class SecondBean {
    private static Logger log = Logger.getLogger(SecondBean.class);

    private List<MyXAResource> enlistedResources = new ArrayList<>();

    @Resource(lookup = "java:/TransactionManager")
    TransactionManager txm;

    public void clear() {
        enlistedResources = new ArrayList<>();
    }

    public void enlistResource() {
        MyXAResource xar = new MyXAResource();
        try {
            txm.getTransaction().enlistResource(xar);
            enlistedResources.add(xar);
        } catch (Exception e) {
            log.info("An exception while enlisting XA resource", e);
        }
    }

    public boolean areResourcesRollbacked() {
        for (MyXAResource resource : enlistedResources) {
            if (!resource.getPhases().contains(MyXAResource.State.ROLLBACK)) {
                return false;
            }
        }
        return true;
    }

    public String secondBeanEcho(String echoMsh) throws RuntimeException {
        return echoMsh;
    }

}

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

import java.util.Collections;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.interceptor.serverside.AbstractServerInterceptorsSetupTask;
import org.jboss.as.test.integration.ejb.interceptor.serverside.InterceptorModule;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test case checking the transaction is rollbacked
 * after a checked exception has been thrown from a server-side interceptor while calling a deployed EJB method.
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@ServerSetup(TransactionExceptionTestCase.SetupTask.class)
public class TransactionExceptionTestCase {

    private static final String MODULE_NAME = "interceptor-transaction";

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(TransactionExceptionTestCase.class.getPackage());
        jar.addPackage(AbstractServerInterceptorsSetupTask.class.getPackage());
        jar.addPackage(MyXAResource.class.getPackage());
        return jar;
    }

    @Test
    public void test() throws NamingException, RuntimeException {
        final InitialContext ctx = new InitialContext();
        FirstBean bean = (FirstBean) ctx.lookup("java:module/" + FirstBean.class.getSimpleName());

        try {
            bean.callSecondBeanMethod("secondBeanEcho");
            Assert.fail("Interceptor was supposed to throw RuntimeException");
        } catch (Exception expected) {
            // expected
        }

        Assert.assertTrue(bean.areResourcesRollbacked());

    }

    static class SetupTask extends AbstractServerInterceptorsSetupTask.SetupTask {
        @Override
        public List<InterceptorModule> getModules() {
            return Collections.singletonList(new InterceptorModule(
                    TransactionInterceptor.class,
                    MODULE_NAME,
                    "module.xml",
                    TransactionExceptionTestCase.class.getResource("module.xml"),
                    "server-side-interceptor.jar"
            ));
        }
    }
}

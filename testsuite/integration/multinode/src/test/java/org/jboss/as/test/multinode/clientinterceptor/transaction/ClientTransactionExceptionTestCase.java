package org.jboss.as.test.multinode.clientinterceptor.transaction;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createFilePermission;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import java.security.SecurityPermission;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.as.test.shared.integration.interceptor.clientside.AbstractClientInterceptorsSetupTask;
import org.jboss.as.test.shared.integration.interceptor.clientside.InterceptorModule;
import org.jboss.as.test.shared.util.ClientInterceptorUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(ClientTransactionExceptionTestCase.SetupTask.class)
public class ClientTransactionExceptionTestCase {

    private static final String ARCHIVE_NAME_CLIENT = "transaction-test-client";
    private static final String ARCHIVE_NAME_SERVER = "transaction-test-server";

    private static final String MODULE_NAME = "interceptor-transaction";

    @Deployment(name = AbstractClientInterceptorsSetupTask.DEPLOYMENT_NAME_SERVER)
    @TargetsContainer(AbstractClientInterceptorsSetupTask.TARGER_CONTAINER_SERVER)
    public static Archive deployment0() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_SERVER + ".jar");
        jar.addClasses(FirstBean.class, FirstBeanRemote.class, SecondBean.class, SecondBeanRemote.class, MyXAResource.class);
        return jar;
    }

    @Deployment(name = AbstractClientInterceptorsSetupTask.DEPLOYMENT_NAME_CLIENT)
    @TargetsContainer(AbstractClientInterceptorsSetupTask.TARGER_CONTAINER_CLIENT)
    public static Archive<?> deployment1() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_CLIENT + ".jar");
        jar.addClasses(Util.class, ClientInterceptorUtil.class);
        jar.addClasses(FirstBean.class, FirstBeanRemote.class, SecondBean.class, SecondBeanRemote.class, MyXAResource.class);
        jar.addClasses(ClientTransactionExceptionTestCase.class, TransactionInterceptor.class);
        jar.addPackage(AbstractClientInterceptorsSetupTask.class.getPackage());
        jar.addAsManifestResource("META-INF/jboss-ejb-client-receivers.xml", "jboss-ejb-client.xml");
        jar.addAsManifestResource(
                createPermissionsXmlAsset(
                        new SecurityPermission("putProviderProperty.WildFlyElytron"),createFilePermission("read,write",
                                "jbossas.multinode.client", Arrays.asList("standalone", "data", "ejb-xa-recovery")),
                        createFilePermission("read,write",
                                "jbossas.multinode.client", Arrays.asList("standalone", "data", "ejb-xa-recovery", "-"))),

                "permissions.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void test() throws NamingException, RuntimeException {
        FirstBeanRemote bean = ClientInterceptorUtil.lookupStatelessRemote(ARCHIVE_NAME_SERVER, FirstBean.class, FirstBeanRemote.class);
        Assert.assertNotNull(bean);

        try {
            bean.callSecondBeanMethod("secondBeanEcho");
            Assert.fail("Interceptor was supposed to throw RuntimeException");
        } catch (Exception expected) {
            // expected
        }

        Assert.assertTrue(bean.areResourcesRollbacked());
    }

    static class SetupTask extends AbstractClientInterceptorsSetupTask.SetupTask {
        @Override
        public List<InterceptorModule> getModules() {
            return Collections.singletonList(new InterceptorModule(
                    TransactionInterceptor.class,
                    MODULE_NAME,
                    "module.xml",
                    ClientTransactionExceptionTestCase.class.getResource("module.xml"),
                    "client-side-interceptor.jar"
            ));
        }
    }
}

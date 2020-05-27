package org.jboss.as.test.manualmode.ejb.client.outbound.connection.ejboverhttp.byteman;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createFilePermission;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import java.net.InetAddress;
import java.rmi.NoSuchObjectException;
import java.util.Arrays;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.extension.byteman.api.BMRule;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

@RunWith(Arquillian.class)
@RunAsClient
public class EjbOverHTTPManualModeTestCase {

    private static final Logger logger = Logger.getLogger(EjbOverHTTPManualModeTestCase.class);

    public static final String ARCHIVE_NAME_SERVER = "ejboverhttp-test-server";
    public static final String ARCHIVE_NAME_CLIENT = "ejboverhttp-test-client";

    private static final String DEFAULT_AS_DEPLOYMENT = "server-deployment";
    private static final String DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML = "client-deployment";

    private static final String CLIENT_CONTAINER = "inbound-server";
    private static final String SERVER_CONTAINER = "outbound-server";

    private ModelControllerClient client;

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEFAULT_AS_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(SERVER_CONTAINER)
    public static Archive<?> createContainer1Deployment() {
        final JavaArchive ejbJar = createJar(ARCHIVE_NAME_SERVER);
        return ejbJar;
    }

    @Deployment(name = DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML, managed = false, testable = false)
    @TargetsContainer(CLIENT_CONTAINER)
    public static Archive<?> createContainer2Deployment() {
        JavaArchive jar = createJar(ARCHIVE_NAME_CLIENT);
        jar.addClasses(EjbOverHTTPManualModeTestCase.class);

        jar.addAsManifestResource(EjbOverHTTPManualModeTestCase.class.getPackage(), "jboss-ejb-client-profile.xml", "jboss-ejb-client.xml")
                .addAsManifestResource(EjbOverHTTPManualModeTestCase.class.getPackage(), "ejb-http-wildfly-config.xml", "wildfly-config.xml")
                .addAsManifestResource(createPermissionsXmlAsset(createFilePermission("read,write", "basedir"
                        , Arrays.asList("target", CLIENT_CONTAINER, "standalone", "data", "ejb-xa-recovery")),
                        createFilePermission("read,write", "basedir"
                                , Arrays.asList("target", CLIENT_CONTAINER, "standalone", "data", "ejb-xa-recovery", "-"))
                ), "permissions.xml");
        return jar;
    }


    private static JavaArchive createJar(String archiveName) {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, archiveName + ".jar");
        jar.addClasses(StatelessBean.class, StatelessLocal.class, StatelessRemote.class);
        return jar;
    }

    @Before
    public void setup() throws Exception {
        this.container.start(CLIENT_CONTAINER);

        client = ModelControllerClient.Factory.create(
                InetAddress.getByName(TestSuiteEnvironment.getServerAddress(CLIENT_CONTAINER)),
                9990
        );

        final ModelNode address = new ModelNode();
        address.add("subsystem", "ejb3");
        address.add("remoting-profile", "test-profile");
        address.protect();

        final ModelNode op1 = new ModelNode();
        op1.get(OP).set("add");
        op1.get(OP_ADDR).add(SUBSYSTEM, "ejb3");
        op1.get(OP_ADDR).add("remoting-profile", "test-profile");
        op1.get(OP_ADDR).set(address);

        op1.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

        client.execute(new OperationBuilder(op1).build());

        ModelNode op2 = new ModelNode();
        op2.get(OP).set(ADD);
        op2.get(OP_ADDR).add(SUBSYSTEM, "ejb3");
        op2.get(OP_ADDR).add("remoting-profile", "test-profile");
        op2.get(OP_ADDR).add("remote-http-connection", "test-connection");

        op2.get("uri").set("http://localhost:8180/wildfly-services");

        op2.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

        client.execute(new OperationBuilder(op2).build());

        this.deployer.deploy(DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML);

        this.container.start(SERVER_CONTAINER);

        this.deployer.deploy(DEFAULT_AS_DEPLOYMENT);
    }

    @After
    public void teardown() throws Exception {
        try {
            try {
                this.deployer.undeploy(DEFAULT_AS_DEPLOYMENT);
            } catch (Exception e) {
                // ignore
            }

            try {
                this.deployer.undeploy(DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML);
            } catch (Exception e) {
                // ignore
            }

            ModelNode op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "ejb3");
            op.get(OP_ADDR).add("remoting-profile", "test-profile");
            client.execute(new OperationBuilder(op).build());

            this.container.stop(SERVER_CONTAINER);
            this.container.stop(CLIENT_CONTAINER);
        } catch (Exception e) {
            logger.debug("Exception during container shutdown", e);
        }
    }

    @Test(expected = RuntimeException.class)
    @BMRule(
            name = "Throw exception on success", targetClass = "HttpEJBDiscoveryProvider", targetMethod = "discover",
            action = "throw new java.lang.RuntimeException()")
    @OperateOnDeployment(DEPLOYMENT_WITH_JBOSS_EJB_CLIENT_XML)
    public void testProcessMissingTarget() throws Exception {

        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());

        InitialContext jndiContext = new InitialContext(props);
        StatelessRemote bean = (StatelessRemote) jndiContext.lookup("ejb:/" + ARCHIVE_NAME_CLIENT
                + "//" + StatelessBean.class.getSimpleName() + "!" + StatelessRemote.class.getName());

        Assert.assertNotNull(bean);
        int methodCount = bean.remoteCall();
        Assert.assertEquals(1, methodCount);

        this.deployer.undeploy(DEFAULT_AS_DEPLOYMENT);

        try {
            int result = bean.remoteCall();
        } catch (NoSuchObjectException e) {
            //expected
        }
    }
}

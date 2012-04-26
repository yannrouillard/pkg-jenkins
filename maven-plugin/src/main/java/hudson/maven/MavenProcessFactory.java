/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.maven;

import hudson.FilePath;
import hudson.Launcher;
import hudson.EnvVars;
import hudson.maven.agent.Main;
import hudson.maven.agent.Maven21Interceptor;
import hudson.model.BuildListener;
import hudson.model.Run.RunnerAbortedException;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Which;
import hudson.tasks.Maven.MavenInstallation;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;


/**
 * Launches the maven process.
 *
 * @author Kohsuke Kawaguchi
 */
final class MavenProcessFactory extends AbstractMavenProcessFactory implements ProcessCache.Factory {


    MavenProcessFactory(MavenModuleSet mms, Launcher launcher, EnvVars envVars, String mavenOpts, FilePath workDir) {
        super( mms, launcher, envVars, mavenOpts, workDir );
    }

    @Override
    protected String getMavenAgentClassPath(MavenInstallation mvn,boolean isMaster,FilePath slaveRoot,BuildListener listener) throws IOException, InterruptedException {
        String classWorldsJar = getLauncher().getChannel().call(new GetClassWorldsJar(mvn.getHome(),listener));
        String classPath =
            ( isMaster ? Which.jarFile( Main.class ).getAbsolutePath()
                            : slaveRoot.child( "maven-agent.jar" ).getRemote() )
                + ( getLauncher().isUnix() ? ":" : ";" )
                + ( isMaster ? classWorldsJar : slaveRoot.child( "classworlds.jar" ).getRemote() );
        return classPath;
    }
    
    protected String getMainClassName() {
        return Main.class.getName();
    }

    @Override
    protected String getMavenInterceptorClassPath(MavenInstallation mvn,boolean isMaster,FilePath slaveRoot) throws IOException, InterruptedException {
        return isMaster?
            Which.jarFile(hudson.maven.agent.AbortException.class).getAbsolutePath():
            slaveRoot.child("maven-interceptor.jar").getRemote();
    }
    
    @Override
    protected String getMavenInterceptorOverride(MavenInstallation mvn,
            boolean isMaster, FilePath slaveRoot) throws IOException,
            InterruptedException {
        if(mvn.isMaven2_1(getLauncher())) {
            return isMaster?
                Which.jarFile(Maven21Interceptor.class).getAbsolutePath():
                slaveRoot.child("maven2.1-interceptor.jar").getRemote();
        }
        return null;
    }

    /**
     * Finds classworlds.jar
     */
    private static final class GetClassWorldsJar implements Callable<String,IOException> {
        private static final long serialVersionUID = 5812919424079344101L;
        private final String mvnHome;
        private final TaskListener listener;

        private GetClassWorldsJar(String mvnHome, TaskListener listener) {
            this.mvnHome = mvnHome;
            this.listener = listener;
        }

        public String call() throws IOException {
            File home = new File(mvnHome);
            File bootDir = new File(home, "core/boot");
            File[] classworlds = bootDir.listFiles(CLASSWORLDS_FILTER);
            if(classworlds==null || classworlds.length==0) {
                // Maven 2.0.6 puts it to a different place
                bootDir = new File(home, "boot");
                classworlds = bootDir.listFiles(CLASSWORLDS_FILTER);
                if(classworlds==null || classworlds.length==0) {
                    listener.error(Messages.MavenProcessFactory_ClassWorldsNotFound(home));
                    throw new RunnerAbortedException();
                }
            }
            return classworlds[0].getAbsolutePath();
        }
    }
    
    /**
     * Locates classworlds jar file.
     *
     * Note that Maven 3.0 changed the name to plexus-classworlds 
     *
     * <pre>
     * $ find tools/ -name "*classworlds*.jar"
     * tools/maven/boot/classworlds-1.1.jar
     * tools/maven-2.2.1/boot/classworlds-1.1.jar
     * tools/maven-3.0-alpha-2/boot/plexus-classworlds-1.3.jar
     * tools/maven-3.0-alpha-3/boot/plexus-classworlds-2.2.2.jar
     * tools/maven-3.0-alpha-4/boot/plexus-classworlds-2.2.2.jar
     * tools/maven-3.0-alpha-5/boot/plexus-classworlds-2.2.2.jar
     * tools/maven-3.0-alpha-6/boot/plexus-classworlds-2.2.2.jar
     * </pre>
     */
    private static final FilenameFilter CLASSWORLDS_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.contains("classworlds") && name.endsWith(".jar");
        }
    };
    
    //-------------------------------------------------
    // Some of those fields are used for maven 3 too
    //-------------------------------------------------

    /**
     * Set true to produce debug output.
     */
    public static boolean debug = false;

    public static boolean profile = Boolean.getBoolean("hudson.maven.profile");
    
    public static int socketTimeOut = Integer.parseInt( System.getProperty( "hudson.maven.socketTimeOut", Integer.toString( 30*1000 ) ) );
}

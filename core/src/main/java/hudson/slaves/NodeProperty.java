/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Tom Huybrechts
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
package hudson.slaves;

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor.FormException;
import hudson.model.ReconfigurableDescribable;
import hudson.model.queue.CauseOfBlockage;
import hudson.scm.SCM;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Environment;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Queue.Task;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.List;

/**
 * Extensible property of {@link Node}.
 *
 * <p>
 * Plugins can contribute this extension point to add additional data or UI actions to {@link Node}.
 * {@link NodeProperty}s show up in the configuration screen of a node, and they are persisted with the {@link Node} object.
 *
 *
 * <h2>Views</h2>
 * <dl>
 * <dt>config.jelly</dt>
 * <dd>Added to the configuration page of the node.
 * <dt>global.jelly</dt>
 * <dd>Added to the system configuration page.
 * <dt>summary.jelly (optional)</dt>
 * <dd>Added to the index page of the {@link hudson.model.Computer} associated with the node
 * </dl>
 *
 * @param <N>
 *      {@link NodeProperty} can choose to only work with a certain subtype of {@link Node}, and this 'N'
 *      represents that type. Also see {@link NodePropertyDescriptor#isApplicable(Class)}.
 *
 * @since 1.286
 */
public abstract class NodeProperty<N extends Node> implements ReconfigurableDescribable<NodeProperty<?>>, ExtensionPoint {

    protected transient N node;

    protected void setNode(N node) { this.node = node; }

    public NodePropertyDescriptor getDescriptor() {
        return (NodePropertyDescriptor)Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Called by the {@link Node} to help determine whether or not it should
     * take the given task. Individual properties can return a non-null value
     * here if there is some reason the given task should not be run on its
     * associated node. By default, this method returns <code>null</code>.
     *
     * @since 1.360
     */
    public CauseOfBlockage canTake(Task task) {
        return null;
    }

    /**
     * Runs before the {@link SCM#checkout(AbstractBuild, Launcher, FilePath, BuildListener, File)} runs, and performs a set up.
     * Can contribute additional properties to the environment.
     * 
     * @param build
     *      The build in progress for which an {@link Environment} object is created.
     *      Never null.
     * @param launcher
     *      This launcher can be used to launch processes for this build.
     *      If the build runs remotely, launcher will also run a job on that remote machine.
     *      Never null.
     * @param listener
     *      Can be used to send any message.
     * @return
     *      non-null if the build can continue, null if there was an error
     *      and the build needs to be aborted.
     * @throws IOException
     *      terminates the build abnormally. Hudson will handle the exception
     *      and reports a nice error message.
     */
    public Environment setUp( AbstractBuild build, Launcher launcher, BuildListener listener ) throws IOException, InterruptedException {
    	return new Environment() {};
    }

    public NodeProperty<?> reconfigure(StaplerRequest req, JSONObject form) throws FormException {
        return form==null ? null : getDescriptor().newInstance(req, form);
    }

    /**
     * Lists up all the registered {@link NodeDescriptor}s in the system.
     */
    public static DescriptorExtensionList<NodeProperty<?>,NodePropertyDescriptor> all() {
        return (DescriptorExtensionList)Hudson.getInstance().getDescriptorList(NodeProperty.class);
    }

    /**
     * List up all {@link NodePropertyDescriptor}s that are applicable for the
     * given project.
     */
    public static List<NodePropertyDescriptor> for_(Node node) {
        return NodePropertyDescriptor.for_(all(),node);
    }
}

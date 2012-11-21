package hudson.matrix;

import hudson.AbortException;
import hudson.Extension;
import hudson.console.HyperlinkNote;
import hudson.matrix.listeners.MatrixBuildListener;
import hudson.model.BuildListener;
import hudson.model.Cause.UpstreamCause;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.ResourceController;
import hudson.model.Result;
import hudson.model.Run;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

/**
 * {@link MatrixExecutionStrategy} that captures historical behavior.
 *
 * <p>
 * This class is somewhat complex because historically this wasn't an extension point and so
 * people tried to put various logics that cover different use cases into one place.
 * Going forward, people are encouraged to create subtypes to implement a custom logic that suits their needs.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.456
 */
public class DefaultMatrixExecutionStrategyImpl extends MatrixExecutionStrategy {
    private volatile boolean runSequentially;

    /**
     * Filter to select a number of combinations to build first
     */
    private volatile String touchStoneCombinationFilter;

    /**
     * Required result on the touchstone combinations, in order to
     * continue with the rest
     */
    private volatile Result touchStoneResultCondition;

    private volatile MatrixConfigurationSorter sorter;

    @DataBoundConstructor
    public DefaultMatrixExecutionStrategyImpl(Boolean runSequentially, boolean hasTouchStoneCombinationFilter, String touchStoneCombinationFilter, Result touchStoneResultCondition, MatrixConfigurationSorter sorter) {
        this(runSequentially!=null ? runSequentially : false,
            hasTouchStoneCombinationFilter ? touchStoneCombinationFilter : null,
            hasTouchStoneCombinationFilter ? touchStoneResultCondition : null,
            sorter);
    }

    public DefaultMatrixExecutionStrategyImpl(boolean runSequentially, String touchStoneCombinationFilter, Result touchStoneResultCondition, MatrixConfigurationSorter sorter) {
        this.runSequentially = runSequentially;
        this.touchStoneCombinationFilter = touchStoneCombinationFilter;
        this.touchStoneResultCondition = touchStoneResultCondition;
        this.sorter = sorter;
    }

    public DefaultMatrixExecutionStrategyImpl() {
        this(false,false,null,null,null);
    }

    public boolean getHasTouchStoneCombinationFilter() {
        return touchStoneCombinationFilter!=null;
    }

    /**
     * If true, {@link MatrixRun}s are run sequentially, instead of running in parallel.
     *
     * TODO: this should be subsumed by {@link ResourceController}.
     */
    public boolean isRunSequentially() {
        return runSequentially;
    }

    public void setRunSequentially(boolean runSequentially) {
        this.runSequentially = runSequentially;
    }

    public String getTouchStoneCombinationFilter() {
        return touchStoneCombinationFilter;
    }

    public void setTouchStoneCombinationFilter(String touchStoneCombinationFilter) {
        this.touchStoneCombinationFilter = touchStoneCombinationFilter;
    }

    public Result getTouchStoneResultCondition() {
        return touchStoneResultCondition;
    }

    public void setTouchStoneResultCondition(Result touchStoneResultCondition) {
        this.touchStoneResultCondition = touchStoneResultCondition;
    }

    public MatrixConfigurationSorter getSorter() {
        return sorter;
    }

    public void setSorter(MatrixConfigurationSorter sorter) {
        this.sorter = sorter;
    }

    public Result run(MatrixBuild build, List<MatrixAggregator> aggregators, BuildListener listener) throws InterruptedException, IOException {
        MatrixProject p = build.getProject();
        PrintStream logger = listener.getLogger();

        Collection<MatrixConfiguration> touchStoneConfigurations = new HashSet<MatrixConfiguration>();
        Collection<MatrixConfiguration> delayedConfigurations = new HashSet<MatrixConfiguration>();
        for (MatrixConfiguration c: p.getActiveConfigurations()) {
            if (!MatrixBuildListener.buildConfiguration(build, c))
                continue; // skip rebuild
            if (touchStoneCombinationFilter != null && c.getCombination().evalGroovyExpression(p.getAxes(), p.getTouchStoneCombinationFilter())) {
                touchStoneConfigurations.add(c);
            } else {
                delayedConfigurations.add(c);
            }
        }

        if (notifyStartBuild(aggregators)) return Result.FAILURE;

        if (sorter != null) {
            touchStoneConfigurations = createTreeSet(touchStoneConfigurations, sorter);
            delayedConfigurations    = createTreeSet(delayedConfigurations,sorter);
        }

        if(!runSequentially)
            for(MatrixConfiguration c : touchStoneConfigurations)
                scheduleConfigurationBuild(build, listener, c);

        Result r = Result.SUCCESS;
        for (MatrixConfiguration c : touchStoneConfigurations) {
            if(runSequentially)
                scheduleConfigurationBuild(build, listener, c);
            MatrixRun run = waitForCompletion(build, listener, c);
            notifyEndBuild(run,aggregators);
            r = r.combine(getResult(run));
        }
        
        if (touchStoneResultCondition != null && r.isWorseThan(touchStoneResultCondition)) {
            logger.printf("Touchstone configurations resulted in %s, so aborting...%n", r);
            return r;
        }
        
        if(!p.isRunSequentially())
            for(MatrixConfiguration c : delayedConfigurations)
                scheduleConfigurationBuild(build, listener, c);

        for (MatrixConfiguration c : delayedConfigurations) {
            if(p.isRunSequentially())
                scheduleConfigurationBuild(build, listener, c);
            MatrixRun run = waitForCompletion(build, listener, c);
            notifyEndBuild(run,aggregators);
            logger.println(Messages.MatrixBuild_Completed(HyperlinkNote.encodeTo('/' + c.getUrl(), c.getDisplayName()), getResult(run)));
            r = r.combine(getResult(run));
        }

        return r;
    }

    private Result getResult(@Nullable MatrixRun run) {
        // null indicates that the run was cancelled before it even gets going
        return run!=null ? run.getResult() : Result.ABORTED;
    }

    private boolean notifyStartBuild(List<MatrixAggregator> aggregators) throws InterruptedException, IOException {
        for (MatrixAggregator a : aggregators)
            if(!a.startBuild())
                return true;
        return false;
    }

    private void notifyEndBuild(MatrixRun b, List<MatrixAggregator> aggregators) throws InterruptedException, IOException {
        if (b==null)    return; // can happen if the configuration run gets cancelled before it gets started.
        for (MatrixAggregator a : aggregators)
            if(!a.endRun(b))
                throw new AbortException();
    }
    
    private <T> TreeSet<T> createTreeSet(Collection<T> items, Comparator<T> sorter) {
        TreeSet<T> r = new TreeSet<T>(sorter);
        r.addAll(items);
        return r;
    }

    private void scheduleConfigurationBuild(MatrixBuild build, BuildListener listener, MatrixConfiguration c) {
        listener.getLogger().println(Messages.MatrixBuild_Triggering(HyperlinkNote.encodeTo('/' + c.getUrl(), c.getDisplayName())));
        c.scheduleBuild(build.getAction(ParametersAction.class), new UpstreamCause((Run)build));
    }

    private MatrixRun waitForCompletion(MatrixBuild build, BuildListener listener, MatrixConfiguration c) throws InterruptedException, IOException {
        String whyInQueue = "";
        long startTime = System.currentTimeMillis();

        // wait for the completion
        int appearsCancelledCount = 0;
        while(true) {
            MatrixRun b = c.getBuildByNumber(build.getNumber());

            // two ways to get beyond this. one is that the build starts and gets done,
            // or the build gets cancelled before it even started.
            if(b!=null && !b.isBuilding()) {
                Result buildResult = b.getResult();
                if(buildResult!=null)
                    return b;
            }
            Queue.Item qi = c.getQueueItem();
            if(b==null && qi==null)
                appearsCancelledCount++;
            else
                appearsCancelledCount = 0;

            if(appearsCancelledCount>=5) {
                // there's conceivably a race condition in computating b and qi, as their computation
                // are not synchronized. There are indeed several reports of Hudson incorrectly assuming
                // builds being cancelled. See
                // http://www.nabble.com/Master-slave-problem-tt14710987.html and also
                // http://www.nabble.com/Anyone-using-AccuRev-plugin--tt21634577.html#a21671389
                // because of this, we really make sure that the build is cancelled by doing this 5
                // times over 5 seconds
                listener.getLogger().println(Messages.MatrixBuild_AppearsCancelled(HyperlinkNote.encodeTo('/'+ c.getUrl(),c.getDisplayName())));
                return null;
            }

            if(qi!=null) {
                // if the build seems to be stuck in the queue, display why
                String why = qi.getWhy();
                if(!why.equals(whyInQueue) && System.currentTimeMillis()-startTime>5000) {
                    listener.getLogger().println(HyperlinkNote.encodeTo('/'+ c.getUrl(),c.getDisplayName())+" is still in the queue: "+why);
                    whyInQueue = why;
                }
            }
            
            Thread.sleep(1000);
        }
    }

    @Extension
    public static class DescriptorImpl extends MatrixExecutionStrategyDescriptor {
        @Override
        public String getDisplayName() {
            return "Classic";
        }
    }
}

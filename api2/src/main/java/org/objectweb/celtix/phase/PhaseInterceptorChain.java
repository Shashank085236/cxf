package org.objectweb.celtix.phase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.celtix.interceptors.Interceptor;
import org.objectweb.celtix.interceptors.InterceptorChain;
import org.objectweb.celtix.message.Message;

/**
 * A PhaseInterceptorChain orders Interceptors according to the phase the
 * particpate in and also according to the before & after properties on an
 * Interceptor.
 * <p>
 * A List of phases is supplied to the PhaseInterceptorChain in the constructor.
 * Interceptors that are added to the chain are ordered by phase. Within that
 * phases interceptors can order themselves. Each PhaseInterceptor has an ID.
 * PhaseInterceptors can supply a Collection of IDs which they should run before
 * or after, supplying fine grained ordering.
 * <p>
 *  
 * @author Dan Diephouse
 */
public class PhaseInterceptorChain implements InterceptorChain {

    private static final Logger LOG = Logger.getLogger(PhaseInterceptorChain.class.getName());
    private Map<String, List<Interceptor>> interceptors = new LinkedHashMap<String, List<Interceptor>>();
    private List<Phase> phases;
    private Phase currentPhase;
    private List<Interceptor> currentPhaseInterceptors;
    private Interceptor currentInterceptor;
    private State state;

    public PhaseInterceptorChain(List<Phase> ps) {

        state = State.PAUSED;

        // Order the phases correctly based on priority
        Collections.sort(ps, new PhaseComparator());
        this.phases = ps;

        for (Phase phase : ps) {
            interceptors.put(phase.getName(), new ArrayList<Interceptor>());
        }

        currentPhase = ps.get(0);
        currentPhaseInterceptors = interceptors.get(currentPhase.getName());
    }

    public void add(List<Interceptor> newhandlers) {
        if (newhandlers == null) {
            return;
        }

        for (Interceptor handler : newhandlers) {
            add(handler);
        }
    }

    @SuppressWarnings("unchecked")
    public void add(Interceptor i) {
        AbstractPhaseInterceptor pi = (AbstractPhaseInterceptor)i;

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Adding interceptor " + i + " to phase " + pi.getPhase());
        }

        String phaseName = pi.getPhase();
        
        List<Interceptor> phase = interceptors.get(phaseName);
        
        if (phase == null) {
            LOG.fine("Phase " + phaseName + " does not exist. Skipping handler "
                      + i.getClass().getName());
        } else {
            insertInterceptor(phase, pi);
        }
    }

    public void pause() {
        state = State.PAUSED;
    }

    public void resume() {
        // TODO Auto-generated method stub

    }
    

    /**
     * Invokes each phase's handler in turn.
     * 
     * @param context
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public boolean doIntercept(Message message) {
        state = State.EXECUTING;
        do {
            if (currentInterceptor == null && currentPhaseInterceptors.size() > 0) {
                currentInterceptor = currentPhaseInterceptors.get(0);                
            } else {
                int index = currentPhaseInterceptors.indexOf(currentInterceptor);
                if (index == currentPhaseInterceptors.size() - 1) {
                    // we're at the end of this phase, go to the next one
                    int phaseIndex = phases.indexOf(currentPhase);

                    if (setupPhase(++phaseIndex)) {
                        currentInterceptor = null;
                    }
                } else {
                    // Find the current position of this interceptor as it could
                    // have changed
                    currentInterceptor = currentPhaseInterceptors.get(index + 1);
                }
            }

            if (null != currentInterceptor) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Invoking handleMessage on interceptor " + currentInterceptor);
                }
                try {
                    currentInterceptor.handleMessage(message);
                } catch (Exception ex) {
                    if (LOG.isLoggable(Level.FINE)) {
                        ex.printStackTrace();
                        LOG.fine("Interceptor has thrown exception, unwinding now");
                    }
                    message.setContent(Exception.class, ex);
                    unwind(message);
                    state = State.ABORTED;
                }                
            } else {
                state = State.COMPLETE;
            }
            

        } while (null != currentInterceptor && state != State.PAUSED);        
        
        return state == State.COMPLETE;
    }
    
    @SuppressWarnings("unchecked")
    private void unwind(Message message) {
        
        while (null != currentInterceptor) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Invoking handleFault on interceptor " + currentInterceptor);
            }
            currentInterceptor.handleFault(message);
            
            int index = currentPhaseInterceptors.indexOf(currentInterceptor);
            if (index == 0) {
                // we're at the begin of this phase, go to the previous one
                int phaseIndex = phases.indexOf(currentPhase);

                if (setupPhase(--phaseIndex, true)) {
                    currentInterceptor = null;
                }
            } else {
                // Find the current position of this interceptor as it could
                // have changed
                currentInterceptor = currentPhaseInterceptors.get(index - 1);
            }
        } 
    }

    private boolean setupPhase(int phaseIndex) {
        return setupPhase(phaseIndex, false);
    }
    
    private boolean setupPhase(int phaseIndex, boolean reverse) {
        // Have we reached the end of the chain??
        if ((!reverse && phaseIndex == phases.size()) 
            || (reverse && phaseIndex == -1)) {
            return true;
        }

        currentPhase = phases.get(phaseIndex);
        currentPhaseInterceptors = interceptors.get(currentPhase.getName());
        if (currentPhaseInterceptors.size() == 0) {
            return setupPhase(reverse ? --phaseIndex : ++phaseIndex);
        }

        currentInterceptor = currentPhaseInterceptors.get(
            reverse ? currentPhaseInterceptors.size() - 1 : 0);
        return false;
    }

    public void remove(Interceptor i) {
        // TODO
    }

    public ListIterator<Interceptor> getIterator() {
        List<Interceptor> allInterceptors = new ArrayList<Interceptor>();
        for (List<Interceptor> interceptorList : interceptors.values()) {
            allInterceptors.addAll(interceptorList);
        }
        return Collections.unmodifiableList(allInterceptors).listIterator();
    }
    

    protected void insertInterceptor(List<Interceptor> intercs, AbstractPhaseInterceptor interc) {
        if (intercs.size() == 0) {
            intercs.add(interc);
            return;
        }

        int begin = -1;
        int end = intercs.size();

        Collection before = interc.getBefore();
        Collection after = interc.getAfter();

        for (int i = 0; i < intercs.size(); i++) {
            PhaseInterceptor cmp = (PhaseInterceptor)intercs.get(i);

            if (cmp.getId() == null) {
                continue;
            }

            if (before.contains(cmp.getId()) && i < end) {
                end = i;
            }

            if (cmp.getBefore().contains(interc.getId()) && i > begin) {
                begin = i;
            }

            if (after.contains(cmp.getId()) && i > begin) {
                begin = i;
            }

            if (cmp.getAfter().contains(interc.getId()) && i < end) {
                end = i;
            }
        }

        if (end < begin + 1) {
            throw new IllegalStateException("Invalid ordering for handler " + interc.getClass().getName());
        }

        intercs.add(begin + 1, interc);
    }

    public Iterator<Interceptor<? extends Message>> iterator() {
        List<Interceptor<? extends Message>> allInterceptors 
            = new ArrayList<Interceptor<? extends Message>>();
        for (List<Interceptor> interceptorList : interceptors.values()) {
            for (Interceptor i : interceptorList) {
                allInterceptors.add((Interceptor<? extends Message>)i);
            }
        }
        return allInterceptors.iterator();
    }
}

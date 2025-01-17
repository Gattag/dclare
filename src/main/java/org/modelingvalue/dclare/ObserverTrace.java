//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2023 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus, Ronald Krijgsheld                                                                           ~
// Contributors:                                                                                                       ~
//     Arjan Kok, Carel Bast                                                                                           ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.dclare;

import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.TriConsumer;

@SuppressWarnings("unused")
public class ObserverTrace implements Comparable<ObserverTrace> {

    private final Mutable                                   mutable;
    private final Observer<?>                               observer;
    private final int                                       nrOfChanges;
    private final ObserverTrace                             previous;
    private final Map<ObservedInstance, Object>             read;
    private final Map<ObservedInstance, Object>             written;
    private final Set<ObserverTrace>                        done;
    private final Map<ObservedInstance, Set<ObserverTrace>> backTrace;
    private final Instant                                   time;

    protected ObserverTrace(Mutable mutable, Observer<?> observer, ObserverTrace previous, int nrOfChanges, Map<ObservedInstance, Object> read, Map<ObservedInstance, Object> written) {
        this.mutable = mutable;
        this.observer = observer;
        this.nrOfChanges = nrOfChanges;
        this.previous = previous;
        this.read = read;
        this.written = written;
        for (Entry<ObservedInstance, Object> e : read) {
            e.getKey().observed().readers().set(e.getKey().mutable(), Set::add, this);
        }
        for (Entry<ObservedInstance, Object> e : written) {
            e.getKey().observed().writers().set(e.getKey().mutable(), Set::add, this);
        }
        Set<ObserverTrace> done = previous != null ? previous.done : Set.of();
        Map<ObservedInstance, Set<ObserverTrace>> backTrace = read.toMap(e -> {
            ObservedInstance observedInstance = e.getKey();
            Set<ObserverTrace> writers = observedInstance.observed().writers().get(observedInstance.mutable());
            return Entry.of(observedInstance, writers.removeAll(done).remove(this));
        });
        Set<ObserverTrace> back = backTrace.flatMap(Entry::getValue).toSet();
        Set<ObserverTrace> backDone = back.flatMap(ObserverTrace::done).toSet();
        backTrace = backTrace.toMap(e -> Entry.of(e.getKey(), e.getValue().removeAll(backDone)));
        if (backTrace.anyMatch(e -> e.getValue().anyMatch(w -> !w.mutable.equals(mutable) || !w.observer.equals(observer)))) {
            backTrace = backTrace.toMap(e -> Entry.of(e.getKey(), e.getValue().filter(w -> !w.mutable.equals(mutable) || !w.observer.equals(observer)).toSet()));
        }
        this.backTrace = backTrace;
        this.done = done.addAll(back).addAll(backDone).addAll(previous != null ? previous.done.add(previous) : Set.of());
        this.time = Instant.now();
    }

    public Instant time() {
        return time;
    }

    public Mutable mutable() {
        return mutable;
    }

    public Observer<?> observer() {
        return observer;
    }

    public int nrOfChanges() {
        return nrOfChanges;
    }

    public Set<ObserverTrace> done() {
        return done;
    }

    public Map<ObservedInstance, Object> read() {
        return read;
    }

    public Map<ObservedInstance, Object> written() {
        return written;
    }

    public ObserverTrace previous() {
        return previous;
    }

    public Map<ObservedInstance, Set<ObserverTrace>> backTrace() {
        return backTrace;
    }

    @Override
    public String toString() {
        return mutable + "." + observer + "#" + nrOfChanges;
    }

    @Override
    public int compareTo(ObserverTrace o) {
        return time.compareTo(o.time);
    }

    @SuppressWarnings("unchecked")
    public String trace(String prefix, int length) {
        StringBuilder sb = new StringBuilder();
        trace(prefix, (c, r) -> sb.append(c).append("run  : ").append(r.mutable()).append(".").append(r.observer()).append(" nr: ").append(r.nrOfChanges), (c, r, s) -> sb.append(c).append("read : ").append(s.mutable()).append(".").append(s.observed()).append("=").append(r.read.get(s)), (c, w, s) -> sb.append(c).append("write: ").append(s.mutable()).append(".").append(s.observed()).append("=").append(w.written.get(s)), p -> p + "  ", new Set[]{Set.of()}, length);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public <C> void trace(C context, BiConsumer<C, ObserverTrace> runHandler, TriConsumer<C, ObserverTrace, ObservedInstance> readHandler, TriConsumer<C, ObserverTrace, ObservedInstance> writeHandler, Function<C, C> traceHandler, int length) {
        trace(context, runHandler, readHandler, writeHandler, traceHandler, new Set[]{Set.of()}, length);
    }

    private <C> void trace(C context, BiConsumer<C, ObserverTrace> runHandler, TriConsumer<C, ObserverTrace, ObservedInstance> readHandler, TriConsumer<C, ObserverTrace, ObservedInstance> writeHandler, Function<C, C> traceHandler, Set<ObserverTrace>[] done, int length) {
        runHandler.accept(context, this);
        if (done[0].size() < length && !done[0].contains(this)) {
            done[0] = done[0].add(this);
            for (Entry<ObservedInstance, Set<ObserverTrace>> e : backTrace()) {
                if (!e.getValue().isEmpty()) {
                    readHandler.accept(context, this, e.getKey());
                    for (ObserverTrace writer : e.getValue()) {
                        writeHandler.accept(context, writer, e.getKey());
                        writer.trace(traceHandler.apply(context), runHandler, readHandler, writeHandler, traceHandler, done, length);
                    }
                }
            }
        }
    }

}

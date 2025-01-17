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

import java.util.function.Supplier;

import org.modelingvalue.collections.DefaultMap;
import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.collections.util.QuadConsumer;
import org.modelingvalue.dclare.ex.ConsistencyError;
import org.modelingvalue.dclare.ex.EmptyMandatoryException;
import org.modelingvalue.dclare.ex.TooManyObserversException;

public class Observed<O, T> extends Setable<O, T> {

    @SuppressWarnings("rawtypes")
    protected static final DefaultMap<Observed, Set<Mutable>> OBSERVED_MAP = DefaultMap.of(k -> Set.of());

    public static <C, V> Observed<C, V> of(Object id, V def, SetableModifier... modifiers) {
        return new Observed<>(id, def, null, null, null, modifiers);
    }

    public static <C, V> Observed<C, V> of(Object id, V def, QuadConsumer<LeafTransaction, C, V, V> changed, SetableModifier... modifiers) {
        return new Observed<>(id, def, null, null, changed, modifiers);
    }

    public static <C, V> Observed<C, V> of(Object id, V def, Supplier<Setable<?, ?>> opposite, SetableModifier... modifiers) {
        return new Observed<>(id, def, opposite, null, null, modifiers);
    }

    public static <C, V> Observed<C, V> of(Object id, V def, QuadConsumer<LeafTransaction, C, V, V> changed, Supplier<Setable<C, Set<?>>> scope, SetableModifier... modifiers) {
        return new Observed<>(id, def, null, scope, changed, modifiers);
    }

    public static <C, V> Observed<C, V> of(Object id, V def, Supplier<Setable<?, ?>> opposite, Supplier<Setable<C, Set<?>>> scope, SetableModifier... modifiers) {
        return new Observed<>(id, def, opposite, scope, null, modifiers);
    }

    private final Setable<Object, Set<ObserverTrace>> readers      = Setable.of(Pair.of(this, "readers"), Set.of());
    private final Setable<Object, Set<ObserverTrace>> writers      = Setable.of(Pair.of(this, "writers"), Set.of());
    private final boolean                             mandatory;
    private final Observers<O, T>                     observers;
    @SuppressWarnings("rawtypes")
    private final Entry<Observed, Set<Mutable>>       thisInstance = Entry.of(this, Mutable.THIS_SINGLETON);

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Observed(Object id, T def, Supplier<Setable<?, ?>> opposite, Supplier<Setable<O, Set<?>>> scope, QuadConsumer<LeafTransaction, O, T, T> changed, SetableModifier... modifiers) {
        super(id, def, opposite, scope, changed, modifiers);
        this.mandatory = SetableModifier.mandatory.in(modifiers);
        this.observers = new Observers<>(this);
    }

    @SuppressWarnings("rawtypes")
    protected void checkTooManyObservers(UniverseTransaction utx, Object object, DefaultMap<Observer, Set<Mutable>> observers) {
        if (!isPlumbing() && utx.stats().maxNrOfObservers() < LeafTransaction.size(observers)) {
            throw new TooManyObserversException(object, this, observers, utx);
        }
    }

    @Override
    protected boolean isHandlingChange() {
        return true;
    }

    public Observers<O, T> observers() {
        return observers;
    }

    @Override
    public boolean mandatory() {
        return mandatory;
    }

    public Setable<Object, Set<ObserverTrace>> readers() {
        return readers;
    }

    public Setable<Object, Set<ObserverTrace>> writers() {
        return writers;
    }

    public int getNrOfObservers(O object) {
        LeafTransaction tx = currentLeaf(object);
        return tx.get(object, observers).size();
    }

    @SuppressWarnings("rawtypes")
    public static final class Observers<O, T> extends Setable<O, DefaultMap<Observer, Set<Mutable>>> {

        @SuppressWarnings("unchecked")
        private Observers(Observed observed) {
            super(observed, Observer.OBSERVER_MAP, null, null, (tx, o, b, a) -> {
                observed.checkTooManyObservers(tx.universeTransaction(), o, a);
            }, SetableModifier.plumbing);
        }

        @SuppressWarnings("unchecked")
        public Observed<O, T> observed() {
            return (Observed<O, T>) id();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + ":" + super.toString();
        }

    }

    @SuppressWarnings("rawtypes")
    protected Entry<Observed, Set<Mutable>> entry(Mutable object, Mutable self) {
        return object.equals(self) ? thisInstance : Entry.of(this, Set.of(object));
    }

    @Override
    public boolean checkConsistency() {
        return !isPlumbing() && (mandatory || super.checkConsistency());
    }

    @Override
    public Set<ConsistencyError> checkConsistency(State state, O object, T post) {
        Set<ConsistencyError> errors = super.checkConsistency() ? super.checkConsistency(state, object, post) : Set.of();
        if (!isPlumbing() && mandatory && isEmpty(post)) {
            errors = errors.add(new EmptyMandatoryException(object, this));
        }
        return errors;
    }

    @Override
    public boolean preserved() {
        return super.preserved() || !isPlumbing();
    }

}

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2022 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

public class MutableState implements IState {

    private final State                  previous;
    private final AtomicReference<State> atomic;

    public MutableState(State state) {
        this.atomic = new AtomicReference<>(state);
        this.previous = state;
    }

    @Override
    public State previous() {
        return previous;
    }

    @Override
    public <O, T> T get(O object, Getable<O, T> property) {
        return state().get(object, property);
    }

    public State state() {
        return atomic.get();
    }

    public State setState(State state) {
        return atomic.getAndUpdate(s -> state);
    }

    public <O, T> State set(O object, Setable<O, T> property, T value) {
        return atomic.updateAndGet(s -> {
            State r = s.set(object, property, value);
            if (r != s && object instanceof Mutable && !property.isPlumbing()) {
                r = setChanged(r, (Mutable) object);
            }
            return r;
        });
    }

    public <O, E, T> State set(O object, Setable<O, T> property, BiFunction<T, E, T> function, E element) {
        return atomic.updateAndGet(s -> {
            State r = s.set(object, property, function, element);
            if (r != s && object instanceof Mutable && !property.isPlumbing()) {
                r = setChanged(r, (Mutable) object);
            }
            return r;
        });
    }

    public <O, T> State set(O object, Setable<O, T> property, UnaryOperator<T> oper) {
        return atomic.updateAndGet(s -> {
            State r = s.set(object, property, oper);
            if (r != s && object instanceof Mutable && !property.isPlumbing()) {
                r = setChanged(r, (Mutable) object);
            }
            return r;
        });
    }

    public <O, T> State set(O object, Setable<O, T> property, T value, T[] oldNew) {
        return atomic.updateAndGet(s -> {
            State r = s.set(object, property, value, oldNew);
            if (r != s && object instanceof Mutable && !property.isPlumbing()) {
                r = setChanged(r, (Mutable) object);
            }
            return r;
        });
    }

    public <O, E, T> State set(O object, Setable<O, T> property, BiFunction<T, E, T> function, E element, T[] oldNew) {
        return atomic.updateAndGet(s -> {
            State r = s.set(object, property, function, element, oldNew);
            if (r != s && object instanceof Mutable && !property.isPlumbing()) {
                r = setChanged(r, (Mutable) object);
            }
            return r;
        });
    }

    public <O, T> State set(O object, Setable<O, T> property, UnaryOperator<T> oper, T[] oldNew) {
        return atomic.updateAndGet(s -> {
            State r = s.set(object, property, oper, oldNew);
            if (r != s && object instanceof Mutable && !property.isPlumbing()) {
                r = setChanged(r, (Mutable) object);
            }
            return r;
        });
    }

    private State setChanged(State state, Mutable changed) {
        TransactionId txid = state.get(state.universeTransaction().universe(), Mutable.D_CHANGE_ID);
        while (changed != null && !(changed instanceof Universe) && state.get(changed, Mutable.D_CHANGE_ID) != txid) {
            state = state.set(changed, Mutable.D_CHANGE_ID, txid);
            changed = state.getA(changed, Mutable.D_PARENT_CONTAINING);
        }
        return state;
    }

}
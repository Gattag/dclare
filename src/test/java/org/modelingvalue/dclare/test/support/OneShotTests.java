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

package org.modelingvalue.dclare.test.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.modelingvalue.collections.Collection;
import org.modelingvalue.dclare.Mutable;
import org.modelingvalue.dclare.MutableClass;
import org.modelingvalue.dclare.Observer;
import org.modelingvalue.dclare.OneShot;
import org.modelingvalue.dclare.Setable;
import org.modelingvalue.dclare.Universe;

public class OneShotTests {
    private static class MyMutableClass implements MutableClass {
        @Override
        public Collection<? extends Observer<?>> dObservers() {
            return null;
        }

        @Override
        public Collection<? extends Setable<? extends Mutable, ?>> dSetables() {
            return null;
        }
    }

    private static class MyUniverse implements Universe {
        @Override
        public MutableClass dClass() {
            return null;
        }
    }

    @Disabled
    @Test
    public void simple() {
        String              jsonIn  = "{}";
        OneShot<MyUniverse> oneShot = new OneShot<>(new MyUniverse(), jsonIn);
        String              jsonOut = oneShot.fromJsonToJson();

        Assertions.assertEquals("xxx", jsonOut);
    }
}

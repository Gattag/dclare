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

import java.util.Arrays;

/**
 * this is a marker interface
 */
@SuppressWarnings("rawtypes")
public interface FeatureModifier<M extends FeatureModifier> {

    @SuppressWarnings("unchecked")
    default M iff(boolean b) {
        return b ? (M) this : null;
    }

    @SuppressWarnings("unchecked")
    default M ifnot(boolean b) {
        return b ? null : (M) this;
    }

    default boolean in(M[] modifiers) {
        for (M m : modifiers) {
            if (this == m) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    static <C extends M, M extends FeatureModifier> C ofClass(Class<C> cls, M[] modifiers) {
        for (M m : modifiers) {
            if (cls.isInstance(m)) {
                return (C) m;
            }
        }
        return null;
    }

    @SafeVarargs
    static <M> M[] add(M[] modifiers, M... added) {
        modifiers = Arrays.copyOf(modifiers, modifiers.length + added.length);
        System.arraycopy(added, 0, modifiers, modifiers.length - added.length, added.length);
        return modifiers;
    }

}

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2019 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

import java.util.*;
import java.util.concurrent.atomic.*;

import org.modelingvalue.collections.util.*;
import org.modelingvalue.dclare.sync.*;

public class CommunicationPeer {
    static {
        System.setProperty("TRACE_LOG", "false");
        System.setProperty("PARALLELISM", "1");
        ModelMaker.weAreSlave = true;
        //        System.err.println("~~~FORCED TRACE_LOG   = " + System.getProperty("TRACE_LOG"));
        //        System.err.println("~~~FORCED PARALLELISM = " + System.getProperty("PARALLELISM"));
    }

    public static void main(String[] args) throws Throwable {
        ModelMaker       mmSlave        = new ModelMaker("mmSlave");
        TestDeltaAdaptor mmSlaveAdaptor = CommunicationHelper.hookupDeltaAdaptor(mmSlave);

        WorkDaemon<String> backFeeder = new WorkDaemon<>("backFeeder") {
            @Override
            protected String waitForWork() {
                return mmSlaveAdaptor.get();
            }

            @Override
            protected void execute(String delta) { // delta slave->master
                System.out.println("D" + delta);
            }
        };
        CommunicationHelper.add(backFeeder);
        backFeeder.start();

        AtomicBoolean stop = new AtomicBoolean();
        CommunicationHelper.interpreter(System.in, stop, Map.of(
                '.', (c, line) -> System.out.println("." + line),
                'D', (c, line) -> mmSlaveAdaptor.accept(line), // delta master->slave
                'C', (c, line) -> check(line, mmSlave.getXyzzy_source(), mmSlave.getXyzzy_target(), mmSlave.getXyzzy_aList().size(), mmSlave.getXyzzy_aSet().size() / 2),
                'Q', (c, line) -> stop.set(true),
                '*', (c, line) -> exit(10, "ERROR: unknown command " + c + line)
        ));

        CommunicationHelper.tearDownAll();
        TraceTimer.dumpLogs();
        System.err.println("stopping...");
    }

    private static void check(String expectedInt, int... values) {
        int expected = Integer.parseInt(expectedInt);
        for (int value : values) {
            System.err.println("CHECK: " + value + " (expecting " + expected + ")");
            if (value != expected) {
                exit(1, "CHECK FAILED");
            }
        }
    }

    private static void exit(int status, String msg) {
        System.err.println(msg + "; immediate exit(" + status + ")");
        System.exit(status);
    }
}
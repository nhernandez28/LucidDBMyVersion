/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2006-2006 LucidEra, Inc.
// Copyright (C) 2006-2006 The Eigenbase Project
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your option)
// any later version approved by The Eigenbase Project.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package com.lucidera.farrago;

import net.sf.farrago.session.*;
import net.sf.farrago.db.*;

import org.eigenbase.relopt.*;

import java.util.*;
import java.util.logging.*;

import org.apache.commons.transaction.locking.*;
import org.apache.commons.transaction.util.*;

/**
 * LucidDbTxnMgr implements the {@link FarragoSessionTxnMgr} interface
 * with locking semantics customized for LucidDB.
 *
 * @author John V. Sichi
 * @version $Id$
 */
class LucidDbTxnMgr extends FarragoDbNullTxnMgr
{
    private final LockManager2 lockMgr;

    private final Integer dbWriteLock;
    
    LucidDbTxnMgr()
    {
        // TODO jvs 15-Mar-2006:  start a new LucidDbTrace.java file?
        LoggerFacade loggerFacade = new Jdk14Logger(
            Logger.getLogger(LucidDbTxnMgr.class.getName()));
        lockMgr = new GenericLockManager(2, loggerFacade);

        // This represents a lock which can be acquired on the
        // entire database.  It prevents concurrent writes,
        // but does not prevent reads; for that we use table-level
        // locking.
        dbWriteLock = new Integer(1);
    }
    
    // implement FarragoSessionTxnMgr
    public FarragoSessionTxnId beginTxn(FarragoSession session)
    {
        return super.beginTxn(session);
    }

    // override FarragoDbNullTxnMgr
    protected void accessTable(
        FarragoSessionTxnId txnId,
        List<String> localTableName,
        TableAccessMap.Mode accessType)
    {
        super.accessTable(txnId, localTableName, accessType);
        
        if (accessType == TableAccessMap.Mode.READ_ACCESS) {
            // S-lock only the table; readers don't care about
            // the database lock
            lockMgr.lock(txnId, localTableName, 1, true);
        } else {
            // X-lock the database to exclude other writers but
            // not readers
            lockMgr.lock(txnId, dbWriteLock, 2, true);
            // X-lock the table to exclude readers
            lockMgr.lock(txnId, localTableName, 2, true);
        }
    }

    // implement FarragoSessionTxnMgr
    public void endTxn(
        FarragoSessionTxnId txnId,
        FarragoSessionTxnEnd endType)
    {
        super.endTxn(txnId, endType);
        lockMgr.releaseAll(txnId);
    }
}

// End LucidDbTxnMgr.java
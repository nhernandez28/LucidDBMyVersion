/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005-2005 The Eigenbase Project
// Copyright (C) 2005-2005 Disruptive Tech
// Copyright (C) 2005-2005 LucidEra, Inc.
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
package org.eigenbase.sql.type;

import org.eigenbase.sql.*;
import org.eigenbase.sql.validate.*;
import org.eigenbase.reltype.*;
import org.eigenbase.util.*;

import java.util.*;

/**
 * This class allows multiple existing {@link SqlOperandTypeChecker} rules
 * to be combined into one rule.<p> For example, giving an operand the
 * signature of both a string or a numeric could be done by:
 * <blockquote><pre><code>
 *
 * CompositeOperandsTypeChecking newCompositeRule =
 *  new CompositeOperandsTypeChecking(
 *    Composition.OR,
 *    new SqlOperandTypeChecker[]{stringRule, numericRule});
 *
 * </code></pre></blockquote>
 * Simmilary a rule that would only allow a numeric literal can be done by:
 * <blockquote><pre><code>
 *
 * CompositeOperandsTypeChecking newCompositeRule =
 *  new CompositeOperandsTypeChecking(
 *    Composition.AND,
 *    new SqlOperandTypeChecker[]{literalRule, numericRule});
 *
 * </code></pre></blockquote>
 *
 * @author Wael Chatila
 * @version $Id$
 */
public class CompositeOperandTypeChecker
    implements SqlOperandTypeChecker
{
    private SqlOperandTypeChecker[] allowedRules;
    private Composition composition;

    public CompositeOperandTypeChecker(
        Composition composition,
        SqlOperandTypeChecker[] allowedRules)
    {
        Util.pre(null != allowedRules, "null != allowedRules");
        Util.pre(allowedRules.length > 1, "Not a composite type");
        int firstArgsLength = allowedRules[0].getArgCount();
        for (int i = 1; i < allowedRules.length; i++) {
            Util.pre(allowedRules[i].getArgCount() == firstArgsLength,
                "All must have the same operand length");
        }
        this.allowedRules = allowedRules;
        this.composition = composition;
    }

    public SqlOperandTypeChecker[] getRules()
    {
        return allowedRules;
    }

    public String getAllowedSignatures(SqlOperator op)
    {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < allowedRules.length; i++) {
            SqlOperandTypeChecker rule = allowedRules[i];
            if (i > 0) {
                ret.append(SqlOperator.NL);
            }
            ret.append(rule.getAllowedSignatures(op));
        }
        return ret.toString();
    }

    public int getArgCount()
    {
        // Check made in constructor to verify that all rules have the same
        // number of arguments. Take and return the first one.
        return allowedRules[0].getArgCount();
    }

    public boolean check(
        SqlCall call,
        SqlValidator validator,
        SqlValidatorScope scope,
        SqlNode node,
        int ruleOrdinal,
        boolean throwOnFailure)
    {
        Util.pre(allowedRules.length >= 1, "allowedRules.length>=1");
        int typeErrorCount = 0;

        boolean throwOnAndFailure =
            (composition == AND) && throwOnFailure;

        for (int i = 0; i < allowedRules.length; i++) {
            SqlOperandTypeChecker rule = allowedRules[i];
            if (!rule.check(call, validator, scope, node,
                    ruleOrdinal, throwOnAndFailure)) {
                typeErrorCount++;
            }
        }

        boolean ret=false;
        if (composition == AND) {
            ret = typeErrorCount == 0;
        } else if (composition == OR) {
            ret = (typeErrorCount < allowedRules.length);
        } else {
            //should never come here
            throw Util.needToImplement(this);
        }

        if (!ret && throwOnFailure) {
            //in the case of a composite OR we want to throw an error
            //describing in more detail what the problem was, hence doing
            //the loop again
            for (int i = 0; i < allowedRules.length; i++) {
                SqlOperandTypeChecker rule = allowedRules[i];
                if (!rule.check(call, validator, scope, node,
                        ruleOrdinal, true)) {
                    typeErrorCount++;
                }
            }
            //if no exception thrown, just throw a generic validation
            //signature error
            throw call.newValidationSignatureError(validator, scope);
        }

        return ret;
    }

    public boolean check(
        SqlValidator validator,
        SqlValidatorScope scope,
        SqlCall call,
        boolean throwOnFailure)
    {
        int typeErrorCount = 0;

        for (int i = 0; i < allowedRules.length; i++) {
            SqlOperandTypeChecker rule = allowedRules[i];

            if (!rule.check(validator, scope, call, false)) {
                typeErrorCount++;
            }
        }

        boolean failed = true;
        if (composition == AND) {
            failed = typeErrorCount>0;
        } else if (composition == OR) {
            failed = (typeErrorCount == allowedRules.length);
        }

        if (failed) {
            if (throwOnFailure) {
                throw call.newValidationSignatureError(validator, scope);
            }
            return false;
        }
        return true;
    }

    //~ Inner Class ----------------------
    public static class Composition extends EnumeratedValues.BasicValue
    {
        private Composition(String name, int ordinal)
        {
            super(name, ordinal, null);
        }
    }
    
    public static final Composition AND = new Composition("AND", 0);
    public static final Composition OR = new Composition("OR", 1);

    public static final EnumeratedValues enumeration =
        new EnumeratedValues(new Composition [] { AND, OR });

}

// End CompositeOperandTypeChecker.java
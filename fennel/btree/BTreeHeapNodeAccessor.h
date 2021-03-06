/*
// Licensed to DynamoBI Corporation (DynamoBI) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  DynamoBI licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at

//   http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
*/

#ifndef Fennel_BTreeHeapNodeAccessor_Included
#define Fennel_BTreeHeapNodeAccessor_Included

#include "fennel/btree/BTreeNodeAccessor.h"

FENNEL_BEGIN_NAMESPACE

/**
 * BTreeHeapNodeAccessor maintains the data on a BTreeNode using a standard
 * indirection scheme.  The start of the data area contains a contiguous array
 * of 2-byte offsets to the actual data, which is stored non-contiguously
 * (intermixed with free space) throughout the rest of the data area.
 */
class FENNEL_BTREE_EXPORT BTreeHeapNodeAccessor
    : public BTreeNodeAccessor
{
    // REVIEW:  this limits us to 64K page size max; could be templatized
    typedef uint16_t EntryOffset;

    inline EntryOffset const *getEntryOffsetPointer(
        BTreeNode const &node,uint iEntry);

    inline EntryOffset *getEntryOffsetPointer(
        BTreeNode &node,uint iEntry);

    inline uint getEntryOffset(BTreeNode const &node,uint iEntry);

    inline uint getEntrySizeWithOverhead(uint cbEntry);

    inline uint getEntryOffsetArrayByteSize(uint nEntries);

public:
    explicit BTreeHeapNodeAccessor();

    inline PConstBuffer getEntryForReadInline(
        BTreeNode const &node,uint iEntry);

    // implement the BTreeNodeAccessor interface
    virtual void clearNode(BTreeNode &node,uint cbPage);
    virtual PBuffer allocateEntry(BTreeNode &node,uint iEntry,uint cbEntry);
    virtual void deallocateEntry(BTreeNode &node,uint iEntry);
    virtual bool hasFixedWidthEntries() const;
    virtual Capacity calculateCapacity(BTreeNode const &node,uint cbEntry);
    virtual uint getEntryByteCount(uint cbTuple);
};

inline BTreeHeapNodeAccessor::EntryOffset const *
BTreeHeapNodeAccessor::getEntryOffsetPointer(
    BTreeNode const &node,uint iEntry)
{
    return reinterpret_cast<EntryOffset const *>(node.getDataForRead())
        + iEntry;
}

inline BTreeHeapNodeAccessor::EntryOffset *
BTreeHeapNodeAccessor::getEntryOffsetPointer(
    BTreeNode &node,uint iEntry)
{
    return reinterpret_cast<EntryOffset *>(node.getDataForWrite())
        + iEntry;
}

inline uint BTreeHeapNodeAccessor::getEntryOffset(
    BTreeNode const &node,uint iEntry)
{
    assert(iEntry < node.nEntries);
    return *getEntryOffsetPointer(node,iEntry);
}

inline uint BTreeHeapNodeAccessor::getEntrySizeWithOverhead(uint cbEntry)
{
    return cbEntry + sizeof(EntryOffset);
}

inline uint BTreeHeapNodeAccessor::getEntryOffsetArrayByteSize(uint nEntries)
{
    assert(sizeof(EntryOffset) == 2);
    return nEntries << 1;
}

inline PConstBuffer BTreeHeapNodeAccessor::getEntryForReadInline(
    BTreeNode const &node,uint iEntry)
{
    uint offset = getEntryOffset(node, iEntry);
    return reinterpret_cast<PConstBuffer>(&node) + offset;
}

FENNEL_END_NAMESPACE

#endif

// End BTreeHeapNodeAccessor.h

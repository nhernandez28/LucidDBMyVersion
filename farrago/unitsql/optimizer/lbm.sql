-- $Id$

-------------------------------------
-- Sql level test for Bitmap Index --
-------------------------------------

create schema lbm;
set schema 'lbm';
set path 'lbm';

create server test_data
foreign data wrapper sys_file_wrapper
options (
    directory 'unitsql/optimizer/data',
    file_extension 'csv',
    with_header 'yes', 
    log_directory 'testlog');

create foreign table matrix3x3(
    a tinyint,
    b integer,
    c bigint)
server test_data
options (filename 'matrix3x3');

create foreign table matrix9x9(
    a1 tinyint,
    b1 integer,
    c1 bigint,
    a2 tinyint,
    b2 integer,
    c2 bigint, 
    a3 tinyint,
    b3 integer,
    c3 bigint) 
server test_data
options (filename 'matrix9x9');

-----------------------------------------------------
-- Part 1. Indexes based on single column clusters --
-----------------------------------------------------

--
-- 1.1 One index on a table without a primary key
--
create table single(
    a tinyint,
    b integer,
    c bigint) 
server sys_column_store_data_server;

create index single_one 
on single(b);

insert into single values (1,2,3);
insert into single values (0,0,0);

insert into single 
select * from matrix3x3;

drop index single_one;

create index single_one_recreated
on single(b);

truncate table single;
drop index single_one_recreated;

--
-- 1.2 One multi-column index on a table without a primary key
--
create index single_one_multi 
on single(b, c);

insert into single 
select * from matrix3x3;

truncate table single;
drop index single_one_multi;

--
-- 1.3 Several single column indexes on a table without a primary key
--
create index single_two_b
on single(b);

create index single_two_c
on single(c);

insert into single 
select * from matrix3x3;

drop table single;

--
-- 1.4 A table with a primary key (the constraint is itself an index)
--
create table single(
    a tinyint,
    b integer primary key,
    c bigint) 
server sys_column_store_data_server;

insert into single 
select * from matrix3x3;

drop table single;


----------------------------------------------------
-- Part 2. Indexes based on multi column clusters --
----------------------------------------------------

--
-- 2.1 An index with multiple columns, ordered
--
create table multi(
    a tinyint,
    b integer,
    c bigint) 
server sys_column_store_data_server
create clustered index multi_all on multi(a, b, c);

create index multi_multikey on multi(a, b);

insert into multi 
select * from matrix3x3;

insert into multi 
select * from matrix3x3;

insert into multi 
select * from matrix3x3;

insert into multi 
select * from matrix3x3;

truncate table multi;
drop index multi_multikey;

--
-- 2.2 An index with multiple columns, rearranged
--
create index multi_multikey on multi(c, a, b);

insert into multi 
select * from matrix3x3;

truncate table multi;
drop index multi_multikey;

--
-- 2.3 Multiple single columns indexes
--
create index multi_singlekey_b on multi(b);
create index multi_singlekey_c on multi(c);

insert into multi 
select * from matrix3x3;

truncate table multi;
drop index multi_singlekey_b;
drop index multi_singlekey_c;

--
-- 2.4 Multiple multi columns indexes
--
create index multi_multikey_cb on multi(c, b);
create index multi_multikey_ba on multi(b, a);

insert into multi 
select * from matrix3x3;

-- try some nulls, and reverse data
create foreign table matrix3x3_alt(
    a tinyint,
    b integer,
    c bigint)
server test_data
options (filename 'matrix3x3_alt');

-- FIXME: these fail with the truncate/drop index scheme

-- insert into multi
-- select * from matrix3x3_alt;

-- some more data
-- insert into multi 
-- select * from matrix3x3;

drop index multi_multikey_cb;
drop index multi_multikey_ba;

create index multi_multikey_cb_recreated on multi(c, b);
create index multi_multikey_ba_recreated on multi(b, a);

drop table multi;


-------------------------------------------------------------
-- Part 3. Indexes based on multiple multi column clusters --
-------------------------------------------------------------

--
-- 3.1 Indexes based on subsets of clusters
--
create table multimulti(
    a1 tinyint,
    b1 integer,
    c1 bigint,
    a2 tinyint,
    b2 integer,
    c2 bigint, 
    a3 tinyint,
    b3 integer,
    c3 bigint) 
server sys_column_store_data_server
create clustered index multi_1 on multimulti(a1, b1, c1)
create clustered index multi_2 on multimulti(a2, b2, c2)
create clustered index multi_3 on multimulti(a3, b3, c3);

create index multimulti_subset_a on multimulti(b1);
create index multimulti_subset_b on multimulti(a2,b2);
create index multimulti_subset_c on multimulti(c2,a2);

insert into multimulti
select * from matrix9x9;

truncate table multimulti;
drop index multimulti_subset_a;
drop index multimulti_subset_b;
drop index multimulti_subset_c;

--
-- 3.2 Indexes based on multiple clusters
--

create index multimulti_mixed_a on multimulti(a1,a2,a3);
create index multimulti_mixed_b on multimulti(b1,b2,b3);
create index multimulti_mixed_c on multimulti(c1,c2,b2);

insert into multimulti
select * from matrix9x9;

-- some alternate data (descending, nulls)
create foreign table matrix9x9_alt(
    a1 tinyint,
    b1 integer,
    c1 bigint,
    a2 bigint,
    b2 integer,
    c2 tinyint, 
    a3 bigint,
    b3 tinyint,
    c3 integer) 
server test_data
options (filename 'matrix9x9_alt');

-- FIXME: these no longer work with the truncate table / drop index testing

-- insert into multimulti
-- select * from matrix9x9_alt;

-- insert into multimulti
-- select * from matrix9x9;

-- insert into multimulti
-- select * from matrix9x9_alt;

-- insert into multimulti
-- select * from matrix9x9;

drop table multimulti;


-------------------------------------------------------------
-- Part 4. Data types, null values                         --
-------------------------------------------------------------

create table typed(
    a int,
    b varbinary(256),
    c binary(10),
    d char(10),
    e decimal(6,2),
    f smallint,
    g real,
    h double,
    i varchar(256),
    j boolean,
    k date,
    l time,
    m timestamp,
    n numeric(10,2)) 
server sys_column_store_data_server;

create index typed_a on typed(a);
create index typed_b on typed(b);
create index typed_c on typed(c);
create index typed_d on typed(d);
create index typed_e on typed(e);
create index typed_f on typed(f);
create index typed_g on typed(g);
create index typed_h on typed(h);
create index typed_i on typed(i);
create index typed_j on typed(j);
create index typed_k on typed(k);
create index typed_l on typed(l);
create index typed_m on typed(m);
create index typed_n on typed(n);

-- surprisingly these don't cause any problems
-- not sure how to input a binary field
insert into typed values(
    1,X'deadbeef',null,'first',
    0.16,1,1,1,
    '1st',true,DATE'2001-11-11',TIME'23:11:08',
    TIMESTAMP'2001-11-11 23:11:08',0.02);
insert into typed values(
    2,null,null,null,
    null,null,null,null,
    null,null,null,null,
    null,null);
insert into typed values(
    3,X'1ead2006',null,'third',
    0.16,1,1,1,
    '3rd',false,DATE'2001-12-12',TIME'23:11:08',
    TIMESTAMP'2001-12-12 23:11:08',0.06);

-- FIXME: this doesn't work

-- insert into typed (a,d,e,f,g,h,i,j,k,l,m,n)
-- select * from test_data.BCP."typed";

-- cleanup

drop server test_data cascade;
-- $Id$
-- Test statement cancel

create schema cancel_test;

create function cancel_test.noise_with_cancel(
    n bigint, random_seed bigint,  cancel_delay_millis bigint)
returns table(b bigint)
language java
parameter style system defined java
no sql
external name 'class net.sf.farrago.test.FarragoTestUDR.noiseWithCancel';

-- Test cancel during external sort (FNL-48).  At least we hope it's
-- during external sort; it's hard to guarantee.  The UDX produces
-- a million rows and then schedules a timer to cancel the rest of the
-- execution after 5 seconds.
select * 
from table(cancel_test.noise_with_cancel(1000000, 0, 5000)) order by 1;

-- Likewise for hash partitioning (LDB-122).
select count(distinct b) 
from table(cancel_test.noise_with_cancel(1000000, 0, 5000));
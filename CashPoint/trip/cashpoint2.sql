drop table if exists cashpoint;
create table cashpoint (
	entry integer not null,	-- unique for the inputs, reference for the distributions
	name text not null,		-- the person involved
	amount real not null,	-- the amount of money, negative for distributions
	currency text,	-- if null it's the default currency (Euro or Dollar or ...)
	date numeric,	-- if null it's a distribution
	comment text	-- optional, for recognition
);
	
-- the following insert represents an input to cashpoint	
insert into cashpoint (entry,name,amount,date,comment)  
		values (1,'Bob',100.0,'12/24/11','gas');
-- the next two inserts represent ditributions referring to the above input
insert into cashpoint (entry,name,amount) values (1,'Bob',-100.0/3);
insert into cashpoint (entry,name,amount) values (1,'Tom',-100.0/3);
insert into cashpoint (entry,name,amount) values (1,'Sue',-100.0/3);

-- the following insert represents an input to cashpoint	
insert into cashpoint (entry,name,amount,date,comment)
		values (2,'Sue',70.0,'12/23/11','groceries');
-- the next two inserts represent ditributions referring to the above input
insert into cashpoint (entry,name,amount) values (2,'Bob',-70.0/3);
insert into cashpoint (entry,name,amount) values (2,'Tom',-70.0/3);
insert into cashpoint (entry,name,amount) values (2,'Sue',-70.0/3);

.header on
.mode column
select * from cashpoint;
select sum(amount) as expenses from cashpoint where date not null;
select count(name) as numOfNames from (select distinct name from cashpoint);
select distinct comment as purposes from cashpoint where comment not null;
select name,balance from 
	(select name,sum(amount) as balance from cashpoint group by name);
 -- following syntax is not required, 'group by ' gives the same effect
 -- (select name,sum(amount) as balance from cashpoint where name='Bob' union
 --  select name,sum(amount) as balance from cashpoint where name='Tom'union 
 --  select name,sum(amount) as balance from cashpoint where name='Sue');


 
-- the following doesn 't work unfortunately
-- select expenses/names as 'per person' from 
--	(select sum(amount) as expenses from cashpoint where date not null) join
--	(select count(name) as names from (select distinct name from cashpoint);

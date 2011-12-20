#!/bin/bash
pwd
sqlite3 test.db  "drop table if exists cashpoint;"
sqlite3 test.db  "create table cashpoint (
	entry integer not null,	-- unique for the inputs, reference for the distributions
	name text not null,		-- the person involved
	amount real not null,	-- the amount of money, negative for distributions
	currency text,	-- if null it's the default currency (Euro or Dollar or ...)
	date numeric,	-- if null it's a distribution
	comment text	-- optional, for recognition
);"

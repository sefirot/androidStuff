create table if not exists kitty (
	entry integer not null,		--	id for transactions, reference for the shares
	name text not null,			--	name of the person involved
	amount real not null,		--	the amount of money
	currency text,				--	if null it's the default currency (Euro or Dollar or ...)
	timestamp text,				--	if null it's a share
	comment text,				--	optional, for recognition
	expense integer not null	--	boolean, if true then the amount means an expense which is likely shared among others
	);

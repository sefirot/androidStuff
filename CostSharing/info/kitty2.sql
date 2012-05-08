PRAGMA foreign_keys = ON;

create table if not exists kitty (
	id integer PRIMARY KEY,
	entry integer not null,		--	id for transactions
	name text not null,			--	name of the person involved
	amount real not null,		--	the amount of money
	currency text,				--	if null it's the default currency (Euro or Dollar or ...)
	timestamp text,				--	if null it's a share
	comment text,				--	optional, for recognition
	expense integer not null	--	boolean, if true then the amount means an expense which is likely shared among others
);

create table if not exists sharepool (
	entry integer not null,		--	reference for the shares
	name text not null,			--	name of the person sharing
	amount real not null,		--	the amount of money
	FOREIGN KEY(entry) REFERENCES kitty(id)             
);

INSERT INTO "kitty" VALUES(null,1,'Tom',50.0,'','2012-02-04 12:19:56.336','stake',0);
INSERT INTO "kitty" VALUES(2,2,'Bob',100.0,'','2012-02-04 12:19:56.438','gas',1);
INSERT INTO "sharepool" VALUES(2,'Bob',-33.3333333333333);
INSERT INTO "sharepool" VALUES(2,'Sue',-33.3333333333333);
INSERT INTO "sharepool" VALUES(2,'Tom',-33.3333333333333);
INSERT INTO "kitty" VALUES(3,3,'Sue',70.0,'','2012-02-04 12:19:56.575','groceries',1);
INSERT INTO "sharepool" VALUES(3,'Bob',-23.3333333333333);
INSERT INTO "sharepool" VALUES(3,'Sue',-23.3333333333333);
INSERT INTO "sharepool" VALUES(3,'Tom',-23.3333333333333);
INSERT INTO "kitty" VALUES(null,4,'Tom',10.0,'','2012-02-04 12:19:56.690','better balance',0);
INSERT INTO "kitty" VALUES(null,4,'Sue',-10.0,'','2012-02-04 12:19:56.706','better balance',0);
INSERT INTO "kitty" VALUES(null,5,'Bob',-43.3333333333333,'','2012-02-04 12:19:56.786','uniform sharing compensation',0);
INSERT INTO "kitty" VALUES(null,5,'Sue',-3.33333333333334,'','2012-02-04 12:19:56.797','uniform sharing compensation',0);
INSERT INTO "kitty" VALUES(null,5,'Tom',-3.33333333333333,'','2012-02-04 12:19:56.806','uniform sharing compensation',0);

select * from kitty, sharepool where kitty.entry = sharepool.entry;


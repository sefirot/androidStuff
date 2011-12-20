drop table credits;
create table credits (
	date text not null,
	creditor text not null,
	amount integer not null,
	purpose text not null);
	
drop table debits;
create table debits (
	ENTRY integer not null,
	debitor text not null,
	share integer not null);
	
insert into credits (ROWID,date,creditor,amount,purpose)  values (1,'12/24/11','Bob',100.0,'gas');
insert into debits (ENTRY,debitor,share)  values (1,'Tom',-100.0/3);
insert into debits (ENTRY,debitor,share)  values (1,'Sue',-100.0/3);
insert into credits (ROWID,date,creditor,amount,purpose)  values (2,'12/23/11','Sue',70.0,'groceries');
insert into debits (ENTRY,debitor,share)  values (2,'Bob',-70.0/3);
insert into debits (ENTRY,debitor,share)  values (2,'Tom',-70.0/3);

.header on
.mode column
select * from credits JOIN debits where credits.ROWID=debits.ENTRY;

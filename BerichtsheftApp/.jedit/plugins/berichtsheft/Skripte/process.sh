#!/bin/bash

dir=`dirname $0`
cd $dir/../databases

. $dir/functions

wi=$(pwd)/weather_info.db
np=/home/lotharla/work/Niklas/note_pad.db
ready=Ready/*.db
raw=/home/lotharla/work/Niklas/Bemerkungen*

case "$1" in

test)
	echo "Hello World"
	;;

confluence_notes)
	db="$3"
	cat > "$sql" << EOF
CREATE TABLE if not exists notes (
_id INTEGER PRIMARY KEY,
title TEXT,
note TEXT,
created INTEGER,
modified INTEGER,
UNIQUE(created,title));
attach '$2' as tributary;
INSERT INTO notes (_id,title,note,created,modified)
SELECT null,title,note,created,modified
FROM tributary.notes;
EOF
	exec_sql
	echo "'notes' copied into $db"
	;;
	
confluence_weathers)
	db="$3"
	cat > "$sql" << EOF
CREATE TABLE if not exists weathers (
_id INTEGER PRIMARY KEY,
description TEXT,
location TEXT,
precipitation FLOAT,
maxtemp FLOAT,
mintemp FLOAT,
created INTEGER,
modified INTEGER);
attach '$2' as tributary;
INSERT INTO weathers (_id,description,location,precipitation,maxtemp,mintemp,created,modified)
SELECT null,description,location,precipitation,maxtemp,mintemp,created,modified
FROM tributary.weathers;
EOF
	exec_sql
	echo "'weathers' copied into $db"
	;;


desc)
	/home/lotharla/gawk-4.0.0/gawk -f "${dir}/descriptions.awk" < "/home/lotharla/work/Niklas/www1.ncdc.noaa.gov/553356121374dat.txt"
	;;

21nov2012)
#	datetime(created/1000,'unixepoch','localtime','start of day')
	let start=1000*$(date +%s -d"Nov 21, 2012 00:00:00")
	let end=$start+86399999
#	sqlite3 "$db" "select strftime('%Y-%m-%d', created/1000),note from notes where created between $start and $end"
	sqlite3 "$db" "select note from notes where date(created/1000, 'unixepoch','localtime')='2012-11-21'"
	;;
	
Doppelte_Tagesberichte)
	sqlite3 "$db" \
		"select date(created/1000, 'unixepoch','localtime'),count(*) 
		from notes 
		where title='$1' 
		group by date(created/1000, 'unixepoch','localtime')
		order by count(*) desc" | \
			zenity --title "$1" --text-info
	;;
	
collect_notes)
	newNotepad "$db"
	$0 read_notes
	$0 read_more_notes
	;;
	
read_notes)
	for f in $ready ; do 
		sqlite3 $f ".dump" | \
			eliminate_id > $sql
		zenity --title "$f" --text-info --filename "$sql"
		insert_count "$sql"
		exec_sql
	done
	echo_results "$db"
	;;
	
read_more_notes)
	for f in $raw ; do 
		cat "$f" | \
			parse_notes > $sql
		zenity --title "$f" --text-info --filename "$sql"
		insert_count "$sql"
		exec_sql
	done
	echo_results "$db"
	;;
	
integrate_notes)
	cat > "$sql" << EOF
attach '$db' as more;
INSERT INTO notes (title,note,created,modified)
SELECT title,note,created,modified
FROM more.notes;
EOF
	db=berichtsheft.db
	echo -n "before : "
	note_count "$db"
	exec_sql
	echo -n "after : "
	note_count "$db"
	;;
	
integrate_weathers)
	cat > "$sql" << EOF
DROP TABLE if exists weathers;
CREATE TABLE weathers (
_id INTEGER PRIMARY KEY AUTOINCREMENT,
description TEXT,
location TEXT,
precipitation FLOAT,
maxtemp FLOAT,
mintemp FLOAT,
created INTEGER,
modified INTEGER);
attach '$wi' as winfo;
INSERT INTO weathers (description,location,precipitation,maxtemp,mintemp,created,modified)
SELECT description,location,precipitation,maxtemp,mintemp,created,modified
FROM winfo.weathers;
EOF
	exec_sql
	echo "'weathers' integrated into $db"
	;;
	
berufsschule_notes)
	db=berichtsheft.db
	db2=berichtsheft_2012.db
	cat > "$sql" << EOF
attach '$db2' as old;
insert into notes (title,note,created,modified)
	select title,note,created,modified from old.notes where title like 'beruf%';
select date(created/1000, 'unixepoch','localtime') from notes where title like 'beruf%';
EOF
	exec_sql
	echo "$db"
	;;
	
copy_weathers)
#	echo `pwd`
	db=berichtsheft_2013.db
	cat > "$sql" << EOF
attach '$wi' as winfo;
INSERT INTO weathers (description,location,precipitation,maxtemp,mintemp,created,modified)
SELECT description,location,precipitation,maxtemp,mintemp,created,modified
FROM winfo.weathers;
EOF
	exec_sql
	echo "'weathers' copied into $db"
	;;
	
esac

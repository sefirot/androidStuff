#!/bin/bash

dir=`dirname $0`
cd $dir/../databases

. $dir/functions

wi=$(pwd)/weather_info.db
np=/home/lotharla/work/Niklas/note_pad.db
ready=Ready/*.db
raw=/home/lotharla/work/Niklas/Bemerkungen*

case "$1" in

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
	echo "\'weathers\' integrated into $db"
	;;
	
esac

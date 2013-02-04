#!/bin/bash

cd `dirname $0`/../databases
db=berichtsheft.db
wi=$(pwd)/weather_info.db
temp=/tmp/berichtsheft.sql

case "$1" in

21nov2012)
	let start=1000*$(date +%s -d"Nov 21, 2012 00:00:00")
	let end=$start+86399999
#	sqlite3 "$db" "select strftime('%Y-%m-%d', created/1000),note from notes where created between $start and $end"
	sqlite3 "$db" "select note from notes where date(created/1000, 'unixepoch')='2012-11-21'"
	;;
	
Bericht)
	sqlite3 "$db" \
		"select date(created/1000, 'unixepoch'),count(*) 
		from notes 
		where title='$1' 
		group by date(created/1000, 'unixepoch')
		order by count(*) desc" | \
			zenity --title "$1" --text-info
	;;
	
collect_notes)
	rm -f "$db"
	sqlite3 "$db" \
		"CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,note TEXT,created INTEGER,modified INTEGER)"
	for f in Ready/*.db ; do 
		sqlite3 $f ".dump" | \
			awk '
			BEGIN {
				print "BEGIN TRANSACTION;"
			}
			/INSERT INTO [\"]*notes/ {
				record = $0
				while (match($0, /\)[[:space:]]*;[[:space:]]*$/) < 1) {
					if (getline <= 0) {
						m = "unexpected EOF or error"
						m = (m ": " ERRNO)
						print m > "/dev/stderr"
						exit
					}
					record = record RS $0
				}
				print gensub(/( VALUES[[:space:]]*\()[[:space:]]*[[:digit:]]+[[:space:]]*,/, " (title,note,created,modified)\\1", 1, record)
				n++
			}
			END {
				print "COMMIT;"
				print "-- "n" inserts"
			}
			' > $temp
		zenity --title "$f" --text-info --filename "$temp"
		while read line ; do    
			case "$line" in
			--*)
				let cnt+=$(echo ${line} | awk '{print $2}')
				;;
			esac
		done < "$temp"
		sqlite3 "$db" < "$temp"
	done
	echo "$cnt inserts"
	cnt=$( sqlite3 "$db" "select count(*) from notes" )
	echo "$cnt records"
	;;
	
integrate_weathers)
	cat > "$temp" << EOF
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
	sqlite3 "$db" < "$temp"
	echo \'weathers\' integrated into "$db"
	;;
	
esac

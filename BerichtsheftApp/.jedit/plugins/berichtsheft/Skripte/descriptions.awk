function walk_array(a, name,    i)
{
	for (i in a) {
		if (isarray(a[i]))
			walk_array(a[i], (name "[" i "]"))
		else
			printf("%s[%s] = %s\n", name, i, a[i])
	}
}
function walk_sky(a, text,    i,n,avg)
{
	if (isarray(a)) {
		printf "%s ", text
#		CLR-CLEAR : 0, SCT-SCATTERED : 3, BKN-BROKEN : 6, OVC-OVERCAST : 9, OBS-OBSCURED : 10
		weight["CLR"] = 0
		weight["SCT"] = 3
		weight["BKN"] = 6
		weight["OVC"] = 9
		weight["OBS"] = 10
		for (i in a) {
#			printf("%s(%d)\t", i, a[i])
			n += a[i]
			avg += a[i] * weight[i]
		}
		avg = avg / n
#		printf("%f\t", avg)
#		sonnig : 0-2, leicht bewölkt : 2-5, aufgelockert : 5-8, stark bewölkt : 8-10
		if (avg < 2)
			printf "sonnig"
		else if (avg < 5)
			printf "leicht bewölkt"
		else if (avg < 8)
			printf "aufgelockert"
		else
			printf "stark bewölkt"
	}
}
function walk_day(a)
{
	walk_sky(a["Vormittag"],"v.m.")
	printf "  "
	walk_sky(a["Nachmittag"],"n.m.")
}
NR > 1 {
	location = $1
	date = 1000 * mktime(substr($3,1,4)" "substr($3,5,2)" "substr($3,7,2)" 00 00 00")
	time = substr($3,9)
	sky = $8
	if (time >= "0700" && time <= "1900") {
		a[date][location] = location
		if (time < "1200")
			a[date]["Vormittag"][sky]++
		else
			a[date]["Nachmittag"][sky]++
	}
}
END {
#	walk_array(a, "a")
	n = asorti(a, ind)
	for (i=1; i<=n; i++)
	{
		date = ind[i]
		printf("%d\t", a[date][location])
		printf("%d\t", date)
#		printf("%s\t", strftime("%Y-%m-%d", date / 1000))
		walk_day(a[date])
		printf "\n"
	}
}

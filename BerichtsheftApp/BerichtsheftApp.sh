#!/bin/bash
dir=`dirname "$0"`/.jedit

# Find a java installation.
if [ -z "${JAVA_HOME}" ]; then
	echo 'Warning: $JAVA_HOME environment variable not set! Consider setting it.'
	echo '         Attempting to locate java...'
	j=`which java 2>/dev/null`
	if [ -z "$j" ]; then
		echo "Failed to locate the java virtual machine! Bailing..."
		exit 1
	else
		echo "Found a virtual machine at: $j..."
		JAVA="$j"
	fi
else
	JAVA="${JAVA_HOME}/bin/java"
fi

if [ ! -e "$dir"/properties ] ; then
	cp "$dir"/plugins/berichtsheft/jedit.properties "$dir"/properties
fi

# Launch application.

exec "${JAVA}" -Djava.library.path="$dir"/jars -jar "$dir/jedit.jar" \
	-settings="$dir" -newview -noserver -nosplash \
	-run=.jedit/macros/startBerichtsheft.bsh

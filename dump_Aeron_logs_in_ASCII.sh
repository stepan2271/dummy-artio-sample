#!/usr/bin/env bash

AERON_LOGS_BASE=/dev/shm/aeron-alex

for filename in ${AERON_LOGS_BASE}/publications/UDP*.logbuffer; do
    java -cp lib/agrona-0.9.26.jar:lib/aeron-client-1.11.4-SNAPSHOT.jar:lib/aeron-samples-1.11.4-SNAPSHOT.jar \
        -Daeron.log.inspector.data.format=ascii \
        io.aeron.samples.LogInspector \
        $filename 5000 >> ${filename}_ascii.log
done

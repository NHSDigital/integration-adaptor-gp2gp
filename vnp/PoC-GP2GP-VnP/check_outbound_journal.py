import sys
import json

# This script consumes std.in assuming it's a list of json payloads recorded by MHS mock
# The source of the list is MHS mock journal that can be accessed on http://localhost:8081/__admin/requests
# Script takes the last message from the journal and returns the inner xml on std.out

outbound_journal = json.loads(sys.stdin.read())
last_journal_entry = json.loads(outbound_journal[-1])
payload = last_journal_entry["payload"]

sys.stdout.writelines(payload)
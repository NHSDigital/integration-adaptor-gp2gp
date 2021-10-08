echo "### Sending 1 message to inbound queue."
echo ""

java -jar a-1.5.0-jar-with-dependencies.jar \
    -A \
    -b "amqp://admin:admin@localhost:5672" \
    -p "@9690937286.json" \
    -S "@set_conversation_id.js" \
    inbound

echo ""
echo "### Sleeping for 5 seconds. Waiting for patient transfer to complete."

sleep 5

echo ""
echo "### Done."
sleep 1
echo ""
echo "### Looking up for AA ACK code in the last request received by MHS mock."

sleep 1
echo ""
curl -s http://localhost:8081/__admin/requests | python check_outbound_journal.py | xpath "/MCCI_IN010000UK13/acknowledgement[@typeCode='AA'][1]/@typeCode"
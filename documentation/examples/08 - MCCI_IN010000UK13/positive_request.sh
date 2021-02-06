curl -i -k -X POST \
-H "Content-Type: application/json" \
-H "Interaction-Id: MCCI_IN010000UK13" \
-H "sync-async: false" \
-H "ods-code: X26" \
-H "wait-for-response: false" \
-d '{
    "payload": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><hl7:MCCI_IN010000UK13 xmlns:hl7=\"urn:hl7-org:v3\">\n<hl7:id root=\"98BB4E88-6C71-2155-7FCB-1900BDE2ED1A\"/>\n<hl7:creationTime value=\"20210203125928\"/>\n<hl7:versionCode code=\"V3NPfIT3.1.09\"/>\n<hl7:interactionId root=\"2.16.840.1.113883.2.1.3.2.4.12\" extension=\"MCCI_IN010000UK13\"/>\n<hl7:processingCode code=\"P\"/>\n<hl7:processingModeCode code=\"T\"/>\n<hl7:acceptAckCode code=\"NE\"/>\n<hl7:acknowledgement typeCode=\"AA\">\n<hl7:messageRef>\n<hl7:id root=\"3a445b16-6b31-42ee-bfe9-f33b48c71fb6}}\"/>\n</hl7:messageRef>\n</hl7:acknowledgement>\n<hl7:communicationFunctionRcv typeCode=\"RCV\">\n<hl7:device classCode=\"DEV\" determinerCode=\"INSTANCE\">\n<hl7:id root=\"1.2.826.0.1285.0.2.0.107\" extension=\"918999198964\"/>\n</hl7:device>\n</hl7:communicationFunctionRcv>\n<hl7:communicationFunctionSnd typeCode=\"SND\">\n<hl7:device classCode=\"DEV\" determinerCode=\"INSTANCE\">\n<hl7:id root=\"1.2.826.0.1285.0.2.0.107\" extension=\"918999198964\"/>\n</hl7:device>\n</hl7:communicationFunctionSnd>\n<hl7:ControlActEvent classCode=\"CACT\" moodCode=\"EVN\">\n<hl7:author1 typeCode=\"AUT\">\n<hl7:AgentSystemSDS classCode=\"AGNT\">\n<hl7:agentSystemSDS classCode=\"DEV\" determinerCode=\"INSTANCE\">\n<hl7:id root=\"1.2.826.0.1285.0.2.0.107\" extension=\"918999198964\"/>\n</hl7:agentSystemSDS>\n</hl7:AgentSystemSDS>\n</hl7:author1>\n</hl7:ControlActEvent>\n</hl7:MCCI_IN010000UK13>\n\n"
}' \
"http://localhost:80"

curl -i -k -X POST \
-H "Content-Type: application/json" \
-H "Interaction-Id: COPC_IN000001UK01" \
-H "sync-async: false" \
-H "ods-code: X26" \
-H "wait-for-response: false" \
-d '{
    "payload": "<COPC_IN000001UK01 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"urn:hl7-org:v3\">\r\n <id root=\"3a445b16-6b31-42ee-bfe9-f33b48c71fb6}}\" />\r\n <creationTime value=\"20190927152035\" />\r\n <versionCode code=\"3NPfIT7.2.02\" />\r\n <interactionId root=\"2.16.840.1.113883.2.1.3.2.4.12\" extension=\"COPC_IN000001UK01\" />\r\n <processingCode code=\"P\" />\r\n <processingModeCode code=\"T\" />\r\n <acceptAckCode code=\"NE\" />\r\n <communicationFunctionRcv type=\"CommunicationFunction\" typeCode=\"RCV\">\r\n <device type=\"Device\" classCode=\"DEV\" determinerCode=\"INSTANCE\">\r\n <id root=\"1.2.826.0.1285.0.2.0.107\" extension=\"918999198964\" />\r\n </device>\r\n </communicationFunctionRcv>\r\n <communicationFunctionSnd type=\"CommunicationFunction\" typeCode=\"SND\">\r\n <device type=\"Device\" classCode=\"DEV\" determinerCode=\"INSTANCE\">\r\n <id root=\"1.2.826.0.1285.0.2.0.107\" extension=\"918999198964\" />\r\n </device>\r\n </communicationFunctionSnd>\r\n </COPC_IN000001UK01>"
}' \
"http://localhost:80"

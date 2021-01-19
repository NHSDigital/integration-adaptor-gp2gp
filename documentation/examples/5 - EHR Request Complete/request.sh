#!/bin/bash

set -x

# curl -i --cacert opentest.ca-bundle --cert endpoint.crt --key endpoint.key https://msg.opentest.hscic.gov.uk

# Provider URL for OpenTest
PROVIDER_URL='https://msg.opentest.hscic.gov.uk/'

# Set SSP_URL to empty string to disable SSP
#SSP_URL='https://proxy.opentest.hscic.gov.uk/'
SSP_URL=''

# NOTE: the Authorization header needs to be updated manually with a non-expired JWT token. Use the postman collection
# against the public demonstrator to generate one

curl --cacert ../opentest.ca-bundle --cert ../endpoint.crt --key ../endpoint.key \
--location --request POST "http://internal-nia-build1-mhs-out-ecs-lb-1290278076.eu-west-2.elb.amazonaws.com:80/" \
--header 'Content-Type: application/json' \
--header 'Interaction-Id: QUPC_IN160101UK05' \
--header 'sync-async: true' \
--header 'from-asid: FROM_ASID' \
--header 'Host: 192.168.128.11' \
--header 'Connection: keep-alive' \
--header 'Accept: */*' \
--data-raw '{"payload": "<QUPC_IN160101UK05 xmlns=\"urn:hl7-org:v3\">\r\n            <id root=\"6565642B-D442-468A-85E2-CDE06B3352AB\" />\r\n            <creationTime value=\"20190927152034\"/>\r\n            <versionCode code=\"3NPfIT7.2.00\" />\r\n            <interactionId root=\"2.16.840.1.113883.2.1.3.2.4.12\" extension=\"QUPC_IN160101UK05\" />\r\n            <processingCode code=\"P\" />\r\n            <processingModeCode code=\"T\" />\r\n            <acceptAckCode code=\"NE\" />\r\n            <communicationFunctionRcv typeCode=\"RCV\">\r\n                <device classCode=\"DEV\" determinerCode=\"INSTANCE\">\r\n                    <id extension=\"YES-0000806\" root=\"1.2.826.0.1285.0.2.0.107\" />\r\n                </device>\r\n            </communicationFunctionRcv>\r\n            <communicationFunctionSnd typeCode=\"SND\">\r\n                <device classCode=\"DEV\" determinerCode=\"INSTANCE\">\r\n                    <id extension=\"FROM_ASID\" root=\"1.2.826.0.1285.0.2.0.107\" />\r\n                </device>\r\n            </communicationFunctionSnd>\r\n            <ControlActEvent classCode=\"CACT\" moodCode=\"EVN\">\r\n                <author1 typeCode=\"AUT\">\r\n                    <AgentSystemSDS classCode=\"AGNT\">\r\n                        <agentSystemSDS classCode=\"DEV\" determinerCode=\"INSTANCE\">\r\n                            <id extension=\"FROM_ASID\" root=\"1.2.826.0.1285.0.2.0.107\" />\r\n                        </agentSystemSDS>\r\n                    </AgentSystemSDS>\r\n                </author1>\r\n                <query>\r\n                    <dissentOverride>\r\n                        <semanticsText>DissentOverride</semanticsText>\r\n                        <value code=\"0\" codeSystem=\"2.16.840.1.113883.2.1.3.2.4.17.60\" displayName=\"Demonstration\">\r\n                            <originalText>Demonstration</originalText>\r\n                        </value>\r\n                    </dissentOverride>\r\n                    <filterParameters>\r\n                        <date>\r\n                            <semanticsText>Date</semanticsText>\r\n                            <value>\r\n                                <low value=\"20100908161126\"/>\r\n                                <high value=\"20190927152034\"/>\r\n                            </value>\r\n                        </date>\r\n                    </filterParameters>\r\n                    <nHSNumber>\r\n                        <semanticsText>NHSNumber</semanticsText>\r\n                        <value root=\"2.16.840.1.113883.2.1.4.1\" extension=\"9689177923\" />\r\n                    </nHSNumber>\r\n                </query>\r\n            </ControlActEvent>\r\n</QUPC_IN160101UK05>\r\n"}'
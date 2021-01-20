# Mock MHS Adaptor
The Mock MHS Adaptor currently has one endpoint, which mocks the MHS Outbound API. 

This is a **POST** request to `localhost:8081/mock-mhs-endpoint/`

## Known Request
- Request can be matched to a known reply
- Must have the request header Interaction-Id with value `RCMR_IN030000UK06`
- Must have a request body JSON of the form: `{"payload": "STRINGIFIED XML HERE"}`
- Publishes a stub JSON message onto the MHS inbound queue
- Produces a HTTP 202 response
- Returns a stub ebXML message in response body

## Unknown Request
- Request cannot be matched to a known reply
- Nothing is published to the inbound queue
- Produces a HTTP 500 response
- Returns an internal server error HTML message in response body

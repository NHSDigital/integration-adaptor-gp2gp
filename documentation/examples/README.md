h1. Example Message Flow for GP2GP Adaptor

h2. 1 - EHR Request

The adaptor receives the EHR Request message on the MHS inbonud queue.

h2. 2 - GPC Access Structured

Create files using values provided by OpenTest before running this `request.sh`

`endpoint.crt` - VPN endpoint certificate
`endpoint.key` - VPN endpoint private key
`opentest.ca-bundle` - root and sub-ca certs copied into the same file
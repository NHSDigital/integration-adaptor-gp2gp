# Wiremock for GP2GP Testing

The GP2GP wiremocks:

* MUST have some patients / orgs with responses consistent with GP Connect 
demonstrator in public (orangelab) / test / int
* COULD have additional patients not present in the GP Connect demonstrator test 
patient list

Most tests should run against both the wiremock and the public demonstrator.

## GP Connect

### Naming Convention

**/mappings**

gpc[CAPABILITY]_[NHS_NUMBER]_[ENDPOINT].json

Example: gpcAccessDocument_9690937286_findAPatient.json

**/__files*

gpc[CAPABILITY]_[NHS_NUMBER]_[ENDPOINT].json

Example: gpcAccessDocument_9690937286_findAPatient.json

### Test Patients

| NHS Number | Demonstrator? | Scenario |
|------------| --------------|----
| 9690937286 | Y             | Patient with documents
| 9690937294 |               | Patient with no documents
| ASDF       |               | Invalid NHS number
| 9876543210 |               | Patient not found

## SDS

### Naming Convention

**/mappings**

sds_[ODS]_[INTERACTION].json

Example: sds_ANY_gpc.getstructuredrecord-1.json

**/__files*

sds_[ODS]_[INTERACTION].json

Example: sds_ANY_gpc.getstructuredrecord-1.json

### Test Orgs

TODO: How does this work exactly?

| ODS Code | Scenario |
| ???      | Searchset with single GP Connect endpoint
| ???      | Empty searchset (no endpoints)